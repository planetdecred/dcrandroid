package dcrlibwallet

import (
	"math"
	"time"

	"github.com/raedahgroup/dcrlibwallet/spv"
	"golang.org/x/sync/errgroup"
)

func (mw *MultiWallet) spvSyncNotificationCallbacks() *spv.Notifications {
	return &spv.Notifications{
		PeerConnected: func(peerCount int32, addr string) {
			mw.handlePeerCountUpdate(peerCount)
		},
		PeerDisconnected: func(peerCount int32, addr string) {
			mw.handlePeerCountUpdate(peerCount)
		},
		Synced:                       mw.synced,
		FetchHeadersStarted:          mw.fetchHeadersStarted,
		FetchHeadersProgress:         mw.fetchHeadersProgress,
		FetchHeadersFinished:         mw.fetchHeadersFinished,
		FetchMissingCFiltersStarted:  func(walletID int) {},
		FetchMissingCFiltersProgress: func(walletID int, missingCFitlersStart, missingCFitlersEnd int32) {},
		FetchMissingCFiltersFinished: func(walletID int) {},
		DiscoverAddressesStarted:     mw.discoverAddressesStarted,
		DiscoverAddressesFinished:    mw.discoverAddressesFinished,
		RescanStarted:                mw.rescanStarted,
		RescanProgress:               mw.rescanProgress,
		RescanFinished:               mw.rescanFinished,
	}
}

func (mw *MultiWallet) handlePeerCountUpdate(peerCount int32) {
	mw.syncData.mu.Lock()
	defer mw.syncData.mu.Unlock()
	mw.syncData.connectedPeers = peerCount
	for _, syncProgressListener := range mw.syncData.syncProgressListeners {
		syncProgressListener.OnPeerConnectedOrDisconnected(peerCount)
	}

	if mw.syncData.showLogs && mw.syncData.syncing {
		if peerCount == 1 {
			log.Infof("Connected to %d peer on %s.\n", peerCount, mw.chainParams.Name)
		} else {
			log.Infof("Connected to %d peers on %s.\n", peerCount, mw.chainParams.Name)
		}
	}
}

// Fetch Headers Callbacks

func (mw *MultiWallet) fetchHeadersStarted(peerInitialHeight int32) {
	mw.syncData.mu.Lock()
	defer mw.syncData.mu.Unlock()
	if !mw.syncData.syncing || mw.syncData.beginFetchTimeStamp != -1 {
		// ignore if sync is not in progress i.e. !mw.syncData.syncing
		// or already started headers fetching i.e. mw.syncData.beginFetchTimeStamp != -1
		return
	}

	for _, wallet := range mw.wallets {
		wallet.waiting = true
	}

	mw.syncData.activeSyncData.syncStage = HeadersFetchSyncStage
	mw.syncData.activeSyncData.beginFetchTimeStamp = time.Now().Unix()
	mw.syncData.activeSyncData.startHeaderHeight = mw.GetLowestBlock().Height
	mw.syncData.activeSyncData.totalFetchedHeadersCount = 0

	if mw.syncData.showLogs && mw.syncData.syncing {
		blockInfo := mw.GetLowestBlock()
		totalHeadersToFetch := mw.estimateBlockHeadersCountAfter(blockInfo.Timestamp)
		log.Infof("Step 1 of 3 - fetching %d block headers.\n", totalHeadersToFetch)
	}
}

