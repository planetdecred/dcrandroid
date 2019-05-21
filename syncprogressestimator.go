package dcrlibwallet

import (
	"fmt"
	"math"
	"time"
)

const (
	SyncStateStart    = "start"
	SyncStateProgress = "progress"
	SyncStateFinish   = "finish"
)

type SyncProgressEstimator struct {
	netType               string
	getBestBlock          func() int32
	getBestBlockTimestamp func() int64

	showLog bool
	syncing bool

	headersFetchProgress     HeadersFetchProgressReport
	addressDiscoveryProgress AddressDiscoveryProgressReport
	headersRescanProgress    HeadersRescanProgressReport

	progressListener EstimatedSyncProgressListener

	connectedPeers int32

	beginFetchTimeStamp   int64
	startHeaderHeight     int32
	currentHeaderHeight   int32
	headersFetchTimeSpent int64

	addressDiscoveryCompleted chan bool
	totalDiscoveryTimeSpent   int64

	rescanStartTime int64

	totalInactiveSeconds int64
}

// SetupSyncProgressEstimator creates an instance of `SyncProgressEstimator` which implements `SyncProgressListener`.
// The created instance can be registered with `AddSyncProgressCallback` to receive updates during a sync operation.
// The data received via the different `SyncProgressListener` interface methods are used to
// estimate the progress of the current step of the sync operation and the overall sync progress.
// This estimated progress report is made available to the sync initiator via the specified `progressListener` callback.
// If `showLog` is set to true, SyncProgressEstimator also prints calculated progress report to stdout.
func SetupSyncProgressEstimator(netType string, showLog bool, getBestBlock func() int32, getBestBlockTimestamp func() int64,
	progressListener EstimatedSyncProgressListener) *SyncProgressEstimator {

	headersFetchProgress := HeadersFetchProgressReport{}
	headersFetchProgress.GeneralSyncProgress = &GeneralSyncProgress{}

	addressDiscoveryProgress := AddressDiscoveryProgressReport{}
	addressDiscoveryProgress.GeneralSyncProgress = &GeneralSyncProgress{}

	headersRescanProgress := HeadersRescanProgressReport{}
	headersRescanProgress.GeneralSyncProgress = &GeneralSyncProgress{}

	return &SyncProgressEstimator{
		netType:               netType,
		getBestBlock:          getBestBlock,
		getBestBlockTimestamp: getBestBlockTimestamp,

		showLog: showLog,
		syncing: true,

		headersFetchProgress:     headersFetchProgress,
		addressDiscoveryProgress: addressDiscoveryProgress,
		headersRescanProgress:    headersRescanProgress,

		progressListener: progressListener,

		beginFetchTimeStamp:   -1,
		headersFetchTimeSpent: -1,

		totalDiscoveryTimeSpent: -1,
	}

}

func (syncListener *SyncProgressEstimator) Reset() {
	syncListener.syncing = true
	syncListener.beginFetchTimeStamp = -1
	syncListener.headersFetchTimeSpent = -1
	syncListener.totalDiscoveryTimeSpent = -1
}

func (syncListener *SyncProgressEstimator) DiscardPeriodsOfInactivity(totalInactiveSeconds int64) {
	syncListener.totalInactiveSeconds += totalInactiveSeconds
	if syncListener.connectedPeers == 0 {
		// assume it would take another 60 seconds to reconnect to peers
		syncListener.totalInactiveSeconds += 60
	}
}

/**
Following methods satisfy the `SyncProgressListener` interface.
*/
func (syncListener *SyncProgressEstimator) OnFetchMissingCFilters(missingCFiltersStart, missingCFiltersEnd int32, state string) {
}

func (syncListener *SyncProgressEstimator) OnIndexTransactions(totalIndexed int32) {
	if syncListener.showLog && syncListener.syncing {
		fmt.Printf("Indexing transactions. %d done.\n", totalIndexed)
	}
}

func (syncListener *SyncProgressEstimator) OnSynced(synced bool) {
	syncListener.syncing = false

	if synced {
		syncListener.progressListener.OnSyncCompleted()
	} else {
		syncListener.progressListener.OnSyncCanceled()
	}
}

func (syncListener *SyncProgressEstimator) OnSyncEndedWithError(code int32, err error) {
	syncListener.syncing = false

	syncError := fmt.Errorf("code: %d, error: %s", code, err.Error())
	syncListener.progressListener.OnSyncEndedWithError(syncError)
}

