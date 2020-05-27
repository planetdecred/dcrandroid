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
	mw.syncData.connectedPeers = peerCount
	shouldLog := mw.syncData.showLogs && mw.syncData.syncing
	mw.syncData.mu.Unlock()

	for _, syncProgressListener := range mw.syncProgressListeners() {
		syncProgressListener.OnPeerConnectedOrDisconnected(peerCount)
	}

	if shouldLog {
		if peerCount == 1 {
			log.Infof("Connected to %d peer on %s.", peerCount, mw.chainParams.Name)
		} else {
			log.Infof("Connected to %d peers on %s.", peerCount, mw.chainParams.Name)
		}
	}
}

// Fetch Headers Callbacks

func (mw *MultiWallet) fetchHeadersStarted(peerInitialHeight int32) {
	if !mw.IsSyncing() {
		return
	}

	mw.syncData.mu.RLock()
	headersFetchingStarted := mw.syncData.beginFetchTimeStamp != -1
	showLogs := mw.syncData.showLogs
	mw.syncData.mu.RUnlock()

	if headersFetchingStarted {
		// This function gets called for each newly connected peer so
		// ignore if headers fetching was already started.
		return
	}

	for _, wallet := range mw.wallets {
		wallet.waiting = true
	}

	lowestBlockHeight := mw.GetLowestBlock().Height

	mw.syncData.mu.Lock()
	mw.syncData.activeSyncData.syncStage = HeadersFetchSyncStage
	mw.syncData.activeSyncData.beginFetchTimeStamp = time.Now().Unix()
	mw.syncData.activeSyncData.startHeaderHeight = lowestBlockHeight
	mw.syncData.mu.Unlock()

	if showLogs {
		log.Infof("Step 1 of 3 - fetching %d block headers.", peerInitialHeight-lowestBlockHeight)
	}
}