func (mw *MultiWallet) fetchHeadersProgress(fetchedHeadersCount int32, lastHeaderTime int64) {
	mw.syncData.mu.Lock()
	defer mw.syncData.mu.Unlock()

	if !mw.syncData.syncing || mw.syncData.activeSyncData.headersFetchTimeSpent != -1 {
		// Ignore this call because this function gets called for each peer and
		// we'd want to ignore those calls as far as the wallet is synced (i.e. !syncListener.syncing)
		// or headers are completely fetched (i.e. syncListener.headersFetchTimeSpent != -1)
		return
	}

	for _, wallet := range mw.wallets {
		if wallet.GetBestBlock() <= fetchedHeadersCount {
			wallet.waiting = false
		}
	}

	// If there was some period of inactivity,
	// assume that this process started at some point in the future,
	// thereby accounting for the total reported time of inactivity.
	mw.syncData.activeSyncData.beginFetchTimeStamp += mw.syncData.activeSyncData.totalInactiveSeconds
	mw.syncData.activeSyncData.totalInactiveSeconds = 0

	mw.syncData.activeSyncData.totalFetchedHeadersCount = fetchedHeadersCount
	headersLeftToFetch := mw.estimateBlockHeadersCountAfter(lastHeaderTime)
	totalHeadersToFetch := mw.syncData.activeSyncData.totalFetchedHeadersCount + headersLeftToFetch
	headersFetchProgress := float64(mw.syncData.activeSyncData.totalFetchedHeadersCount) / float64(totalHeadersToFetch)

	// update headers fetching progress report
	mw.syncData.activeSyncData.headersFetchProgress.TotalHeadersToFetch = totalHeadersToFetch
	mw.syncData.activeSyncData.headersFetchProgress.CurrentHeaderTimestamp = lastHeaderTime
	mw.syncData.activeSyncData.headersFetchProgress.FetchedHeadersCount = mw.syncData.activeSyncData.totalFetchedHeadersCount
	mw.syncData.activeSyncData.headersFetchProgress.HeadersFetchProgress = roundUp(headersFetchProgress * 100.0)

	timeTakenSoFar := time.Now().Unix() - mw.syncData.activeSyncData.beginFetchTimeStamp
	if timeTakenSoFar < 1 {
		timeTakenSoFar = 1
	}
	estimatedTotalHeadersFetchTime := float64(timeTakenSoFar) / headersFetchProgress

	// For some reason, the actual total headers fetch time is more than the predicted/estimated time.
	// Account for this difference by multiplying the estimatedTotalHeadersFetchTime by an incrementing factor.
	// The incrementing factor is inversely proportional to the headers fetch progress,
	// ranging from 0.5 to 0 as headers fetching progress increases from 0 to 1.
	adjustmentFactor := 0.5 * (1 - headersFetchProgress)
	estimatedTotalHeadersFetchTime += estimatedTotalHeadersFetchTime * adjustmentFactor

	estimatedDiscoveryTime := estimatedTotalHeadersFetchTime * DiscoveryPercentage
	estimatedRescanTime := estimatedTotalHeadersFetchTime * RescanPercentage
	estimatedTotalSyncTime := estimatedTotalHeadersFetchTime + estimatedDiscoveryTime + estimatedRescanTime

	// update total progress percentage and total time remaining
	totalSyncProgress := float64(timeTakenSoFar) / estimatedTotalSyncTime
	totalTimeRemainingSeconds := int64(math.Round(estimatedTotalSyncTime)) - timeTakenSoFar
	mw.syncData.activeSyncData.headersFetchProgress.TotalSyncProgress = roundUp(totalSyncProgress * 100.0)
	mw.syncData.activeSyncData.headersFetchProgress.TotalTimeRemainingSeconds = totalTimeRemainingSeconds

	// notify progress listener of estimated progress report
	mw.publishFetchHeadersProgress()

	headersFetchTimeRemaining := estimatedTotalHeadersFetchTime - float64(timeTakenSoFar)
	debugInfo := &DebugInfo{
		timeTakenSoFar,
		totalTimeRemainingSeconds,
		timeTakenSoFar,
		int64(math.Round(headersFetchTimeRemaining)),
	}
	mw.publishDebugInfo(debugInfo)
}

func (mw *MultiWallet) publishFetchHeadersProgress() {
	for _, syncProgressListener := range mw.syncData.syncProgressListeners {
		syncProgressListener.OnHeadersFetchProgress(&mw.syncData.headersFetchProgress)
	}
}