// Peer Connections
func (syncListener *SyncProgressEstimator) OnPeerConnected(peerCount int32) {
	syncListener.handlePeerCountUpdate(peerCount)
}

func (syncListener *SyncProgressEstimator) OnPeerDisconnected(peerCount int32) {
	syncListener.handlePeerCountUpdate(peerCount)
}

func (syncListener *SyncProgressEstimator) handlePeerCountUpdate(peerCount int32) {
	syncListener.connectedPeers = peerCount
	syncListener.progressListener.OnPeerConnectedOrDisconnected(peerCount)

	if syncListener.showLog && syncListener.syncing {
		if peerCount == 1 {
			fmt.Printf("Connected to %d peer on %s.\n", peerCount, syncListener.netType)
		} else {
			fmt.Printf("Connected to %d peers on %s.\n", peerCount, syncListener.netType)
		}
	}
}

// Step 1 - Fetch Block Headers
func (syncListener *SyncProgressEstimator) OnFetchedHeaders(fetchedHeadersCount int32, lastHeaderTime int64, state string) {
	if !syncListener.syncing || syncListener.headersFetchTimeSpent != -1 {
		// Ignore this call because this function gets called for each peer and
		// we'd want to ignore those calls as far as the wallet is synced (i.e. !syncListener.syncing)
		// or headers are completely fetched (i.e. syncListener.headersFetchTimeSpent != -1)
		return
	}

	bestBlockTimeStamp := syncListener.getBestBlockTimestamp()
	bestBlock := syncListener.getBestBlock()
	estimatedFinalBlockHeight := estimateFinalBlockHeight(syncListener.netType, bestBlockTimeStamp, bestBlock)

	switch state {
	case SyncStateStart:
		if syncListener.beginFetchTimeStamp != -1 {
			// already started headers fetching
			break
		}

		syncListener.beginFetchTimeStamp = time.Now().Unix()
		syncListener.startHeaderHeight = bestBlock
		syncListener.currentHeaderHeight = syncListener.startHeaderHeight

		if syncListener.showLog && syncListener.syncing {
			totalHeadersToFetch := int32(estimatedFinalBlockHeight) - syncListener.startHeaderHeight
			fmt.Printf("Step 1 of 3 - fetching %d block headers.\n", totalHeadersToFetch)
		}

	case SyncStateProgress:
		// increment current block height value
		syncListener.currentHeaderHeight += fetchedHeadersCount

		// calculate percentage progress and eta
		totalFetchedHeaders := syncListener.currentHeaderHeight
		if syncListener.startHeaderHeight > 0 {
			totalFetchedHeaders -= syncListener.startHeaderHeight
		}

		syncEndPoint := estimatedFinalBlockHeight - syncListener.startHeaderHeight
		headersFetchingRate := float64(totalFetchedHeaders) / float64(syncEndPoint)

		// If there was some period of inactivity,
		// assume that this process started at some point in the future,
		// thereby accounting for the total reported time of inactivity.
		syncListener.beginFetchTimeStamp += syncListener.totalInactiveSeconds
		syncListener.totalInactiveSeconds = 0

		timeTakenSoFar := time.Now().Unix() - syncListener.beginFetchTimeStamp
		estimatedTotalHeadersFetchTime := math.Round(float64(timeTakenSoFar) / headersFetchingRate)

		estimatedRescanTime := math.Round(estimatedTotalHeadersFetchTime * RescanPercentage)
		estimatedDiscoveryTime := math.Round(estimatedTotalHeadersFetchTime * DiscoveryPercentage)
		estimatedTotalSyncTime := estimatedTotalHeadersFetchTime + estimatedRescanTime + estimatedDiscoveryTime

		totalTimeRemainingSeconds := int64(math.Round(estimatedTotalSyncTime)) + timeTakenSoFar
		totalSyncProgress := (float64(timeTakenSoFar) / float64(estimatedTotalSyncTime)) * 100.0

		// update total progress and headers progress sync info
		syncListener.headersFetchProgress.TotalSyncProgress = int32(math.Round(totalSyncProgress))
		syncListener.headersFetchProgress.TotalTimeRemainingSeconds = totalTimeRemainingSeconds
		syncListener.headersFetchProgress.TotalHeadersToFetch = syncEndPoint
		syncListener.headersFetchProgress.CurrentHeaderTimestamp = lastHeaderTime
		syncListener.headersFetchProgress.FetchedHeadersCount = totalFetchedHeaders
		syncListener.headersFetchProgress.HeadersFetchProgress = int32(math.Round(headersFetchingRate * 100))

		syncListener.progressListener.OnHeadersFetchProgress(&syncListener.headersFetchProgress)

		syncListener.progressListener.Debug(&DebugInfo{
			timeTakenSoFar,
			totalTimeRemainingSeconds,
			timeTakenSoFar,
			int64(math.Round(estimatedTotalHeadersFetchTime)),
		})

		if syncListener.showLog && syncListener.syncing {
			fmt.Printf("Syncing %d%%, %s remaining, fetched %d of %d block headers, %s behind.\n",
				syncListener.headersFetchProgress.TotalSyncProgress,
				calculateTotalTimeRemaining(totalTimeRemainingSeconds),
				syncListener.headersFetchProgress.FetchedHeadersCount,
				syncListener.headersFetchProgress.TotalHeadersToFetch,
				calculateDaysBehind(lastHeaderTime),
			)
		}

	case SyncStateFinish:
		syncListener.headersFetchTimeSpent = time.Now().Unix() - syncListener.beginFetchTimeStamp
		syncListener.startHeaderHeight = -1
		syncListener.currentHeaderHeight = -1

		if syncListener.showLog && syncListener.syncing {
			fmt.Println("Fetch headers completed.")
		}
	}
}

