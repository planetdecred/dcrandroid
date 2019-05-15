package syncprogressestimator

const (
	SyncStateStart    = "start"
	SyncStateProgress = "progress"
	SyncStateFinish   = "finish"
)

type EstimatedSyncProgressListener interface {
	OnPeerConnectedOrDisconnected(numberOfConnectedPeers int32)
	OnHeadersFetchProgress(headersFetchProgress HeadersFetchProgressReport)
	OnAddressDiscoveryProgress(addressDiscoveryProgress AddressDiscoveryProgressReport)
	OnHeadersRescanProgress(headersRescanProgress HeadersRescanProgressReport)
	OnSyncCompleted()
	OnSyncCanceled()
	OnSyncEndedWithError(err string)
	Debug(debugInfo DebugInfo)
}

type GeneralSyncProgress struct {
	TotalSyncProgress         int32 `json:"totalSyncProgress"`
	TotalTimeRemainingSeconds int64 `json:"totalTimeRemainingSeconds"`
}

type HeadersFetchProgressReport struct {
	GeneralSyncProgress
	TotalHeadersToFetch    int32 `json:"totalHeadersToFetch"`
	CurrentHeaderTimestamp int64 `json:"currentHeaderTimestamp"`
	FetchedHeadersCount    int32 `json:"fetchedHeadersCount"`
	HeadersFetchProgress   int32 `json:"headersFetchProgress"`
}

type AddressDiscoveryProgressReport struct {
	GeneralSyncProgress
	AddressDiscoveryProgress int32 `json:"addressDiscoveryProgress"`
}

type HeadersRescanProgressReport struct {
	GeneralSyncProgress
	TotalHeadersToScan  int32 `json:"totalHeadersToScan"`
	RescanProgress      int32 `json:"rescanProgress"`
	CurrentRescanHeight int32 `json:"currentRescanHeight"`
}

type DebugInfo struct {
	TotalTimeElapsed          int64
	TotalTimeRemaining        int64
	CurrentStageTimeElapsed   int64
	CurrentStageTimeRemaining int64
}
