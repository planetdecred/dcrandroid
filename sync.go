package dcrlibwallet

import (
	"context"
	"net"
	"strings"
	"sync"

	"github.com/decred/dcrd/addrmgr"
	"github.com/decred/dcrd/rpcclient"
	"github.com/decred/dcrwallet/chain"
	"github.com/decred/dcrwallet/errors"
	"github.com/decred/dcrwallet/p2p"
	"github.com/decred/dcrwallet/spv"
	"github.com/decred/dcrwallet/wallet"
)

type syncData struct {
	mu                    sync.Mutex
	rpcClient             *chain.RPCClient
	cancelSync            context.CancelFunc
	syncProgressListeners []SyncProgressListener
	rescanning            bool
}

type SyncErrorCode int32

const (
	ErrorCodeUnexpectedError SyncErrorCode = iota
	ErrorCodeDeadlineExceeded
)

func (lw *LibWallet) AddSyncProgressListener(syncProgressListener SyncProgressListener) {
	lw.syncProgressListeners = append(lw.syncProgressListeners, syncProgressListener)
}

func (lw *LibWallet) AddEstimatedSyncProgressListener(syncProgressListener EstimatedSyncProgressListener, logEstimatedProgress bool) {
	syncProgressEstimator := SetupSyncProgressEstimator(
		lw.activeNet.Params.Name,
		logEstimatedProgress,
		lw.GetBestBlock,
		lw.GetBestBlockTimeStamp,
		syncProgressListener,
	)

	lw.AddSyncProgressListener(syncProgressEstimator)
}

func (lw *LibWallet) ResetSyncProgressListeners() {
	for _, syncProgressListener := range lw.syncProgressListeners {
		if syncProgressEstimator, ok := syncProgressListener.(*SyncProgressEstimator); ok {
			syncProgressEstimator.Reset()
		}
	}
}

func (lw *LibWallet) SpvSync(peerAddresses string) error {
	loadedWallet, walletLoaded := lw.walletLoader.LoadedWallet()
	if !walletLoaded {
		return errors.New(ErrWalletNotLoaded)
	}

	// Error if the wallet is already syncing with the network.
	currentNetworkBackend, _ := loadedWallet.NetworkBackend()
	if currentNetworkBackend != nil {
		return errors.New(ErrSyncAlreadyInProgress)
	}

	addr := &net.TCPAddr{IP: net.ParseIP("::1"), Port: 0}
	addrManager := addrmgr.New(lw.walletDataDir, net.LookupIP) // TODO: be mindful of tor
	lp := p2p.NewLocalPeer(loadedWallet.ChainParams(), addr, addrManager)

	var validPeerAddresses []string
	if peerAddresses != "" {
		addresses := strings.Split(peerAddresses, ";")
		for _, address := range addresses {
			peerAddress, err := NormalizeAddress(address, lw.activeNet.Params.DefaultPort)
			if err != nil {
				log.Errorf("SPV peer address invalid: %v", err)
			} else {
				validPeerAddresses = append(validPeerAddresses, peerAddress)
			}
		}

		if len(validPeerAddresses) == 0 {
			return errors.New(ErrInvalidPeers)
		}
	}

	// reset sync listeners before starting sync
	// (especially useful if this is not the first sync since the listener was registered)
	lw.ResetSyncProgressListeners()

	syncer := spv.NewSyncer(loadedWallet, lp)
	syncer.SetNotifications(lw.spvSyncNotificationCallbacks(loadedWallet))
	if len(validPeerAddresses) > 0 {
		syncer.SetPersistantPeers(validPeerAddresses)
	}

	loadedWallet.SetNetworkBackend(syncer)
	lw.walletLoader.SetNetworkBackend(syncer)

	ctx, cancel := contextWithShutdownCancel(context.Background())
	lw.cancelSync = cancel

	// syncer.Run uses a wait group to block the thread until sync completes or an error occurs
	go func() {
		err := syncer.Run(ctx)
		if err != nil {
			if err == context.Canceled {
				lw.notifySyncCanceled()
			} else if err == context.DeadlineExceeded {
				lw.notifySyncError(ErrorCodeDeadlineExceeded, errors.E("SPV synchronization deadline exceeded: %v", err))
			} else {
				lw.notifySyncError(ErrorCodeUnexpectedError, err)
			}
		}
	}()

	return nil
}