func (mw *MultiWallet) fetchHeadersProgress(lastFetchedHeaderHeight int32, lastFetchedHeaderTime int64) {
	if !mw.IsSyncing() {
		return
	}

	mw.syncData.mu.RLock()
	headersFetchingCompleted := mw.syncData.activeSyncData.headersFetchTimeSpent != -1
	mw.syncData.mu.RUnlock()

	if headersFetchingCompleted {
		// This function gets called for each newly connected peer so ignore
		// this call if the headers fetching phase was previously completed.
		return
	}

	for _, wallet := range mw.wallets {
		if wallet.waiting {
			wallet.waiting = wallet.GetBestBlock() > lastFetchedHeaderHeight
		}
	}

	headersLeftToFetch := mw.estimateBlockHeadersCountAfter(lastFetchedHeaderTime)
	totalHeadersToFetch := lastFetchedHeaderHeight + headersLeftToFetch
	headersFetchProgress := float64(lastFetchedHeaderHeight) / float64(totalHeadersToFetch)
	// todo: above equation is potentially flawed because `lastFetchedHeaderHeight`
	// may not be the total number of headers fetched so far in THIS sync operation.
	// it may include headers previously fetched.
	// probably better to compare number of headers fetched so far in THIS sync operation
	// against the estimated number of headers left to fetch in THIS sync operation
	// in order to determine the headers fetch progress so far in THIS sync operation.

	// lock the mutex before reading and writing to mw.syncData.*
	mw.syncData.mu.Lock()

	// If there was some period of inactivity,
	// assume that this process started at some point in the future,
	// thereby accounting for the total reported time of inactivity.
	mw.syncData.activeSyncData.beginFetchTimeStamp += mw.syncData.activeSyncData.totalInactiveSeconds
	mw.syncData.activeSyncData.totalInactiveSeconds = 0

	timeTakenSoFar := time.Now().Unix() - mw.syncData.activeSyncData.beginFetchTimeStamp
	if timeTakenSoFar < 1 {
		timeTakenSoFar = 1
	}
	estimatedTotalHeadersFetchTime := float64(timeTakenSoFar) / headersFetchProgress

	// For some reason, the actual total headers fetch time is more than the predicted/estimated time.
	// Account for this difference by multiplying the estimatedTotalHeadersFetchTime by an incrementing factor.
	// The incrementing factor is inversely proportional to the headers fetch progress,
	// ranging from 0.5 to 0 as headers fetching progress increases from 0 to 1.
	// todo, the above noted (mal)calculation may explain this difference.
	adjustmentFactor := 0.5 * (1 - headersFetchProgress)
	estimatedTotalHeadersFetchTime += estimatedTotalHeadersFetchTime * adjustmentFactor

	estimatedDiscoveryTime := estimatedTotalHeadersFetchTime * DiscoveryPercentage
	estimatedRescanTime := estimatedTotalHeadersFetchTime * RescanPercentage
	estimatedTotalSyncTime := estimatedTotalHeadersFetchTime + estimatedDiscoveryTime + estimatedRescanTime

	totalSyncProgress := float64(timeTakenSoFar) / estimatedTotalSyncTime
	totalTimeRemainingSeconds := int64(math.Round(estimatedTotalSyncTime)) - timeTakenSoFar

	// update headers fetching progress report including total progress percentage and total time remaining
	mw.syncData.activeSyncData.headersFetchProgress.TotalHeadersToFetch = totalHeadersToFetch
	mw.syncData.activeSyncData.headersFetchProgress.CurrentHeaderHeight = lastFetchedHeaderHeight
	mw.syncData.activeSyncData.headersFetchProgress.CurrentHeaderTimestamp = lastFetchedHeaderTime
	mw.syncData.activeSyncData.headersFetchProgress.HeadersFetchProgress = roundUp(headersFetchProgress * 100.0)
	mw.syncData.activeSyncData.headersFetchProgress.TotalSyncProgress = roundUp(totalSyncProgress * 100.0)
	mw.syncData.activeSyncData.headersFetchProgress.TotalTimeRemainingSeconds = totalTimeRemainingSeconds

	// unlock the mutex before issuing notification callbacks to prevent potential deadlock
	// if any invoked callback takes a considerable amount of time to execute.
	mw.syncData.mu.Unlock()

	// notify progress listener of estimated progress report
	mw.publishFetchHeadersProgress()

	// todo: also log report if showLog == true

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
	for _, syncProgressListener := range mw.syncProgressListeners() {
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
	if !mw.IsSyncing() {
		return
	}

	mw.syncData.mu.RLock()
	addressDiscoveryAlreadyStarted := mw.syncData.activeSyncData.addressDiscoveryStartTime != -1
	totalHeadersFetchTime := float64(mw.syncData.headersFetchTimeSpent)
	mw.syncData.mu.RUnlock()

	if addressDiscoveryAlreadyStarted {
		return
	}

	mw.syncData.mu.Lock()
	mw.syncData.activeSyncData.syncStage = AddressDiscoverySyncStage
	mw.syncData.activeSyncData.addressDiscoveryStartTime = time.Now().Unix()
	mw.syncData.activeSyncData.addressDiscoveryProgress.WalletID = walletID
	mw.syncData.addressDiscoveryCompletedOrCanceled = make(chan bool)
	mw.syncData.mu.Unlock()

	go mw.updateAddressDiscoveryProgress(totalHeadersFetchTime)

	if mw.syncData.showLogs {
		log.Info("Step 2 of 3 - discovering used addresses.")
	}
}

func (mw *MultiWallet) updateAddressDiscoveryProgress(totalHeadersFetchTime float64) {
	// use ticker to calculate and broadcast address discovery progress every second
	everySecondTicker := time.NewTicker(1 * time.Second)

	// these values will be used every second to calculate the total sync progress
	estimatedDiscoveryTime := totalHeadersFetchTime * DiscoveryPercentage
	estimatedRescanTime := totalHeadersFetchTime * RescanPercentage

	// track last logged time remaining and total percent to avoid re-logging same message
	var lastTimeRemaining int64
	var lastTotalPercent int32 = -1

	for {
		if !mw.IsSyncing() {
			return
		}

		// If there was some period of inactivity,
		// assume that this process started at some point in the future,
		// thereby accounting for the total reported time of inactivity.
		mw.syncData.mu.Lock()
		mw.syncData.addressDiscoveryStartTime += mw.syncData.totalInactiveSeconds
		mw.syncData.totalInactiveSeconds = 0
		addressDiscoveryStartTime := mw.syncData.addressDiscoveryStartTime
		showLogs := mw.syncData.showLogs
		mw.syncData.mu.Unlock()

		select {
		case <-mw.syncData.addressDiscoveryCompletedOrCanceled:
			// stop calculating and broadcasting address discovery progress
			everySecondTicker.Stop()
			if showLogs {
				log.Info("Address discovery complete.")
			}
			return

		case <-everySecondTicker.C:
			// calculate address discovery progress
			elapsedDiscoveryTime := float64(time.Now().Unix() - addressDiscoveryStartTime)
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
			mw.syncData.mu.Lock()
			mw.syncData.addressDiscoveryProgress.AddressDiscoveryProgress = int32(math.Round(discoveryProgress))
			mw.syncData.addressDiscoveryProgress.TotalSyncProgress = totalProgressPercent
			mw.syncData.addressDiscoveryProgress.TotalTimeRemainingSeconds = totalTimeRemainingSeconds
			mw.syncData.mu.Unlock()

			mw.publishAddressDiscoveryProgress()

			debugInfo := &DebugInfo{
				int64(math.Round(totalElapsedTime)),
				totalTimeRemainingSeconds,
				int64(math.Round(elapsedDiscoveryTime)),
				int64(math.Round(remainingAccountDiscoveryTime)),
			}
			mw.publishDebugInfo(debugInfo)

			if showLogs {
				// avoid logging same message multiple times
				if totalProgressPercent != lastTotalPercent || totalTimeRemainingSeconds != lastTimeRemaining {
					log.Infof("Syncing %d%%, %s remaining, discovering used addresses.",
						totalProgressPercent, CalculateTotalTimeRemaining(totalTimeRemainingSeconds))

					lastTotalPercent = totalProgressPercent
					lastTimeRemaining = totalTimeRemainingSeconds
				}
			}
		}
	}
}

func (mw *MultiWallet) publishAddressDiscoveryProgress() {
	for _, syncProgressListener := range mw.syncProgressListeners() {
		syncProgressListener.OnAddressDiscoveryProgress(&mw.syncData.activeSyncData.addressDiscoveryProgress)
	}
}

func (mw *MultiWallet) discoverAddressesFinished(walletID int) {
	if !mw.IsSyncing() {
		return
	}

	mw.stopUpdatingAddressDiscoveryProgress()
}

func (mw *MultiWallet) stopUpdatingAddressDiscoveryProgress() {
	mw.syncData.mu.Lock()
	if mw.syncData.activeSyncData != nil && mw.syncData.activeSyncData.addressDiscoveryCompletedOrCanceled != nil {
		close(mw.syncData.activeSyncData.addressDiscoveryCompletedOrCanceled)
		mw.syncData.activeSyncData.addressDiscoveryCompletedOrCanceled = nil
		mw.syncData.activeSyncData.totalDiscoveryTimeSpent = time.Now().Unix() - mw.syncData.addressDiscoveryStartTime
	}
	mw.syncData.mu.Unlock()
}

// Blocks Scan Callbacks

func (mw *MultiWallet) rescanStarted(walletID int) {
	mw.stopUpdatingAddressDiscoveryProgress()

	mw.syncData.mu.Lock()
	defer mw.syncData.mu.Unlock()

	if !mw.syncData.syncing {
		// ignore if sync is not in progress
		return
	}

	mw.syncData.activeSyncData.syncStage = HeadersRescanSyncStage
	mw.syncData.activeSyncData.rescanStartTime = time.Now().Unix()

	// retain last total progress report from address discovery phase
	mw.syncData.activeSyncData.headersRescanProgress.TotalTimeRemainingSeconds = mw.syncData.activeSyncData.addressDiscoveryProgress.TotalTimeRemainingSeconds
	mw.syncData.activeSyncData.headersRescanProgress.TotalSyncProgress = mw.syncData.activeSyncData.addressDiscoveryProgress.TotalSyncProgress
	mw.syncData.activeSyncData.headersRescanProgress.WalletID = walletID

	if mw.syncData.showLogs && mw.syncData.syncing {
		log.Info("Step 3 of 3 - Scanning block headers.")
	}
}

func (mw *MultiWallet) rescanProgress(walletID int, rescannedThrough int32) {
	if !mw.IsSyncing() {
		// ignore if sync is not in progress
		return
	}

	wallet := mw.wallets[walletID]
	totalHeadersToScan := wallet.GetBestBlock()

	rescanRate := float64(rescannedThrough) / float64(totalHeadersToScan)

	mw.syncData.mu.Lock()

	// If there was some period of inactivity,
	// assume that this process started at some point in the future,
	// thereby accounting for the total reported time of inactivity.
	mw.syncData.activeSyncData.rescanStartTime += mw.syncData.activeSyncData.totalInactiveSeconds
	mw.syncData.activeSyncData.totalInactiveSeconds = 0

	elapsedRescanTime := time.Now().Unix() - mw.syncData.activeSyncData.rescanStartTime
	estimatedTotalRescanTime := int64(math.Round(float64(elapsedRescanTime) / rescanRate))
	totalTimeRemainingSeconds := estimatedTotalRescanTime - elapsedRescanTime
	totalElapsedTime := mw.syncData.activeSyncData.headersFetchTimeSpent + mw.syncData.activeSyncData.totalDiscoveryTimeSpent + elapsedRescanTime

	mw.syncData.activeSyncData.headersRescanProgress.WalletID = walletID
	mw.syncData.activeSyncData.headersRescanProgress.TotalHeadersToScan = totalHeadersToScan
	mw.syncData.activeSyncData.headersRescanProgress.RescanProgress = int32(math.Round(rescanRate * 100))
	mw.syncData.activeSyncData.headersRescanProgress.CurrentRescanHeight = rescannedThrough
	mw.syncData.activeSyncData.headersRescanProgress.RescanTimeRemaining = totalTimeRemainingSeconds

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

	mw.syncData.mu.Unlock()

	mw.publishHeadersRescanProgress()

	debugInfo := &DebugInfo{
		totalElapsedTime,
		totalTimeRemainingSeconds,
		elapsedRescanTime,
		totalTimeRemainingSeconds,
	}
	mw.publishDebugInfo(debugInfo)

	mw.syncData.mu.RLock()
	if mw.syncData.showLogs {
		log.Infof("Syncing %d%%, %s remaining, scanning %d of %d block headers.",
			mw.syncData.activeSyncData.headersRescanProgress.TotalSyncProgress,
			CalculateTotalTimeRemaining(mw.syncData.activeSyncData.headersRescanProgress.TotalTimeRemainingSeconds),
			mw.syncData.activeSyncData.headersRescanProgress.CurrentRescanHeight,
			mw.syncData.activeSyncData.headersRescanProgress.TotalHeadersToScan,
		)
	}
	mw.syncData.mu.RUnlock()
}

func (mw *MultiWallet) publishHeadersRescanProgress() {
	for _, syncProgressListener := range mw.syncProgressListeners() {
		syncProgressListener.OnHeadersRescanProgress(&mw.syncData.activeSyncData.headersRescanProgress)
	}
}

func (mw *MultiWallet) rescanFinished(walletID int) {
	if !mw.IsSyncing() {
		// ignore if sync is not in progress
		return
	}

	mw.syncData.mu.Lock()
	mw.syncData.activeSyncData.headersRescanProgress.WalletID = walletID
	mw.syncData.activeSyncData.headersRescanProgress.TotalTimeRemainingSeconds = 0
	mw.syncData.activeSyncData.headersRescanProgress.TotalSyncProgress = 100

	// Reset these value so that address discovery would
	// not be skipped for the next wallet.
	mw.syncData.activeSyncData.addressDiscoveryStartTime = -1
	mw.syncData.activeSyncData.totalDiscoveryTimeSpent = -1
	mw.syncData.mu.Unlock()

	mw.publishHeadersRescanProgress()
}

func (mw *MultiWallet) publishDebugInfo(debugInfo *DebugInfo) {
	for _, syncProgressListener := range mw.syncProgressListeners() {
		syncProgressListener.Debug(debugInfo)
	}
}

/** Helper functions start here */

func (mw *MultiWallet) estimateBlockHeadersCountAfter(lastHeaderTime int64) int32 {
	// Use the difference between current time (now) and last reported block time,
	// to estimate total headers to fetch.
	timeDifferenceInSeconds := float64(time.Now().Unix() - lastHeaderTime)
	targetTimePerBlockInSeconds := mw.chainParams.TargetTimePerBlock.Seconds()
	estimatedHeadersDifference := timeDifferenceInSeconds / targetTimePerBlockInSeconds

	// return next integer value (upper limit) if estimatedHeadersDifference is a fraction
	return int32(math.Ceil(estimatedHeadersDifference))
}

func (mw *MultiWallet) notifySyncError(err error) {
	for _, syncProgressListener := range mw.syncProgressListeners() {
		syncProgressListener.OnSyncEndedWithError(err)
	}
}

func (mw *MultiWallet) notifySyncCanceled() {
	mw.syncData.mu.RLock()
	restartSyncRequested := mw.syncData.restartSyncRequested
	mw.syncData.mu.RUnlock()

	for _, syncProgressListener := range mw.syncProgressListeners() {
		syncProgressListener.OnSyncCanceled(restartSyncRequested)
	}
}

func (mw *MultiWallet) resetSyncData() {
	// It's possible that sync ends or errors while address discovery is ongoing.
	// If this happens, it's important to stop the address discovery process before
	// resetting sync data.
	mw.stopUpdatingAddressDiscoveryProgress()

	mw.syncData.mu.Lock()
	mw.syncData.syncing = false
	mw.syncData.synced = false
	mw.syncData.cancelSync = nil
	mw.syncData.activeSyncData = nil
	mw.syncData.mu.Unlock()

	for _, wallet := range mw.wallets {
		wallet.waiting = true
		wallet.LockWallet() // lock wallet if previously unlocked to perform account discovery.
	}
}

func (mw *MultiWallet) synced(walletID int, synced bool) {
	mw.syncData.mu.RLock()
	allWalletsSynced := mw.syncData.synced
	mw.syncData.mu.RUnlock()

	if allWalletsSynced && synced {
		return
	}

	wallet := mw.wallets[walletID]
	wallet.synced = synced
	wallet.syncing = false
	mw.listenForTransactions(wallet.ID)

	if !wallet.internal.Locked() {
		wallet.LockWallet() // lock wallet if previously unlocked to perform account discovery.
		err := mw.markWalletAsDiscoveredAccounts(walletID)
		if err != nil {
			log.Error(err)
		}
	}

	if mw.OpenedWalletsCount() == mw.SyncedWalletsCount() {
		mw.syncData.mu.Lock()
		mw.syncData.syncing = false
		mw.syncData.synced = true
		mw.syncData.mu.Unlock()

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

			for _, syncProgressListener := range mw.syncProgressListeners() {
				if synced {
					syncProgressListener.OnSyncCompleted()
				} else {
					syncProgressListener.OnSyncCanceled(false)
				}
			}
		}()
	}
}
