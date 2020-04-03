package dcrlibwallet

import (
	"context"
	"net"
	"strings"
	"sync"

	"github.com/decred/dcrd/addrmgr"
	"github.com/decred/dcrwallet/errors/v2"
	"github.com/decred/dcrwallet/p2p/v2"
	w "github.com/decred/dcrwallet/wallet/v3"
	"github.com/raedahgroup/dcrlibwallet/spv"
)

// reading/writing of properties of this struct are protected by mutex.x
type syncData struct {
	mu sync.RWMutex

	syncProgressListeners map[string]SyncProgressListener
	showLogs              bool

	synced       bool
	syncing      bool
	cancelSync   context.CancelFunc
	cancelRescan context.CancelFunc
	syncCanceled chan bool

	// Flag to notify syncCanceled callback if the sync was canceled so as to be restarted.
	restartSyncRequested bool

	rescanning     bool
	connectedPeers int32

	*activeSyncData
}

// reading/writing of properties of this struct are protected by syncData.mu.
type activeSyncData struct {
	syncStage int32

	headersFetchProgress     HeadersFetchProgressReport
	addressDiscoveryProgress AddressDiscoveryProgressReport
	headersRescanProgress    HeadersRescanProgressReport

	beginFetchTimeStamp   int64
	startHeaderHeight     int32
	headersFetchTimeSpent int64

	addressDiscoveryStartTime int64
	totalDiscoveryTimeSpent   int64

	addressDiscoveryCompletedOrCanceled chan bool

	rescanStartTime int64

	totalInactiveSeconds int64
}

const (
	InvalidSyncStage          = -1
	HeadersFetchSyncStage     = 0
	AddressDiscoverySyncStage = 1
	HeadersRescanSyncStage    = 2
)

func (mw *MultiWallet) initActiveSyncData() {
	headersFetchProgress := HeadersFetchProgressReport{}
	headersFetchProgress.GeneralSyncProgress = &GeneralSyncProgress{}

	addressDiscoveryProgress := AddressDiscoveryProgressReport{}
	addressDiscoveryProgress.GeneralSyncProgress = &GeneralSyncProgress{}

	headersRescanProgress := HeadersRescanProgressReport{}
	headersRescanProgress.GeneralSyncProgress = &GeneralSyncProgress{}

	mw.syncData.mu.Lock()
	mw.syncData.activeSyncData = &activeSyncData{
		syncStage: InvalidSyncStage,

		headersFetchProgress:     headersFetchProgress,
		addressDiscoveryProgress: addressDiscoveryProgress,
		headersRescanProgress:    headersRescanProgress,

		beginFetchTimeStamp:       -1,
		headersFetchTimeSpent:     -1,
		addressDiscoveryStartTime: -1,
		totalDiscoveryTimeSpent:   -1,
	}
	mw.syncData.mu.Unlock()
}

func (mw *MultiWallet) IsSyncProgressListenerRegisteredFor(uniqueIdentifier string) bool {
	mw.syncData.mu.RLock()
	_, exists := mw.syncData.syncProgressListeners[uniqueIdentifier]
	mw.syncData.mu.RUnlock()
	return exists
}

func (mw *MultiWallet) AddSyncProgressListener(syncProgressListener SyncProgressListener, uniqueIdentifier string) error {
	if mw.IsSyncProgressListenerRegisteredFor(uniqueIdentifier) {
		return errors.New(ErrListenerAlreadyExist)
	}

	mw.syncData.mu.Lock()
	mw.syncData.syncProgressListeners[uniqueIdentifier] = syncProgressListener
	mw.syncData.mu.Unlock()

	// If sync is already on, notify this newly added listener of the current progress report.
	return mw.PublishLastSyncProgress(uniqueIdentifier)
}

func (mw *MultiWallet) RemoveSyncProgressListener(uniqueIdentifier string) {
	mw.syncData.mu.Lock()
	delete(mw.syncData.syncProgressListeners, uniqueIdentifier)
	mw.syncData.mu.Unlock()
}

func (mw *MultiWallet) syncProgressListeners() []SyncProgressListener {
	mw.syncData.mu.RLock()
	defer mw.syncData.mu.RUnlock()

	listeners := make([]SyncProgressListener, 0, len(mw.syncData.syncProgressListeners))
	for _, listener := range mw.syncData.syncProgressListeners {
		listeners = append(listeners, listener)
	}

	return listeners
}

func (mw *MultiWallet) PublishLastSyncProgress(uniqueIdentifier string) error {
	mw.syncData.mu.RLock()
	defer mw.syncData.mu.RUnlock()

	syncProgressListener, exists := mw.syncData.syncProgressListeners[uniqueIdentifier]
	if !exists {
		return errors.New(ErrInvalid)
	}

	if mw.syncData.syncing && mw.syncData.activeSyncData != nil {
		switch mw.syncData.activeSyncData.syncStage {
		case HeadersFetchSyncStage:
			syncProgressListener.OnHeadersFetchProgress(&mw.syncData.headersFetchProgress)
		case AddressDiscoverySyncStage:
			syncProgressListener.OnAddressDiscoveryProgress(&mw.syncData.addressDiscoveryProgress)
		case HeadersRescanSyncStage:
			syncProgressListener.OnHeadersRescanProgress(&mw.syncData.headersRescanProgress)
		}
	}

	return nil
}

