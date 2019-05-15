package dcrlibwallet

import (
	"encoding/json"

	"github.com/raedahgroup/dcrlibwallet/syncprogressestimator"
)

type EstimatedSyncProgressJsonListener interface {
	OnPeerConnectedOrDisconnected(numberOfConnectedPeers int32)
	OnHeadersFetchProgress(headersFetchProgressJson string)
	OnAddressDiscoveryProgress(addressDiscoveryProgressJson string)
	OnHeadersRescanProgress(headersRescanProgressJson string)
	OnSyncCompleted()
	OnSyncCanceled()
	OnSyncEndedWithError(err string)
	Debug(totalTimeElapsed, totalTimeRemaining, currentStageTimeElapsed, currentStageTimeRemaining int64)
}

type EstimatedSyncProgressListenerJsonWrapper struct {
	jsonListener EstimatedSyncProgressJsonListener
}

func (wrapper *EstimatedSyncProgressListenerJsonWrapper) OnPeerConnectedOrDisconnected(numberOfConnectedPeers int32) {
	wrapper.jsonListener.OnPeerConnectedOrDisconnected(numberOfConnectedPeers)
}

func (wrapper *EstimatedSyncProgressListenerJsonWrapper) OnHeadersFetchProgress(headersFetchProgress syncprogressestimator.HeadersFetchProgressReport) {
	reportJson, err := json.Marshal(headersFetchProgress)
	if err != nil {
		log.Error("sync progress json marshal error:", err)
		return
	}

	wrapper.jsonListener.OnHeadersFetchProgress(string(reportJson))
}

func (wrapper *EstimatedSyncProgressListenerJsonWrapper) OnAddressDiscoveryProgress(addressDiscoveryProgress syncprogressestimator.AddressDiscoveryProgressReport) {
	reportJson, err := json.Marshal(addressDiscoveryProgress)
	if err != nil {
		log.Error("sync progress json marshal error:", err)
		return
	}

	wrapper.jsonListener.OnAddressDiscoveryProgress(string(reportJson))
}

func (wrapper *EstimatedSyncProgressListenerJsonWrapper) OnHeadersRescanProgress(headersRescanProgress syncprogressestimator.HeadersRescanProgressReport) {
	reportJson, err := json.Marshal(headersRescanProgress)
	if err != nil {
		log.Error("sync progress json marshal error:", err)
		return
	}

	wrapper.jsonListener.OnHeadersRescanProgress(string(reportJson))
}

func (wrapper *EstimatedSyncProgressListenerJsonWrapper) OnSyncCompleted() {
	wrapper.jsonListener.OnSyncCompleted()
}

func (wrapper *EstimatedSyncProgressListenerJsonWrapper) OnSyncCanceled() {
	wrapper.jsonListener.OnSyncCanceled()
}

func (wrapper *EstimatedSyncProgressListenerJsonWrapper) OnSyncEndedWithError(err string) {
	wrapper.jsonListener.OnSyncEndedWithError(err)
}

func (wrapper *EstimatedSyncProgressListenerJsonWrapper) Debug(debugInfo syncprogressestimator.DebugInfo) {
	wrapper.jsonListener.Debug(
		debugInfo.TotalTimeElapsed,
		debugInfo.TotalTimeRemaining,
		debugInfo.CurrentStageTimeElapsed,
		debugInfo.CurrentStageTimeRemaining,
	)
}
