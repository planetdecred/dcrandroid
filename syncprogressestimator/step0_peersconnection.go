package syncprogressestimator

import "fmt"

func (syncListener *SyncProgressEstimator) OnPeerConnected(peerCount int32) {
	syncListener.handlePeerCountUpdate(peerCount)
}

func (syncListener *SyncProgressEstimator) OnPeerDisconnected(peerCount int32) {
	syncListener.handlePeerCountUpdate(peerCount)
}

func (syncListener *SyncProgressEstimator) handlePeerCountUpdate(peerCount int32) {
	syncListener.progressListener.OnPeerConnectedOrDisconnected(peerCount)

	if syncListener.showLog && syncListener.syncing {
		if peerCount == 1 {
			fmt.Printf("Connected to %d peer on %s.\n", peerCount, syncListener.netType)
		} else {
			fmt.Printf("Connected to %d peers on %s.\n", peerCount, syncListener.netType)
		}
	}
}