func (mw *MultiWallet) fetchHeadersFinished() {
	mw.syncData.mu.Lock()
	defer mw.syncData.mu.Unlock()

	if !mw.syncData.syncing {
		// ignore if sync is not in progress
		return
	}

	mw.syncData.activeSyncData.startHeaderHeight = -1
	mw.syncData.activeSyncData.headersFetchTimeSpent = time.Now().Unix() - mw.syncData.beginFetchTimeStamp

	// If there is some period of inactivity reported at this stage,
	// subtract it from the total stage time.
	mw.syncData.activeSyncData.headersFetchTimeSpent -= mw.syncData.totalInactiveSeconds
	mw.syncData.activeSyncData.totalInactiveSeconds = 0

	if mw.syncData.activeSyncData.headersFetchTimeSpent < 150 {
		// This ensures that minimum ETA used for stage 2 (address discovery) is 120 seconds (80% of 150 seconds).
		mw.syncData.activeSyncData.headersFetchTimeSpent = 150
	}

	if mw.syncData.showLogs && mw.syncData.syncing {
		log.Info("Fetch headers completed.")
	}
}

// Address/Account Discovery Callbacks

func (mw *MultiWallet) discoverAddressesStarted(walletID int) {
	mw.syncData.mu.Lock()
	defer mw.syncData.mu.Unlock()

	if !mw.syncData.syncing || mw.syncData.activeSyncData.addressDiscoveryCompletedOrCanceled != nil {
		// ignore if sync is not in progress i.e. !mw.syncData.syncing
		// or already started address discovery i.e. mw.syncData.activeSyncData.addressDiscoveryCompleted != nil
		return
	}

	mw.syncData.activeSyncData.syncStage = AddressDiscoverySyncStage
	mw.syncData.activeSyncData.addressDiscoveryStartTime = time.Now().Unix()
	mw.syncData.activeSyncData.addressDiscoveryProgress.WalletID = walletID
	if mw.syncData.showLogs && mw.syncData.syncing {
		log.Info("Step 2 of 3 - discovering used addresses.")
	}

	mw.updateAddressDiscoveryProgress()
}

func (mw *MultiWallet) updateAddressDiscoveryProgress() {
	// these values will be used every second to calculate the total sync progress
	totalHeadersFetchTime := float64(mw.syncData.headersFetchTimeSpent)
	estimatedDiscoveryTime := totalHeadersFetchTime * DiscoveryPercentage
	estimatedRescanTime := totalHeadersFetchTime * RescanPercentage

	// following channels are used to determine next step in the below subroutine
	everySecondTicker := time.NewTicker(1 * time.Second)
	everySecondTickerChannel := everySecondTicker.C

	// track last logged time remaining and total percent to avoid re-logging same message
	var lastTimeRemaining int64
	var lastTotalPercent int32 = -1

	mw.syncData.addressDiscoveryCompletedOrCanceled = make(chan bool)

	go func() {
		for {

			mw.syncData.mu.Lock()
			// If there was some period of inactivity,
			// assume that this process started at some point in the future,
			// thereby accounting for the total reported time of inactivity.
			mw.syncData.addressDiscoveryStartTime += mw.syncData.totalInactiveSeconds
			mw.syncData.totalInactiveSeconds = 0
			mw.syncData.mu.Unlock()

			select {
			case <-everySecondTickerChannel:
				mw.syncData.mu.Lock()

				if mw.syncData.activeSyncData == nil {
					mw.syncData.mu.Unlock()
					return
				}

				// calculate address discovery progress
				elapsedDiscoveryTime := float64(time.Now().Unix() - mw.syncData.addressDiscoveryStartTime)
				discoveryProgress := (elapsedDiscoveryTime / estimatedDiscoveryTime) * 100

				var totalSyncTime float64
				if elapsedDiscoveryTime > estimatedDiscoveryTime {
					totalSyncTime = totalHeadersFetchTime + elapsedDiscoveryTime + estimatedRescanTime
				} else {
					totalSyncTime = totalHeadersFetchTime + estimatedDiscoveryTime + estimatedRescanTime
				}

				totalElapsedTime := totalHeadersFetchTime + elapsedDiscoveryTime
				totalProgress := (totalElapsedTime / totalSyncTime) * 100

				remainingAccountDiscoveryTime := math.Round(estimatedDiscoveryTime - elapsedDiscoveryTime)
				if remainingAccountDiscoveryTime < 0 {
					remainingAccountDiscoveryTime = 0
				}

				totalProgressPercent := int32(math.Round(totalProgress))
				totalTimeRemainingSeconds := int64(math.Round(remainingAccountDiscoveryTime + estimatedRescanTime))

				// update address discovery progress, total progress and total time remaining
				mw.syncData.addressDiscoveryProgress.AddressDiscoveryProgress = int32(math.Round(discoveryProgress))
				mw.syncData.addressDiscoveryProgress.TotalSyncProgress = totalProgressPercent
				mw.syncData.addressDiscoveryProgress.TotalTimeRemainingSeconds = totalTimeRemainingSeconds

				mw.publishAddressDiscoveryProgress()

				debugInfo := &DebugInfo{
					int64(math.Round(totalElapsedTime)),
					totalTimeRemainingSeconds,
					int64(math.Round(elapsedDiscoveryTime)),
					int64(math.Round(remainingAccountDiscoveryTime)),
				}
				mw.publishDebugInfo(debugInfo)

				if mw.syncData.showLogs && mw.syncData.syncing {
					// avoid logging same message multiple times
					if totalProgressPercent != lastTotalPercent || totalTimeRemainingSeconds != lastTimeRemaining {
						log.Infof("Syncing %d%%, %s remaining, discovering used addresses.\n",
							totalProgressPercent, CalculateTotalTimeRemaining(totalTimeRemainingSeconds))

						lastTotalPercent = totalProgressPercent
						lastTimeRemaining = totalTimeRemainingSeconds
					}
				}
				mw.syncData.mu.Unlock()
			case <-mw.syncData.addressDiscoveryCompletedOrCanceled:
				mw.syncData.mu.RLock()
				// stop updating time taken and progress for address discovery
				everySecondTicker.Stop()

				if mw.syncData.showLogs && mw.syncData.syncing {
					log.Info("Address discovery complete.")
				}

				mw.syncData.mu.RUnlock()
				return
			}
		}
	}()
}

