package syncprogressestimator

import (
	"fmt"
	"math"
	"time"
)

const (
	// Approximate time (in seconds) to mine a block in mainnet
	MainNetTargetTimePerBlock = 300

	// Approximate time (in seconds) to mine a block in testnet
	TestNetTargetTimePerBlock = 120

	// Use 10% of estimated total headers fetch time to estimate rescan time
	RescanPercentage = 0.1

	// Use 80% of estimated total headers fetch time to estimate address discovery time
	DiscoveryPercentage = 0.8
)

func calculateTotalTimeRemaining(timeRemainingInSeconds int64) string {
	minutes := timeRemainingInSeconds / 60
	if minutes > 0 {
		return fmt.Sprintf("%d min", minutes)
	}
	return fmt.Sprintf("%d sec", timeRemainingInSeconds)
}

func calculateDaysBehind(timestamp int64) string {
	hoursBehind := float64(time.Now().Unix()-timestamp) / 60
	daysBehind := int(math.Round(hoursBehind / 24))
	if daysBehind < 1 {
		return "<1 day"
	} else if daysBehind == 1 {
		return "1 day"
	} else {
		return fmt.Sprintf("%d days", daysBehind)
	}
}

func estimateFinalBlockHeight(netType string, bestBlockTimeStamp int64, bestBlock int32) int32 {
	var targetTimePerBlock int32
	if netType == "mainnet" {
		targetTimePerBlock = MainNetTargetTimePerBlock
	} else {
		targetTimePerBlock = TestNetTargetTimePerBlock
	}

	timeDifference := time.Now().Unix() - bestBlockTimeStamp
	return (int32(timeDifference) / targetTimePerBlock) + bestBlock
}