// Step 2 - Address Discovery
func (syncListener *SyncProgressEstimator) OnDiscoveredAddresses(state string) {
	if state == SyncStateStart && syncListener.addressDiscoveryCompleted == nil {
		if syncListener.showLog && syncListener.syncing {
			fmt.Println("Step 2 of 3 - discovering used addresses.")
		}
		syncListener.updateAddressDiscoveryProgress()
	} else {
		close(syncListener.addressDiscoveryCompleted)
		syncListener.addressDiscoveryCompleted = nil
	}
}

func (syncListener *SyncProgressEstimator) updateAddressDiscoveryProgress() {
	// these values will be used every second to calculate the total sync progress
	addressDiscoveryStartTime := time.Now().Unix()
	totalHeadersFetchTime := float64(syncListener.headersFetchTimeSpent)
	if totalHeadersFetchTime < 150 {
		// 80% of 150 seconds is 120 seconds.
		// This ensures that minimum estimated discovery time is 120 seconds (2 minutes).
		totalHeadersFetchTime = 150
	}
	estimatedRescanTime := totalHeadersFetchTime * RescanPercentage
	estimatedDiscoveryTime := totalHeadersFetchTime * DiscoveryPercentage

	// following channels are used to determine next step in the below subroutine
	everySecondTicker := time.NewTicker(1 * time.Second)
	everySecondTickerChannel := everySecondTicker.C

	// track last logged time remaining and total percent to avoid re-logging same message
	var lastTimeRemaining int64
	var lastTotalPercent int32 = -1

	syncListener.addressDiscoveryCompleted = make(chan bool)

	go func() {
		for {
			// If there was some period of inactivity,
			// assume that this process started at some point in the future,
			// thereby accounting for the total reported time of inactivity.
			addressDiscoveryStartTime += syncListener.totalInactiveSeconds
			syncListener.totalInactiveSeconds = 0

			select {
			case <-everySecondTickerChannel:
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
				syncListener.addressDiscoveryProgress.AddressDiscoveryProgress = int32(math.Round(discoveryProgress))
				syncListener.addressDiscoveryProgress.TotalSyncProgress = totalProgressPercent
				syncListener.addressDiscoveryProgress.TotalTimeRemainingSeconds = totalTimeRemainingSeconds

				syncListener.progressListener.OnAddressDiscoveryProgress(&syncListener.addressDiscoveryProgress)

				syncListener.progressListener.Debug(&DebugInfo{
					int64(math.Round(totalElapsedTime)),
					totalTimeRemainingSeconds,
					int64(math.Round(elapsedDiscoveryTime)),
					int64(math.Round(remainingAccountDiscoveryTime)),
				})

				if syncListener.showLog && syncListener.syncing {
					// avoid logging same message multiple times
					if totalProgressPercent != lastTotalPercent || totalTimeRemainingSeconds != lastTimeRemaining {
						fmt.Printf("Syncing %d%%, %s remaining, discovering used addresses.\n",
							totalProgressPercent, calculateTotalTimeRemaining(totalTimeRemainingSeconds))

						lastTotalPercent = totalProgressPercent
						lastTimeRemaining = totalTimeRemainingSeconds
					}
				}

			case <-syncListener.addressDiscoveryCompleted:
				// stop updating time taken and progress for address discovery
				everySecondTicker.Stop()

				// update final discovery time taken
				addressDiscoveryFinishTime := time.Now().Unix()
				syncListener.totalDiscoveryTimeSpent = addressDiscoveryFinishTime - addressDiscoveryStartTime

				if syncListener.showLog && syncListener.syncing {
					fmt.Println("Address discovery complete.")
				}

				return
			}
		}
	}()
}