func (mw *MultiWallet) publishAddressDiscoveryProgress() {
	if !mw.syncData.syncing {
		// ignore if sync is not in progress
		return
	}

	for _, syncProgressListener := range mw.syncData.syncProgressListeners {
		syncProgressListener.OnAddressDiscoveryProgress(&mw.syncData.activeSyncData.addressDiscoveryProgress)
	}
}

func (mw *MultiWallet) discoverAddressesFinished(walletID int) {
	mw.syncData.mu.Lock()
	defer mw.syncData.mu.Unlock()
	if !mw.syncData.syncing {
		// ignore if sync is not in progress
		return
	}

	addressDiscoveryFinishTime := time.Now().Unix()
	mw.syncData.activeSyncData.totalDiscoveryTimeSpent = addressDiscoveryFinishTime - mw.syncData.addressDiscoveryStartTime

	close(mw.syncData.activeSyncData.addressDiscoveryCompletedOrCanceled)
	mw.syncData.activeSyncData.addressDiscoveryCompletedOrCanceled = nil

	loadedWallet, loaded := mw.wallets[walletID].loader.LoadedWallet()
	if loaded { // loaded should always be through
		if !loadedWallet.Locked() {
			loadedWallet.Lock()
			err := mw.markWalletAsDiscoveredAccounts(walletID)
			if err != nil {
				log.Error(err)
			}
		}
	}

}

// Blocks Scan Callbacks

func (mw *MultiWallet) rescanStarted(walletID int) {
	mw.syncData.mu.Lock()
	defer mw.syncData.mu.Unlock()

	if !mw.syncData.syncing {
		// ignore if sync is not in progress
		return
	}

	if mw.syncData.activeSyncData.addressDiscoveryCompletedOrCanceled != nil {
		close(mw.syncData.activeSyncData.addressDiscoveryCompletedOrCanceled)
		mw.syncData.activeSyncData.addressDiscoveryCompletedOrCanceled = nil
	}

	mw.syncData.activeSyncData.syncStage = HeadersRescanSyncStage
	mw.syncData.activeSyncData.rescanStartTime = time.Now().Unix()

	// retain last total progress report from address discovery phase
	mw.syncData.activeSyncData.headersRescanProgress.TotalTimeRemainingSeconds = mw.syncData.activeSyncData.addressDiscoveryProgress.TotalTimeRemainingSeconds
	mw.syncData.activeSyncData.headersRescanProgress.TotalSyncProgress = mw.syncData.activeSyncData.addressDiscoveryProgress.TotalSyncProgress
	mw.syncData.activeSyncData.headersRescanProgress.WalletID = walletID

	if mw.syncData.showLogs && mw.syncData.syncing {
		log.Info("Step 3 of 3 - Scanning block headers")
	}
}

