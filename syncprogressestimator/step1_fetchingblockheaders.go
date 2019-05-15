package syncprogressestimator

import (
	"fmt"
	"math"
	"time"
)

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

		syncListener.progressListener.OnHeadersFetchProgress(syncListener.headersFetchProgress)

		syncListener.progressListener.Debug(DebugInfo{
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
