package syncprogressestimator

import "fmt"

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

	beginFetchTimeStamp   int64
	startHeaderHeight     int32
	currentHeaderHeight   int32
	headersFetchTimeSpent int64

	addressDiscoveryCompleted chan bool
	totalDiscoveryTimeSpent   int64

	rescanStartTime int64
}

// SetupSyncProgressEstimator creates an instance of `SyncProgressEstimator` which implements `SyncProgressListener`.
// The created instance can be registered with `AddSyncProgressCallback` to receive updates during a sync operation.
// The data received via the different `SyncProgressListener` interface methods are used to
// estimate the progress of the current step of the sync operation and the overall sync progress.
// This estimated progress report is made available to the sync initiator via the specified `progressListener` callback.
// If `showLog` is set to true, SyncProgressEstimator also prints calculated progress report to stdout.
func Setup(netType string, showLog bool, getBestBlock func() int32, getBestBlockTimestamp func() int64,
	progressListener EstimatedSyncProgressListener) *SyncProgressEstimator {

	return &SyncProgressEstimator{
		netType:               netType,
		getBestBlock:          getBestBlock,
		getBestBlockTimestamp: getBestBlockTimestamp,

		showLog: showLog,
		syncing: true,

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

/**
Following methods satisfy the `SyncProgressListener` interface.
Other interface methods are implemented in the different step*.go files in this package.
*/
func (syncListener *SyncProgressEstimator) OnFetchMissingCFilters(missingCFiltersStart, missingCFiltersEnd int32, state string) {
}

func (syncListener *SyncProgressEstimator) OnIndexTransactions(totalIndexed int32) {
	if syncListener.showLog && syncListener.syncing {
		fmt.Printf("Indexing transactions. %d done.\n", totalIndexed)
	}
}

func (syncListener *SyncProgressEstimator) OnSynced(synced bool) {
	if !syncListener.syncing {
		// ignore subsequent updates
		return
	}

	syncListener.syncing = false

	if synced {
		syncListener.progressListener.OnSyncCompleted()
	} else {
		syncListener.progressListener.OnSyncCanceled()
	}
}

func (syncListener *SyncProgressEstimator) OnSyncEndedWithError(code int32, err error) {
	if !syncListener.syncing {
		// ignore subsequent updates
		return
	}

	syncListener.syncing = false

	syncError := fmt.Sprintf("Code: %d, Error: %s", code, err.Error())
	syncListener.progressListener.OnSyncEndedWithError(syncError)
}