func (mw *MultiWallet) rescanProgress(walletID int, rescannedThrough int32) {
	mw.syncData.mu.Lock()
	defer mw.syncData.mu.Unlock()

	if !mw.syncData.syncing {
		// ignore if sync is not in progress
		return
	}

	wallet := mw.wallets[walletID]

	mw.syncData.activeSyncData.headersRescanProgress.WalletID = walletID
	mw.syncData.activeSyncData.headersRescanProgress.TotalHeadersToScan = wallet.GetBestBlock()

	rescanRate := float64(rescannedThrough) / float64(mw.syncData.activeSyncData.headersRescanProgress.TotalHeadersToScan)
	mw.syncData.activeSyncData.headersRescanProgress.RescanProgress = int32(math.Round(rescanRate * 100))
	mw.syncData.activeSyncData.headersRescanProgress.CurrentRescanHeight = rescannedThrough

	// If there was some period of inactivity,
	// assume that this process started at some point in the future,
	// thereby accounting for the total reported time of inactivity.
	mw.syncData.activeSyncData.rescanStartTime += mw.syncData.activeSyncData.totalInactiveSeconds
	mw.syncData.activeSyncData.totalInactiveSeconds = 0

	elapsedRescanTime := time.Now().Unix() - mw.syncData.activeSyncData.rescanStartTime
	totalElapsedTime := mw.syncData.activeSyncData.headersFetchTimeSpent + mw.syncData.activeSyncData.totalDiscoveryTimeSpent + elapsedRescanTime

	estimatedTotalRescanTime := int64(math.Round(float64(elapsedRescanTime) / rescanRate))
	mw.syncData.activeSyncData.headersRescanProgress.RescanTimeRemaining = estimatedTotalRescanTime - elapsedRescanTime
	totalTimeRemainingSeconds := mw.syncData.activeSyncData.headersRescanProgress.RescanTimeRemaining

	// do not update total time taken and total progress percent if elapsedRescanTime is 0
	// because the estimatedTotalRescanTime will be inaccurate (also 0)
	// which will make the estimatedTotalSyncTime equal to totalElapsedTime
	// giving the wrong impression that the process is complete
	if elapsedRescanTime > 0 {
		estimatedTotalSyncTime := mw.syncData.activeSyncData.headersFetchTimeSpent + mw.syncData.activeSyncData.totalDiscoveryTimeSpent + estimatedTotalRescanTime
		totalProgress := (float64(totalElapsedTime) / float64(estimatedTotalSyncTime)) * 100

		mw.syncData.activeSyncData.headersRescanProgress.TotalTimeRemainingSeconds = totalTimeRemainingSeconds
		mw.syncData.activeSyncData.headersRescanProgress.TotalSyncProgress = int32(math.Round(totalProgress))
	}

	mw.publishHeadersRescanProgress()

	debugInfo := &DebugInfo{
		totalElapsedTime,
		totalTimeRemainingSeconds,
		elapsedRescanTime,
		mw.syncData.activeSyncData.headersRescanProgress.RescanTimeRemaining,
	}
	mw.publishDebugInfo(debugInfo)

	if mw.syncData.showLogs && mw.syncData.syncing {
		log.Infof("Syncing %d%%, %s remaining, scanning %d of %d block headers.\n",
			mw.syncData.activeSyncData.headersRescanProgress.TotalSyncProgress,
			CalculateTotalTimeRemaining(mw.syncData.activeSyncData.headersRescanProgress.TotalTimeRemainingSeconds),
			mw.syncData.activeSyncData.headersRescanProgress.CurrentRescanHeight,
			mw.syncData.activeSyncData.headersRescanProgress.TotalHeadersToScan,
		)
	}
}