func (mw *MultiWallet) EnableSyncLogs() {
	mw.syncData.mu.Lock()
	mw.syncData.showLogs = true
	mw.syncData.mu.Unlock()
}

func (mw *MultiWallet) SyncInactiveForPeriod(totalInactiveSeconds int64) {
	mw.syncData.mu.Lock()
	defer mw.syncData.mu.Unlock()

	if !mw.syncData.syncing || mw.syncData.activeSyncData == nil {
		log.Debug("Not accounting for inactive time, wallet is not syncing.")
		return
	}

	mw.syncData.totalInactiveSeconds += totalInactiveSeconds
	if mw.syncData.connectedPeers == 0 {
		// assume it would take another 60 seconds to reconnect to peers
		mw.syncData.totalInactiveSeconds += 60
	}
}

func (mw *MultiWallet) SpvSync() error {
	// prevent an attempt to sync when the previous syncing has not been canceled
	if mw.IsSyncing() || mw.IsSynced() {
		return errors.New(ErrSyncAlreadyInProgress)
	}

	addr := &net.TCPAddr{IP: net.ParseIP("::1"), Port: 0}
	addrManager := addrmgr.New(mw.rootDir, net.LookupIP) // TODO: be mindful of tor
	lp := p2p.NewLocalPeer(mw.chainParams, addr, addrManager)

	var validPeerAddresses []string
	peerAddresses := mw.ReadStringConfigValueForKey(SpvPersistentPeerAddressesConfigKey)
	if peerAddresses != "" {
		addresses := strings.Split(peerAddresses, ";")
		for _, address := range addresses {
			peerAddress, err := NormalizeAddress(address, mw.chainParams.DefaultPort)
			if err != nil {
				log.Errorf("SPV peer address(%s) is invalid: %v", peerAddress, err)
			} else {
				validPeerAddresses = append(validPeerAddresses, peerAddress)
			}
		}

		if len(validPeerAddresses) == 0 {
			return errors.New(ErrInvalidPeers)
		}
	}

	// init activeSyncData to be used to hold data used
	// to calculate sync estimates only during sync
	mw.initActiveSyncData()

	wallets := make(map[int]*w.Wallet)
	for id, wallet := range mw.wallets {
		wallets[id] = wallet.internal
		wallet.waiting = true
		wallet.syncing = true
	}

	syncer := spv.NewSyncer(wallets, lp)
	syncer.SetNotifications(mw.spvSyncNotificationCallbacks())
	if len(validPeerAddresses) > 0 {
		syncer.SetPersistentPeers(validPeerAddresses)
	}

	mw.setNetworkBackend(syncer)

	ctx, cancel := mw.contextWithShutdownCancel()

	var restartSyncRequested bool

	mw.syncData.mu.Lock()
	restartSyncRequested = mw.syncData.restartSyncRequested
	mw.syncData.restartSyncRequested = false
	mw.syncData.syncing = true
	mw.syncData.cancelSync = cancel
	mw.syncData.mu.Unlock()

	for _, listener := range mw.syncProgressListeners() {
		listener.OnSyncStarted(restartSyncRequested)
	}

	// syncer.Run uses a wait group to block the thread until the sync context
	// expires or is canceled or some other error occurs such as
	// losing connection to all persistent peers.
	go func() {
		syncError := syncer.Run(ctx)

		// sync has ended or errored, reset sync variables
		mw.resetSyncData()

		if syncError != nil {
			if syncError == context.DeadlineExceeded {
				mw.notifySyncError(errors.Errorf("SPV synchronization deadline exceeded: %v", syncError))
			} else if syncError == context.Canceled {
				mw.syncData.syncCanceled <- true
				mw.notifySyncCanceled()
			} else {
				mw.notifySyncError(syncError)
			}
		}
	}()
	return nil
}

func (mw *MultiWallet) RestartSpvSync() error {
	mw.syncData.mu.Lock()
	mw.syncData.restartSyncRequested = true
	mw.syncData.mu.Unlock()

	mw.CancelSync() // necessary to unset the network backend.
	return mw.SpvSync()
}

func (mw *MultiWallet) CancelSync() {
	mw.syncData.mu.RLock()
	cancelSync := mw.syncData.cancelSync
	mw.syncData.mu.RUnlock()

	if cancelSync != nil {
		log.Info("Canceling sync. May take a while for sync to fully cancel.")

		// Cancel the context used for syncer.Run in spvSync().
		// This may not immediately cause the sync process to terminate,
		// but when it eventually terminates, syncer.Run will return `err == context.Canceled`.
		cancelSync()

		// When sync terminates and syncer.Run returns `err == context.Canceled`,
		// we will get notified on this channel.
		<-mw.syncData.syncCanceled

		log.Info("Sync fully canceled.")
	}

	for _, libWallet := range mw.wallets {
		loadedWallet, walletLoaded := libWallet.loader.LoadedWallet()
		if !walletLoaded {
			continue
		}

		loadedWallet.SetNetworkBackend(nil)
	}
}