// Step 3 - Rescan Blocks
func (syncListener *SyncProgressEstimator) OnRescan(rescannedThrough int32, state string) {
	if syncListener.addressDiscoveryCompleted != nil {
		close(syncListener.addressDiscoveryCompleted)
		syncListener.addressDiscoveryCompleted = nil
	}

	syncListener.headersRescanProgress.TotalHeadersToScan = syncListener.getBestBlock()

	switch state {
	case SyncStateStart:
		syncListener.rescanStartTime = time.Now().Unix()

		// retain last total progress report from address discovery phase
		syncListener.headersRescanProgress.TotalTimeRemainingSeconds = syncListener.addressDiscoveryProgress.TotalTimeRemainingSeconds
		syncListener.headersRescanProgress.TotalSyncProgress = syncListener.addressDiscoveryProgress.TotalSyncProgress

		if syncListener.showLog && syncListener.syncing {
			fmt.Println("Step 3 of 3 - Scanning block headers")
		}

	case SyncStateProgress:
		rescanRate := float64(rescannedThrough) / float64(syncListener.headersRescanProgress.TotalHeadersToScan)
		syncListener.headersRescanProgress.RescanProgress = int32(math.Round(rescanRate * 100))
		syncListener.headersRescanProgress.CurrentRescanHeight = rescannedThrough

		// If there was some period of inactivity,
		// assume that this process started at some point in the future,
		// thereby accounting for the total reported time of inactivity.
		syncListener.rescanStartTime += syncListener.totalInactiveSeconds
		syncListener.totalInactiveSeconds = 0

		elapsedRescanTime := time.Now().Unix() - syncListener.rescanStartTime
		totalElapsedTime := syncListener.headersFetchTimeSpent + syncListener.totalDiscoveryTimeSpent + elapsedRescanTime

		estimatedTotalRescanTime := float64(elapsedRescanTime) / rescanRate
		estimatedTotalSyncTime := syncListener.headersFetchTimeSpent + syncListener.totalDiscoveryTimeSpent +
			int64(math.Round(estimatedTotalRescanTime))
		totalProgress := (float64(totalElapsedTime) / float64(estimatedTotalSyncTime)) * 100

		totalTimeRemainingSeconds := int64(math.Round(estimatedTotalRescanTime)) + elapsedRescanTime

		// do not update total time taken and total progress percent if elapsedRescanTime is 0
		// because the estimatedTotalRescanTime will be inaccurate (also 0)
		// which will make the estimatedTotalSyncTime equal to totalElapsedTime
		// giving the wrong impression that the process is complete
		if elapsedRescanTime > 0 {
			syncListener.headersRescanProgress.TotalTimeRemainingSeconds = totalTimeRemainingSeconds
			syncListener.headersRescanProgress.TotalSyncProgress = int32(math.Round(totalProgress))
		}

		syncListener.progressListener.Debug(&DebugInfo{
			totalElapsedTime,
			totalTimeRemainingSeconds,
			elapsedRescanTime,
			int64(math.Round(estimatedTotalRescanTime)) - elapsedRescanTime,
		})

		if syncListener.showLog && syncListener.syncing {
			fmt.Printf("Syncing %d%%, %s remaining, scanning %d of %d block headers.\n",
				syncListener.headersRescanProgress.TotalSyncProgress,
				calculateTotalTimeRemaining(syncListener.headersRescanProgress.TotalTimeRemainingSeconds),
				syncListener.headersRescanProgress.CurrentRescanHeight,
				syncListener.headersRescanProgress.TotalHeadersToScan,
			)
		}

	case SyncStateFinish:
		syncListener.headersRescanProgress.TotalTimeRemainingSeconds = 0
		syncListener.headersRescanProgress.TotalSyncProgress = 100

		if syncListener.showLog && syncListener.syncing {
			fmt.Println("Block headers scan complete.")
		}
	}

	syncListener.progressListener.OnHeadersRescanProgress(&syncListener.headersRescanProgress)
}