func (mw *MultiWallet) publishHeadersRescanProgress() {
	for _, syncProgressListener := range mw.syncData.syncProgressListeners {
		syncProgressListener.OnHeadersRescanProgress(&mw.syncData.activeSyncData.headersRescanProgress)
	}
}

func (mw *MultiWallet) rescanFinished(walletID int) {
	mw.syncData.mu.Lock()
	defer mw.syncData.mu.Unlock()

	if !mw.syncData.syncing {
		// ignore if sync is not in progress
		return
	}

	mw.syncData.activeSyncData.headersRescanProgress.WalletID = walletID
	mw.syncData.activeSyncData.headersRescanProgress.TotalTimeRemainingSeconds = 0
	mw.syncData.activeSyncData.headersRescanProgress.TotalSyncProgress = 100
	mw.publishHeadersRescanProgress()
}

func (mw *MultiWallet) publishDebugInfo(debugInfo *DebugInfo) {
	for _, syncProgressListener := range mw.syncData.syncProgressListeners {
		syncProgressListener.Debug(debugInfo)
	}
}

/** Helper functions start here */

func (mw *MultiWallet) estimateBlockHeadersCountAfter(lastHeaderTime int64) int32 {
	// Use the difference between current time (now) and last reported block time, to estimate total headers to fetch
	timeDifference := time.Now().Unix() - lastHeaderTime
	estimatedHeadersDifference := float64(timeDifference) / float64(mw.syncData.activeSyncData.targetTimePerBlock)

	// return next integer value (upper limit) if estimatedHeadersDifference is a fraction
	return int32(math.Ceil(estimatedHeadersDifference))
}

func (mw *MultiWallet) notifySyncError(err error) {
	mw.resetSyncData()

	mw.syncData.mu.RLock()
	defer mw.syncData.mu.RUnlock()

	for _, syncProgressListener := range mw.syncData.syncProgressListeners {
		syncProgressListener.OnSyncEndedWithError(err)
	}
}

func (mw *MultiWallet) notifySyncCanceled() {
	mw.resetSyncData()

	mw.syncData.mu.RLock()
	defer mw.syncData.mu.RUnlock()

	for _, syncProgressListener := range mw.syncData.syncProgressListeners {
		syncProgressListener.OnSyncCanceled(mw.syncData.restartSyncRequested)
	}
}

func (mw *MultiWallet) resetSyncData() {
	mw.syncData.mu.Lock()
	defer mw.syncData.mu.Unlock()

	mw.syncData.syncing = false
	mw.syncData.synced = false
	mw.syncData.activeSyncData = nil // to be reintialized on next sync

	for _, wallet := range mw.wallets {
		wallet.waiting = true
	}
}

func (mw *MultiWallet) synced(walletID int, synced bool) {
	mw.syncData.mu.Lock()
	defer mw.syncData.mu.Unlock()
	if mw.syncData.synced && synced {
		return
	}

	wallet := mw.wallets[walletID]
	wallet.synced = synced
	wallet.syncing = false
	if mw.OpenedWalletsCount() == mw.SyncedWalletsCount() {
		mw.syncData.syncing = false
		mw.syncData.synced = true
		mw.syncData.activeSyncData = nil // to be reintialized on next sync

		// begin indexing transactions after sync is completed,
		// syncProgressListeners.OnSynced() will be invoked after transactions are indexed
		var txIndexing errgroup.Group
		for _, wallet := range mw.wallets {
			txIndexing.Go(wallet.IndexTransactions)
		}

		go func() {
			err := txIndexing.Wait()
			if err != nil {
				log.Errorf("Tx Index Error: %v", err)
			}

			for _, syncProgressListener := range mw.syncData.syncProgressListeners {
				if mw.IsSynced() {
					syncProgressListener.OnSyncCompleted()
				} else {
					syncProgressListener.OnSyncCanceled(false)
				}
			}
		}()
	}
}
