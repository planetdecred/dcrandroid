package syncprogressestimator

import (
	"fmt"
	"math"
	"time"
)

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

				syncListener.progressListener.OnAddressDiscoveryProgress(syncListener.addressDiscoveryProgress)

				syncListener.progressListener.Debug(DebugInfo{
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