func (wallet *Wallet) IsWaiting() bool {
	return wallet.waiting
}

func (wallet *Wallet) IsSynced() bool {
	return wallet.synced
}

func (wallet *Wallet) IsSyncing() bool {
	return wallet.syncing
}

func (mw *MultiWallet) IsConnectedToDecredNetwork() bool {
	mw.syncData.mu.RLock()
	defer mw.syncData.mu.RUnlock()
	return mw.syncData.syncing || mw.syncData.synced
}

func (mw *MultiWallet) IsSynced() bool {
	mw.syncData.mu.RLock()
	defer mw.syncData.mu.RUnlock()
	return mw.syncData.synced
}

func (mw *MultiWallet) IsSyncing() bool {
	mw.syncData.mu.RLock()
	defer mw.syncData.mu.RUnlock()
	return mw.syncData.syncing
}

func (mw *MultiWallet) CurrentSyncStage() int32 {
	mw.syncData.mu.RLock()
	defer mw.syncData.mu.RUnlock()

	if mw.syncData != nil && mw.syncData.syncing {
		return mw.syncData.syncStage
	}
	return InvalidSyncStage
}

func (mw *MultiWallet) GeneralSyncProgress() *GeneralSyncProgress {
	mw.syncData.mu.RLock()
	defer mw.syncData.mu.RUnlock()

	if mw.syncData != nil && mw.syncData.syncing {
		switch mw.syncData.syncStage {
		case HeadersFetchSyncStage:
			return mw.syncData.headersFetchProgress.GeneralSyncProgress
		case AddressDiscoverySyncStage:
			return mw.syncData.addressDiscoveryProgress.GeneralSyncProgress
		case HeadersRescanSyncStage:
			return mw.syncData.headersRescanProgress.GeneralSyncProgress
		}
	}

	return nil
}

func (mw *MultiWallet) ConnectedPeers() int32 {
	mw.syncData.mu.RLock()
	defer mw.syncData.mu.RUnlock()
	return mw.syncData.connectedPeers
}

func (mw *MultiWallet) GetBestBlock() *BlockInfo {
	var bestBlock int32 = -1
	var blockInfo *BlockInfo
	for _, wallet := range mw.wallets {
		if !wallet.WalletOpened() {
			continue
		}

		walletBestBLock := wallet.GetBestBlock()
		if walletBestBLock > bestBlock || bestBlock == -1 {
			bestBlock = walletBestBLock
			blockInfo = &BlockInfo{Height: bestBlock, Timestamp: wallet.GetBestBlockTimeStamp()}
		}
	}

	return blockInfo
}

func (mw *MultiWallet) GetLowestBlock() *BlockInfo {
	var lowestBlock int32 = -1
	var blockInfo *BlockInfo
	for _, wallet := range mw.wallets {
		if !wallet.WalletOpened() {
			continue
		}
		walletBestBLock := wallet.GetBestBlock()
		if walletBestBLock < lowestBlock || lowestBlock == -1 {
			lowestBlock = walletBestBLock
			blockInfo = &BlockInfo{Height: lowestBlock, Timestamp: wallet.GetBestBlockTimeStamp()}
		}
	}

	return blockInfo
}

func (wallet *Wallet) GetBestBlock() int32 {
	if wallet.internal == nil {
		// This method is sometimes called after a wallet is deleted and causes crash.
		log.Error("Attempting to read best block height without a loaded wallet.")
		return 0
	}

	_, height := wallet.internal.MainChainTip(wallet.shutdownContext())
	return height
}

func (wallet *Wallet) GetBestBlockTimeStamp() int64 {
	if wallet.internal == nil {
		// This method is sometimes called after a wallet is deleted and causes crash.
		log.Error("Attempting to read best block timestamp without a loaded wallet.")
		return 0
	}

	ctx := wallet.shutdownContext()
	_, height := wallet.internal.MainChainTip(ctx)
	identifier := w.NewBlockIdentifierFromHeight(height)
	info, err := wallet.internal.BlockInfo(ctx, identifier)
	if err != nil {
		log.Error(err)
		return 0
	}
	return info.Timestamp
}

func (mw *MultiWallet) GetLowestBlockTimestamp() int64 {
	var timestamp int64 = -1
	for _, wallet := range mw.wallets {
		bestBlockTimestamp := wallet.GetBestBlockTimeStamp()
		if bestBlockTimestamp < timestamp || timestamp == -1 {
			timestamp = bestBlockTimestamp
		}
	}
	return timestamp
}