func (lw *LibWallet) RpcSync(networkAddress string, username string, password string, cert []byte) error {
	loadedWallet, walletLoaded := lw.walletLoader.LoadedWallet()
	if !walletLoaded {
		return errors.New(ErrWalletNotLoaded)
	}

	// Error if the wallet is already syncing with the network.
	currentNetworkBackend, _ := loadedWallet.NetworkBackend()
	if currentNetworkBackend != nil {
		return errors.New(ErrSyncAlreadyInProgress)
	}

	ctx, cancel := contextWithShutdownCancel(context.Background())
	lw.cancelSync = cancel

	chainClient, err := lw.connectToRpcClient(ctx, networkAddress, username, password, cert)
	if err != nil {
		return err
	}

	// reset sync listeners before starting sync
	// (especially useful if this is not the first sync since the listener was registered)
	lw.ResetSyncProgressListeners()

	syncer := chain.NewRPCSyncer(loadedWallet, chainClient)
	syncer.SetNotifications(lw.generalSyncNotificationCallbacks(loadedWallet))

	networkBackend := chain.BackendFromRPCClient(chainClient.Client)
	lw.walletLoader.SetNetworkBackend(networkBackend)
	loadedWallet.SetNetworkBackend(networkBackend)

	// notify sync progress listeners that connected peer count will not be reported because we're using rpc
	for _, syncProgressListener := range lw.syncProgressListeners {
		syncProgressListener.OnPeerDisconnected(-1)
	}

	// syncer.Run uses a wait group to block the thread until sync completes or an error occurs
	go func() {
		err := syncer.Run(ctx, true)
		if err != nil {
			if err == context.Canceled {
				lw.notifySyncCanceled()
			} else if err == context.DeadlineExceeded {
				lw.notifySyncError(ErrorCodeDeadlineExceeded, errors.E("RPC synchronization deadline exceeded: %v", err))
			} else {
				lw.notifySyncError(ErrorCodeUnexpectedError, err)
			}
		}
	}()

	return nil
}

func (lw *LibWallet) connectToRpcClient(ctx context.Context, networkAddress string, username string, password string,
	cert []byte) (chainClient *chain.RPCClient, err error) {

	lw.mu.Lock()
	chainClient = lw.rpcClient
	lw.mu.Unlock()

	// If the rpcClient is already set, you can just use that instead of attempting a new connection.
	if chainClient != nil {
		return
	}

	// rpcClient is not already set, attempt a new connection.
	networkAddress, err = NormalizeAddress(networkAddress, lw.activeNet.JSONRPCClientPort)
	if err != nil {
		return nil, errors.New(ErrInvalidAddress)
	}
	chainClient, err = chain.NewRPCClient(lw.activeNet.Params, networkAddress, username, password, cert, len(cert) == 0)
	if err != nil {
		return nil, translateError(err)
	}

	err = chainClient.Start(ctx, false)
	if err != nil {
		if err == rpcclient.ErrInvalidAuth {
			return nil, errors.New(ErrInvalid)
		}
		if errors.Match(errors.E(context.Canceled), err) {
			return nil, errors.New(ErrContextCanceled)
		}
		return nil, errors.New(ErrUnavailable)
	}

	// Set rpcClient so it can be used subsequently without re-connecting to the rpc server.
	lw.mu.Lock()
	lw.rpcClient = chainClient
	lw.mu.Unlock()

	return
}

func (lw *LibWallet) CancelSync() {
	if lw.cancelSync != nil {
		lw.cancelSync() // will trigger context canceled in rpcSync or spvSync
		lw.cancelSync = nil
	}

	loadedWallet, walletLoaded := lw.walletLoader.LoadedWallet()
	if !walletLoaded {
		return
	}

	lw.walletLoader.SetNetworkBackend(nil)
	loadedWallet.SetNetworkBackend(nil)
}

func (lw *LibWallet) RescanBlocks() error {
	netBackend, err := lw.wallet.NetworkBackend()
	if err != nil {
		return errors.E(ErrNotConnected)
	}

	if lw.rescanning {
		return errors.E(ErrInvalid)
	}

	go func() {
		defer func() {
			lw.rescanning = false
		}()
		lw.rescanning = true
		progress := make(chan wallet.RescanProgress, 1)
		ctx, _ := contextWithShutdownCancel(context.Background())

		var totalHeight int32
		go lw.wallet.RescanProgressFromHeight(ctx, netBackend, 0, progress)

		for p := range progress {
			if p.Err != nil {
				log.Error(p.Err)

				return
			}
			totalHeight += p.ScannedThrough
			for _, syncProgressListener := range lw.syncProgressListeners {
				syncProgressListener.OnRescan(p.ScannedThrough, SyncStateProgress)
			}
		}

		select {
		case <-ctx.Done():
			for _, syncProgressListener := range lw.syncProgressListeners {
				syncProgressListener.OnRescan(totalHeight, SyncStateProgress)
			}
		default:
			for _, syncProgressListener := range lw.syncProgressListeners {
				syncProgressListener.OnRescan(totalHeight, SyncStateFinish)
			}
		}
	}()

	return nil
}

func (lw *LibWallet) GetBestBlock() int32 {
	_, height := lw.wallet.MainChainTip()
	return height
}

func (lw *LibWallet) GetBestBlockTimeStamp() int64 {
	_, height := lw.wallet.MainChainTip()
	identifier := wallet.NewBlockIdentifierFromHeight(height)
	info, err := lw.wallet.BlockInfo(identifier)
	if err != nil {
		log.Error(err)
		return 0
	}
	return info.Timestamp
}
