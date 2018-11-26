package mobilewallet

import (
	"bytes"
	"context"
	"encoding/base64"
	"encoding/binary"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"math"
	"net"
	"os"
	"os/signal"
	"path/filepath"
	"strings"
	"sync"
	"syscall"
	"time"

	"github.com/decred/dcrd/addrmgr"
	stake "github.com/decred/dcrd/blockchain/stake"
	"github.com/decred/dcrd/chaincfg"
	chainhash "github.com/decred/dcrd/chaincfg/chainhash"
	"github.com/decred/dcrd/dcrec"
	"github.com/decred/dcrd/dcrjson"
	"github.com/decred/dcrd/dcrutil"
	"github.com/decred/dcrd/hdkeychain"
	"github.com/decred/dcrd/rpcclient"
	"github.com/decred/dcrd/txscript"
	"github.com/decred/dcrd/wire"
	"github.com/decred/dcrwallet/chain"
	"github.com/decred/dcrwallet/errors"
	"github.com/decred/dcrwallet/loader"
	"github.com/decred/dcrwallet/netparams"
	"github.com/decred/dcrwallet/p2p"
	"github.com/decred/dcrwallet/spv"
	"github.com/decred/dcrwallet/wallet"
	"github.com/decred/dcrwallet/wallet/txauthor"
	"github.com/decred/dcrwallet/wallet/txrules"
	walletseed "github.com/decred/dcrwallet/walletseed"
	"github.com/decred/slog"
	_ "github.com/raedahgroup/mobilewallet/badgerdb"
)

var shutdownRequestChannel = make(chan struct{})
var shutdownSignaled = make(chan struct{})
var signals = []os.Signal{os.Interrupt, syscall.SIGTERM}

const BlockValid int = 1 << 0

type LibWallet struct {
	dataDir       string
	dbDriver      string
	wallet        *wallet.Wallet
	rpcClient     *chain.RPCClient
	spvSyncer     *spv.Syncer
	loader        *loader.Loader
	mu            sync.Mutex
	activeNet     *netparams.Params
	syncResponses []SpvSyncResponse
	rescannning   bool
}

func NewLibWallet(homeDir string, dbDriver string, netType string) *LibWallet {

	var activeNet *netparams.Params

	if netType == "mainnet" {
		activeNet = &netparams.MainNetParams
	} else {
		activeNet = &netparams.TestNet3Params
	}

	lw := &LibWallet{
		dataDir:   filepath.Join(homeDir, netType),
		dbDriver:  dbDriver,
		activeNet: activeNet,
	}
	errors.Separator = ":: "
	initLogRotator(filepath.Join(homeDir, "/logs/"+netType+"/dcrwallet.log"))
	return lw
}

func (lw *LibWallet) SetLogLevel(loglevel string) {
	_, ok := slog.LevelFromString(loglevel)
	if ok {
		setLogLevels(loglevel)
	}
}

func NormalizeAddress(addr string, defaultPort string) (hostport string, err error) {
	// If the first SplitHostPort errors because of a missing port and not
	// for an invalid host, add the port.  If the second SplitHostPort
	// fails, then a port is not missing and the original error should be
	// returned.
	host, port, origErr := net.SplitHostPort(addr)
	if origErr == nil {
		return net.JoinHostPort(host, port), nil
	}
	addr = net.JoinHostPort(addr, defaultPort)
	_, _, err = net.SplitHostPort(addr)
	if err != nil {
		return "", origErr
	}
	return addr, nil
}

func (lw *LibWallet) UnlockWallet(privPass []byte) error {

	wallet, ok := lw.loader.LoadedWallet()
	if !ok {
		return fmt.Errorf("Wallet has not been loaded")
	}

	defer func() {
		for i := range privPass {
			privPass[i] = 0
		}
	}()

	err := wallet.Unlock(privPass, nil)
	return err
}

func (lw *LibWallet) LockWallet() {
	if lw.wallet.Locked() {
		lw.wallet.Lock()
	}
}

func (lw *LibWallet) ChangePrivatePassphrase(oldPass []byte, newPass []byte) error {
	defer func() {
		for i := range oldPass {
			oldPass[i] = 0
		}

		for i := range newPass {
			newPass[i] = 0
		}
	}()

	err := lw.wallet.ChangePrivatePassphrase(oldPass, newPass)
	if err != nil {
		return translateError(err)
	}
	return nil
}

func (lw *LibWallet) ChangePublicPassphrase(oldPass []byte, newPass []byte) error {
	defer func() {
		for i := range oldPass {
			oldPass[i] = 0
		}

		for i := range newPass {
			newPass[i] = 0
		}
	}()

	if len(oldPass) == 0 {
		oldPass = []byte(wallet.InsecurePubPassphrase)
	}
	if len(newPass) == 0 {
		newPass = []byte(wallet.InsecurePubPassphrase)
	}

	err := lw.wallet.ChangePublicPassphrase(oldPass, newPass)
	if err != nil {
		return translateError(err)
	}
	return nil
}

func (lw *LibWallet) Shutdown() {
	log.Info("Shuting down mobile wallet")
	if lw.rpcClient != nil {
		lw.rpcClient.Stop()
	}
	close(shutdownSignaled)
	if logRotator != nil {
		log.Infof("Shutting down log rotator")
		logRotator.Close()
	}
	err := lw.loader.UnloadWallet()
	if err != nil {
		log.Errorf("Failed to close wallet: %v", err)
	} else {
		log.Infof("Closed wallet")
	}
	os.Exit(0)
}

func shutdownListener() {
	interruptChannel := make(chan os.Signal, 1)
	signal.Notify(interruptChannel, signals...)

	// Listen for the initial shutdown signal
	select {
	case sig := <-interruptChannel:
		log.Infof("Received signal (%s).  Shutting down...", sig)
	case <-shutdownRequestChannel:
		log.Info("Shutdown requested.  Shutting down...")
	}

	// Cancel all contexts created from withShutdownCancel.
	close(shutdownSignaled)

	// Listen for any more shutdown signals and log that shutdown has already
	// been signaled.
	for {
		select {
		case <-interruptChannel:
		case <-shutdownRequestChannel:
		}
		log.Info("Shutdown signaled.  Already shutting down...")
	}
}

func contextWithShutdownCancel(ctx context.Context) context.Context {
	ctx, cancel := context.WithCancel(ctx)
	go func() {
		<-shutdownSignaled
		cancel()
	}()
	return ctx
}

func decodeAddress(a string, params *chaincfg.Params) (dcrutil.Address, error) {
	addr, err := dcrutil.DecodeAddress(a)
	if err != nil {
		return nil, err
	}
	if !addr.IsForNet(params) {
		return nil, fmt.Errorf("address %v is not intended for use on %v",
			a, params.Name)
	}
	return addr, nil
}

func (lw *LibWallet) InitLoader() {
	stakeOptions := &loader.StakeOptions{
		VotingEnabled: false,
		AddressReuse:  false,
		VotingAddress: nil,
		TicketFee:     10e8,
	}
	fmt.Println("Initizing Loader: ", lw.dataDir, "Db: ", lw.dbDriver)
	l := loader.NewLoader(lw.activeNet.Params, lw.dataDir, stakeOptions,
		20, false, 10e5, wallet.DefaultAccountGapLimit)
	l.SetDatabaseDriver(lw.dbDriver)
	lw.loader = l
	go shutdownListener()
}

func (lw *LibWallet) CreateWallet(passphrase string, seedMnemonic string) error {
	log.Info("Creating Wallet")
	if len(seedMnemonic) == 0 {
		return errors.New(ErrEmptySeed)
	}
	pubPass := []byte(wallet.InsecurePubPassphrase)
	privPass := []byte(passphrase)
	seed, err := walletseed.DecodeUserInput(seedMnemonic)
	if err != nil {
		log.Error(err)
		return err
	}

	w, err := lw.loader.CreateNewWallet(pubPass, privPass, seed)
	if err != nil {
		log.Error(err)
		return err
	}
	lw.wallet = w

	log.Info("Created Wallet")
	return nil
}

func (lw *LibWallet) CloseWallet() error {
	err := lw.loader.UnloadWallet()
	return err
}

func (lw *LibWallet) GenerateSeed() (string, error) {
	seed, err := hdkeychain.GenerateSeed(hdkeychain.RecommendedSeedLen)
	if err != nil {
		log.Error(err)
		return "", err
	}

	return walletseed.EncodeMnemonic(seed), nil
}

func (lw *LibWallet) VerifySeed(seedMnemonic string) bool {
	_, err := walletseed.DecodeUserInput(seedMnemonic)
	return err == nil
}

func (lw *LibWallet) AddSyncResponse(syncResponse SpvSyncResponse) {
	lw.syncResponses = append(lw.syncResponses, syncResponse)
}

func (lw *LibWallet) SpvSync(peerAddresses string) error {
	wallet, ok := lw.loader.LoadedWallet()
	if !ok {
		return errors.New(ErrWalletNotLoaded)
	}

	addr := &net.TCPAddr{IP: net.ParseIP("::1"), Port: 0}
	amgrDir := filepath.Join(lw.dataDir, lw.wallet.ChainParams().Name)
	amgr := addrmgr.New(amgrDir, net.LookupIP) // TODO: be mindful of tor
	lp := p2p.NewLocalPeer(wallet.ChainParams(), addr, amgr)

	ntfns := &spv.Notifications{
		Synced: func(sync bool) {
			for _, syncResponse := range lw.syncResponses {
				syncResponse.OnSynced(sync)
			}
		},
		FetchHeadersStarted: func() {
			for _, syncResponse := range lw.syncResponses {
				syncResponse.OnFetchedHeaders(0, 0, START)
			}
		},
		FetchHeadersProgress: func(fetchedHeadersCount int32, lastHeaderTime int64) {
			for _, syncResponse := range lw.syncResponses {
				syncResponse.OnFetchedHeaders(fetchedHeadersCount, lastHeaderTime, PROGRESS)
			}
		},
		FetchHeadersFinished: func() {
			for _, syncResponse := range lw.syncResponses {
				syncResponse.OnFetchedHeaders(0, 0, FINISH)
			}
		},
		FetchMissingCFiltersStarted: func() {
			for _, syncResponse := range lw.syncResponses {
				syncResponse.OnFetchMissingCFilters(0, 0, START)
			}
		},
		FetchMissingCFiltersProgress: func(missingCFitlersStart, missingCFitlersEnd int32) {
			for _, syncResponse := range lw.syncResponses {
				syncResponse.OnFetchMissingCFilters(missingCFitlersStart, missingCFitlersEnd, PROGRESS)
			}
		},
		FetchMissingCFiltersFinished: func() {
			for _, syncResponse := range lw.syncResponses {
				syncResponse.OnFetchMissingCFilters(0, 0, FINISH)
			}
		},
		DiscoverAddressesStarted: func() {
			for _, syncResponse := range lw.syncResponses {
				syncResponse.OnDiscoveredAddresses(START)
			}
		},
		DiscoverAddressesFinished: func() {
			for _, syncResponse := range lw.syncResponses {
				syncResponse.OnDiscoveredAddresses(FINISH)
			}

			if !wallet.Locked() {
				wallet.Lock()
			}
		},
		RescanStarted: func() {
			for _, syncResponse := range lw.syncResponses {
				syncResponse.OnRescan(0, START)
			}
		},
		RescanProgress: func(rescannedThrough int32) {
			for _, syncResponse := range lw.syncResponses {
				syncResponse.OnRescan(rescannedThrough, PROGRESS)
			}
		},
		RescanFinished: func() {
			for _, syncResponse := range lw.syncResponses {
				syncResponse.OnRescan(0, FINISH)
			}
		},
		PeerDisconnected: func(peerCount int32, addr string) {
			for _, syncResponse := range lw.syncResponses {
				syncResponse.OnPeerDisconnected(peerCount)
			}
		},
		PeerConnected: func(peerCount int32, addr string) {
			for _, syncResponse := range lw.syncResponses {
				syncResponse.OnPeerConnected(peerCount)
			}
		},
	}
	var spvConnect []string
	if len(peerAddresses) > 0 {
		spvConnect = strings.Split(peerAddresses, ";")
	}
	go func() {
		syncer := spv.NewSyncer(wallet, lp)
		syncer.SetNotifications(ntfns)
		if len(spvConnect) > 0 {
			spvConnects := make([]string, len(spvConnect))
			for i := 0; i < len(spvConnect); i++ {
				spvConnect, err := NormalizeAddress(spvConnect[i], lw.activeNet.Params.DefaultPort)
				if err != nil {
					for _, syncResponse := range lw.syncResponses {
						syncResponse.OnSyncError(3, errors.E("SPV Connect address invalid: %v", err))
					}
					return
				}
				spvConnects[i] = spvConnect
			}
			syncer.SetPersistantPeers(spvConnects)
		}
		wallet.SetNetworkBackend(syncer)
		lw.loader.SetNetworkBackend(syncer)
		ctx := contextWithShutdownCancel(context.Background())
		err := syncer.Run(ctx)
		if err != nil {
			if err == context.Canceled {
				for _, syncResponse := range lw.syncResponses {
					syncResponse.OnSyncError(1, errors.E("SPV synchronization canceled: %v", err))
				}

				return
			} else if err == context.DeadlineExceeded {
				for _, syncResponse := range lw.syncResponses {
					syncResponse.OnSyncError(2, errors.E("SPV synchronization deadline exceeded: %v", err))
				}
				return
			}
			for _, syncResponse := range lw.syncResponses {
				syncResponse.OnSyncError(-1, err)
			}
			return
		}
	}()
	return nil
}

func (lw *LibWallet) RpcSync(networkAddress string, username string, password string, cert []byte) error {

	// Error if the wallet is already syncing with the network.
	wallet, walletLoaded := lw.loader.LoadedWallet()
	if walletLoaded {
		_, err := wallet.NetworkBackend()
		if err == nil {
			return errors.New(ErrFailedPrecondition)
		}
	}

	lw.mu.Lock()
	chainClient := lw.rpcClient
	lw.mu.Unlock()

	ctx := contextWithShutdownCancel(context.Background())
	// If the rpcClient is already set, you can just use that instead of attempting a new connection.
	if chainClient == nil {
		networkAddress, err := NormalizeAddress(networkAddress, lw.activeNet.JSONRPCClientPort)
		if err != nil {
			return errors.New(ErrInvalidAddress)
		}
		chainClient, err = chain.NewRPCClient(lw.activeNet.Params, networkAddress, username,
			password, cert, len(cert) == 0)
		if err != nil {
			return translateError(err)
		}

		err = chainClient.Start(ctx, false)
		if err != nil {
			if err == rpcclient.ErrInvalidAuth {
				return errors.New(ErrInvalid)
			}
			if errors.Match(errors.E(context.Canceled), err) {
				return errors.New(ErrContextCanceled)
			}
			return errors.New(ErrUnavailable)
		}
		lw.mu.Lock()
		lw.rpcClient = chainClient
		lw.mu.Unlock()
	}

	n := chain.BackendFromRPCClient(chainClient.Client)
	lw.loader.SetNetworkBackend(n)
	wallet.SetNetworkBackend(n)

	// Disassociate the RPC client from all subsystems until reconnection
	// occurs.
	defer lw.wallet.SetNetworkBackend(nil)
	defer lw.loader.SetNetworkBackend(nil)
	defer lw.loader.StopTicketPurchase()

	ntfns := &chain.Notifications{
		Synced: func(sync bool) {
			for _, syncResponse := range lw.syncResponses {
				syncResponse.OnSynced(sync)
			}
		},
		FetchMissingCFiltersStarted: func() {
			for _, syncResponse := range lw.syncResponses {
				syncResponse.OnFetchMissingCFilters(0, 0, START)
			}
		},
		FetchMissingCFiltersProgress: func(missingCFitlersStart, missingCFitlersEnd int32) {
			for _, syncResponse := range lw.syncResponses {
				syncResponse.OnFetchMissingCFilters(missingCFitlersStart, missingCFitlersEnd, PROGRESS)
			}
		},
		FetchMissingCFiltersFinished: func() {
			for _, syncResponse := range lw.syncResponses {
				syncResponse.OnFetchMissingCFilters(0, 0, FINISH)
			}
		},
		FetchHeadersStarted: func() {
			for _, syncResponse := range lw.syncResponses {
				syncResponse.OnFetchedHeaders(0, 0, START)
			}
		},
		FetchHeadersProgress: func(fetchedHeadersCount int32, lastHeaderTime int64) {
			for _, syncResponse := range lw.syncResponses {
				syncResponse.OnFetchedHeaders(fetchedHeadersCount, lastHeaderTime, PROGRESS)
			}
		},
		FetchHeadersFinished: func() {
			for _, syncResponse := range lw.syncResponses {
				syncResponse.OnFetchedHeaders(0, 0, FINISH)
			}
		},
		DiscoverAddressesStarted: func() {
			for _, syncResponse := range lw.syncResponses {
				syncResponse.OnDiscoveredAddresses(START)
			}
		},
		DiscoverAddressesFinished: func() {
			for _, syncResponse := range lw.syncResponses {
				syncResponse.OnDiscoveredAddresses(FINISH)
			}

			if !wallet.Locked() {
				wallet.Lock()
			}
		},
		RescanStarted: func() {
			for _, syncResponse := range lw.syncResponses {
				syncResponse.OnRescan(0, START)
			}
		},
		RescanProgress: func(rescannedThrough int32) {
			for _, syncResponse := range lw.syncResponses {
				syncResponse.OnRescan(rescannedThrough, PROGRESS)
			}
		},
		RescanFinished: func() {
			for _, syncResponse := range lw.syncResponses {
				syncResponse.OnRescan(0, FINISH)
			}
		},
	}
	syncer := chain.NewRPCSyncer(wallet, chainClient)
	syncer.SetNotifications(ntfns)

	go func() {
		// Run wallet synchronization until it is cancelled or errors.  If the
		// context was cancelled, return immediately instead of trying to
		// reconnect.
		err := syncer.Run(ctx, true)
		if err != nil {
			if err == context.Canceled {
				for _, syncResponse := range lw.syncResponses {
					syncResponse.OnSyncError(1, errors.E("SPV synchronization canceled: %v", err))
				}

				return
			} else if err == context.DeadlineExceeded {
				for _, syncResponse := range lw.syncResponses {
					syncResponse.OnSyncError(2, errors.E("SPV synchronization deadline exceeded: %v", err))
				}

				return
			}
			for _, syncResponse := range lw.syncResponses {
				syncResponse.OnSyncError(-1, err)
			}
		}
	}()

	return nil
}

func done(ctx context.Context) bool {
	select {
	case <-ctx.Done():
		return true
	default:
		return false
	}
}

func (lw *LibWallet) OpenWallet(pubPass []byte) error {

	w, err := lw.loader.OpenExistingWallet(pubPass)
	if err != nil {
		log.Error(err)
		return translateError(err)
	}
	lw.wallet = w
	return nil
}

func (lw *LibWallet) RescanBlocks() error {
	netBackend, err := lw.wallet.NetworkBackend()
	if err != nil {
		return errors.E(ErrNotConnected)
	}

	if lw.rescannning {
		return errors.E(ErrInvalid)
	}

	go func() {
		defer func() {
			lw.rescannning = false
		}()
		lw.rescannning = true
		progress := make(chan wallet.RescanProgress, 1)
		ctx := contextWithShutdownCancel(context.Background())
		var totalHeight int32
		go lw.wallet.RescanProgressFromHeight(ctx, netBackend, 0, progress)
		for p := range progress {
			if p.Err != nil {
				log.Error(p.Err)

				return
			}
			totalHeight += p.ScannedThrough
			for _, response := range lw.syncResponses {
				response.OnRescan(p.ScannedThrough, PROGRESS)
			}
		}
		select {
		case <-ctx.Done():
			for _, response := range lw.syncResponses {
				response.OnRescan(totalHeight, PROGRESS)
			}
		default:
			for _, response := range lw.syncResponses {
				response.OnRescan(totalHeight, FINISH)
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

func (lw *LibWallet) TransactionNotification(listener TransactionListener) {
	go func() {
		n := lw.wallet.NtfnServer.TransactionNotifications()
		defer n.Done()
		for {
			v := <-n.C
			for _, transaction := range v.UnminedTransactions {
				var amount int64
				var inputAmounts int64
				var outputAmounts int64
				tempCredits := make([]TransactionCredit, len(transaction.MyOutputs))
				for index, credit := range transaction.MyOutputs {
					outputAmounts += int64(credit.Amount)
					tempCredits[index] = TransactionCredit{
						Index:    int32(credit.Index),
						Account:  int32(credit.Account),
						Internal: credit.Internal,
						Amount:   int64(credit.Amount),
						Address:  credit.Address.String()}
				}
				tempDebits := make([]TransactionDebit, len(transaction.MyInputs))
				for index, debit := range transaction.MyInputs {
					inputAmounts += int64(debit.PreviousAmount)
					tempDebits[index] = TransactionDebit{
						Index:           int32(debit.Index),
						PreviousAccount: int32(debit.PreviousAccount),
						PreviousAmount:  int64(debit.PreviousAmount),
						AccountName:     lw.AccountName(int32(debit.PreviousAccount))}
				}
				var direction int32
				amountDifference := outputAmounts - inputAmounts
				if amountDifference < 0 && (float64(transaction.Fee) == math.Abs(float64(amountDifference))) {
					//Transfered
					direction = 2
					amount = int64(transaction.Fee)
				} else if amountDifference > 0 {
					//Received
					direction = 1
					amount = outputAmounts
				} else {
					//Sent
					direction = 0
					amount = inputAmounts
					amount -= outputAmounts
					amount -= int64(transaction.Fee)
				}
				tempTransaction := Transaction{
					Fee:       int64(transaction.Fee),
					Hash:      fmt.Sprintf("%02x", reverse(transaction.Hash[:])),
					Raw:       fmt.Sprintf("%02x", transaction.Transaction[:]),
					Timestamp: transaction.Timestamp,
					Type:      transactionType(transaction.Type),
					Credits:   &tempCredits,
					Amount:    amount,
					Height:    -1,
					Direction: direction,
					Debits:    &tempDebits}
				fmt.Println("New Transaction")
				result, err := json.Marshal(tempTransaction)
				if err != nil {
					log.Error(err)
				} else {
					listener.OnTransaction(string(result))
				}
			}
			for _, block := range v.AttachedBlocks {
				listener.OnBlockAttached(int32(block.Header.Height), block.Header.Timestamp.UnixNano())
				for _, transaction := range block.Transactions {
					listener.OnTransactionConfirmed(fmt.Sprintf("%02x", reverse(transaction.Hash[:])), int32(block.Header.Height))
				}
			}
		}
	}()
}

func (lw *LibWallet) GetTransaction(txHash []byte) (string, error) {
	hash, err := chainhash.NewHash(txHash)
	if err != nil {
		log.Error(err)
		return "", err
	}

	txSummary, _, blockHash, err := lw.wallet.TransactionSummary(hash)
	if err != nil {
		log.Error(err)
		return "", err
	}

	var inputTotal int64
	var outputTotal int64
	var amount int64

	credits := make([]TransactionCredit, len(txSummary.MyOutputs))
	for index, credit := range txSummary.MyOutputs {
		outputTotal += int64(credit.Amount)
		credits[index] = TransactionCredit{
			Index:    int32(credit.Index),
			Account:  int32(credit.Account),
			Internal: credit.Internal,
			Amount:   int64(credit.Amount),
			Address:  credit.Address.String()}
	}

	debits := make([]TransactionDebit, len(txSummary.MyInputs))
	for index, debit := range txSummary.MyInputs {
		inputTotal += int64(debit.PreviousAmount)
		debits[index] = TransactionDebit{
			Index:           int32(debit.Index),
			PreviousAccount: int32(debit.PreviousAccount),
			PreviousAmount:  int64(debit.PreviousAmount),
			AccountName:     lw.AccountName(int32(debit.PreviousAccount))}
	}

	var direction int32
	if txSummary.Type == wallet.TransactionTypeRegular {
		amountDifference := outputTotal - inputTotal
		if amountDifference < 0 && (float64(txSummary.Fee) == math.Abs(float64(amountDifference))) {
			//Transfered
			direction = 2
			amount = int64(txSummary.Fee)
		} else if amountDifference > 0 {
			//Received
			direction = 1
			amount = outputTotal
		} else {
			//Sent
			direction = 0
			amount = inputTotal
			amount -= outputTotal

			amount -= int64(txSummary.Fee)
		}
	}

	var height int32 = -1
	if blockHash != nil {
		blockIdentifier := wallet.NewBlockIdentifierFromHash(blockHash)
		blockInfo, err := lw.wallet.BlockInfo(blockIdentifier)
		if err != nil {
			log.Error(err)
		} else {
			height = blockInfo.Height
		}
	}

	transaction := Transaction{
		Fee:       int64(txSummary.Fee),
		Hash:      fmt.Sprintf("%02x", reverse(txSummary.Hash[:])),
		Raw:       fmt.Sprintf("%02x", txSummary.Transaction[:]),
		Timestamp: txSummary.Timestamp,
		Type:      transactionType(txSummary.Type),
		Credits:   &credits,
		Amount:    amount,
		Height:    height,
		Direction: direction,
		Debits:    &debits}

	result, err := json.Marshal(transaction)

	if err != nil {
		return "", err
	}

	return string(result), nil
}

func (lw *LibWallet) GetTransactions(response GetTransactionsResponse) error {
	ctx := contextWithShutdownCancel(context.Background())
	var startBlock, endBlock *wallet.BlockIdentifier
	transactions := make([]Transaction, 0)
	rangeFn := func(block *wallet.Block) (bool, error) {
		for _, transaction := range block.Transactions {
			var inputAmounts int64
			var outputAmounts int64
			var amount int64
			tempCredits := make([]TransactionCredit, len(transaction.MyOutputs))
			for index, credit := range transaction.MyOutputs {
				outputAmounts += int64(credit.Amount)
				tempCredits[index] = TransactionCredit{
					Index:    int32(credit.Index),
					Account:  int32(credit.Account),
					Internal: credit.Internal,
					Amount:   int64(credit.Amount),
					Address:  credit.Address.String()}
			}
			tempDebits := make([]TransactionDebit, len(transaction.MyInputs))
			for index, debit := range transaction.MyInputs {
				inputAmounts += int64(debit.PreviousAmount)
				tempDebits[index] = TransactionDebit{
					Index:           int32(debit.Index),
					PreviousAccount: int32(debit.PreviousAccount),
					PreviousAmount:  int64(debit.PreviousAmount),
					AccountName:     lw.AccountName(int32(debit.PreviousAccount))}
			}

			var direction int32
			if transaction.Type == wallet.TransactionTypeRegular {
				amountDifference := outputAmounts - inputAmounts
				if amountDifference < 0 && (float64(transaction.Fee) == math.Abs(float64(amountDifference))) {
					//Transfered
					direction = 2
					amount = int64(transaction.Fee)
				} else if amountDifference > 0 {
					//Received
					direction = 1
					for _, credit := range transaction.MyOutputs {
						amount += int64(credit.Amount)
					}
				} else {
					//Sent
					direction = 0
					for _, debit := range transaction.MyInputs {
						amount += int64(debit.PreviousAmount)
					}
					for _, credit := range transaction.MyOutputs {
						amount -= int64(credit.Amount)
					}
					amount -= int64(transaction.Fee)
				}
			}
			var height int32 = -1
			if block.Header != nil {
				height = int32(block.Header.Height)
			}
			tempTransaction := Transaction{
				Fee:       int64(transaction.Fee),
				Hash:      fmt.Sprintf("%02x", reverse(transaction.Hash[:])),
				Raw:       fmt.Sprintf("%02x", transaction.Transaction[:]),
				Timestamp: transaction.Timestamp,
				Type:      transactionType(transaction.Type),
				Credits:   &tempCredits,
				Amount:    amount,
				Height:    height,
				Direction: direction,
				Debits:    &tempDebits}
			transactions = append(transactions, tempTransaction)
		}
		select {
		case <-ctx.Done():
			return true, ctx.Err()
		default:
			return false, nil
		}
	}
	err := lw.wallet.GetTransactions(rangeFn, startBlock, endBlock)
	result, _ := json.Marshal(getTransactionsResponse{ErrorOccurred: false, Transactions: transactions})
	response.OnResult(string(result))
	return err
}

func (lw *LibWallet) DecodeTransaction(txHash []byte) (string, error) {
	hash, err := chainhash.NewHash(txHash)
	if err != nil {
		log.Error(err)
		return "", err
	}
	txSummary, _, _, err := lw.wallet.TransactionSummary(hash)
	if err != nil {
		log.Error(err)
		return "", err
	}
	serializedTx := txSummary.Transaction
	var mtx wire.MsgTx
	err = mtx.Deserialize(bytes.NewReader(serializedTx))
	if err != nil {
		log.Error(err)
		return "", err
	}

	var ssGenVersion uint32
	var lastBlockValid bool
	var votebits string
	if stake.IsSSGen(&mtx) {
		ssGenVersion = voteVersion(&mtx)
		lastBlockValid = voteBits(&mtx)&uint16(BlockValid) != 0
		votebits = fmt.Sprintf("%#04x", voteBits(&mtx))
	}

	var tx = DecodedTransaction{
		Hash:           fmt.Sprintf("%02x", reverse(hash[:])),
		Type:           transactionType(wallet.TxTransactionType(&mtx)),
		Version:        int32(mtx.Version),
		LockTime:       int32(mtx.LockTime),
		Expiry:         int32(mtx.Expiry),
		Inputs:         decodeTxInputs(&mtx),
		Outputs:        decodeTxOutputs(&mtx, lw.wallet.ChainParams()),
		VoteVersion:    int32(ssGenVersion),
		LastBlockValid: lastBlockValid,
		VoteBits:       votebits,
	}
	result, _ := json.Marshal(tx)
	return string(result), nil
}

func decodeTxInputs(mtx *wire.MsgTx) []DecodedInput {
	inputs := make([]DecodedInput, len(mtx.TxIn))
	for i, txIn := range mtx.TxIn {
		inputs[i] = DecodedInput{
			PreviousTransactionHash:  fmt.Sprintf("%02x", reverse(txIn.PreviousOutPoint.Hash[:])),
			PreviousTransactionIndex: int32(txIn.PreviousOutPoint.Index),
			AmountIn:                 txIn.ValueIn,
		}
	}
	return inputs
}

func decodeTxOutputs(mtx *wire.MsgTx, chainParams *chaincfg.Params) []DecodedOutput {
	outputs := make([]DecodedOutput, len(mtx.TxOut))
	txType := stake.DetermineTxType(mtx)
	for i, v := range mtx.TxOut {

		var addrs []dcrutil.Address
		var encodedAddrs []string
		var scriptClass txscript.ScriptClass
		if (txType == stake.TxTypeSStx) && (stake.IsStakeSubmissionTxOut(i)) {
			scriptClass = txscript.StakeSubmissionTy
			addr, err := stake.AddrFromSStxPkScrCommitment(v.PkScript,
				chainParams)
			if err != nil {
				encodedAddrs = []string{fmt.Sprintf(
					"[error] failed to decode ticket "+
						"commitment addr output for tx hash "+
						"%v, output idx %v", mtx.TxHash(), i)}
			} else {
				encodedAddrs = []string{addr.EncodeAddress()}
			}
		} else {
			// Ignore the error here since an error means the script
			// couldn't parse and there is no additional information
			// about it anyways.
			scriptClass, addrs, _, _ = txscript.ExtractPkScriptAddrs(
				v.Version, v.PkScript, chainParams)
			encodedAddrs = make([]string, len(addrs))
			for j, addr := range addrs {
				encodedAddrs[j] = addr.EncodeAddress()
			}
		}

		outputs[i] = DecodedOutput{
			Index:      int32(i),
			Value:      v.Value,
			Version:    int32(v.Version),
			Addresses:  encodedAddrs,
			ScriptType: scriptClass.String(),
		}
	}

	return outputs
}

func voteVersion(mtx *wire.MsgTx) uint32 {
	if len(mtx.TxOut[1].PkScript) < 8 {
		return 0 // Consensus version absent
	}

	return binary.LittleEndian.Uint32(mtx.TxOut[1].PkScript[4:8])
}

func voteBits(mtx *wire.MsgTx) uint16 {
	return binary.LittleEndian.Uint16(mtx.TxOut[1].PkScript[2:4])
}

func reverse(hash []byte) []byte {
	for i := 0; i < len(hash)/2; i++ {
		j := len(hash) - i - 1
		hash[i], hash[j] = hash[j], hash[i]
	}
	return hash
}

func transactionType(txType wallet.TransactionType) string {
	switch txType {
	case wallet.TransactionTypeCoinbase:
		return "COINBASE"
	case wallet.TransactionTypeTicketPurchase:
		return "TICKET_PURCHASE"
	case wallet.TransactionTypeVote:
		return "VOTE"
	case wallet.TransactionTypeRevocation:
		return "REVOCATION"
	default:
		return "REGULAR"
	}
}

func (lw *LibWallet) SpendableForAccount(account int32, requiredConfirmations int32) (int64, error) {
	bals, err := lw.wallet.CalculateAccountBalance(uint32(account), requiredConfirmations)
	if err != nil {
		log.Error(err)
		return 0, err
	}
	return int64(bals.Spendable), nil
}

type txChangeSource struct {
	version uint16
	script  []byte
}

func (src *txChangeSource) Script() ([]byte, uint16, error) {
	return src.script, src.version, nil
}

func (src *txChangeSource) ScriptSize() int {
	return len(src.script)
}

func makeTxChangeSource(destAddr string) (*txChangeSource, error) {
	addr, err := dcrutil.DecodeAddress(destAddr)
	if err != nil {
		log.Error(err)
		return nil, err
	}
	pkScript, err := txscript.PayToAddrScript(addr)
	if err != nil {
		log.Error(err)
		return nil, err
	}
	changeSource := &txChangeSource{
		script:  pkScript,
		version: txscript.DefaultScriptVersion,
	}
	return changeSource, nil
}

func (lw *LibWallet) ConstructTransaction(destAddr string, amount int64, srcAccount int32, requiredConfirmations int32, sendAll bool) (*UnsignedTransaction, error) {
	// output destination
	addr, err := dcrutil.DecodeAddress(destAddr)
	if err != nil {
		log.Error(err)
		return nil, err
	}
	pkScript, err := txscript.PayToAddrScript(addr)
	if err != nil {
		log.Error(err)
		return nil, err
	}
	version := txscript.DefaultScriptVersion

	// pay output
	outputs := make([]*wire.TxOut, 0)
	var algo wallet.OutputSelectionAlgorithm = wallet.OutputSelectionAlgorithmAll
	var changeSource txauthor.ChangeSource
	if !sendAll {
		algo = wallet.OutputSelectionAlgorithmDefault
		output := &wire.TxOut{
			Value:    amount,
			Version:  version,
			PkScript: pkScript,
		}
		outputs = append(outputs, output)
	} else {
		changeSource, err = makeTxChangeSource(destAddr)
		if err != nil {
			log.Error(err)
			return nil, err
		}
	}
	feePerKb := txrules.DefaultRelayFeePerKb

	// create tx
	tx, err := lw.wallet.NewUnsignedTransaction(outputs, feePerKb, uint32(srcAccount),
		requiredConfirmations, algo, changeSource)
	if err != nil {
		log.Error(err)
		return nil, translateError(err)
	}

	if tx.ChangeIndex >= 0 {
		tx.RandomizeChangePosition()
	}

	var txBuf bytes.Buffer
	txBuf.Grow(tx.Tx.SerializeSize())
	err = tx.Tx.Serialize(&txBuf)
	if err != nil {
		log.Error(err)
		return nil, err
	}

	var totalOutput dcrutil.Amount
	for _, txOut := range outputs {
		totalOutput += dcrutil.Amount(txOut.Value)
	}

	return &UnsignedTransaction{
		UnsignedTransaction:       txBuf.Bytes(),
		TotalOutputAmount:         int64(totalOutput),
		TotalPreviousOutputAmount: int64(tx.TotalInput),
		EstimatedSignedSize:       tx.EstimatedSignedSerializeSize,
		ChangeIndex:               tx.ChangeIndex,
	}, nil
}

func (lw *LibWallet) SendTransaction(privPass []byte, destAddr string, amount int64, srcAccount int32, requiredConfs int32, sendAll bool) ([]byte, error) {
	n, err := lw.wallet.NetworkBackend()
	if err != nil {
		log.Error(err)
		return nil, err
	}
	defer func() {
		for i := range privPass {
			privPass[i] = 0
		}
	}()
	// output destination
	addr, err := dcrutil.DecodeAddress(destAddr)
	if err != nil {
		log.Error(err)
		return nil, err
	}
	pkScript, err := txscript.PayToAddrScript(addr)
	if err != nil {
		log.Error(err)
		return nil, err
	}

	// pay output
	outputs := make([]*wire.TxOut, 0)
	var algo wallet.OutputSelectionAlgorithm = wallet.OutputSelectionAlgorithmAll
	var changeSource txauthor.ChangeSource
	if !sendAll {
		algo = wallet.OutputSelectionAlgorithmDefault
		output := &wire.TxOut{
			Value:    amount,
			Version:  txscript.DefaultScriptVersion,
			PkScript: pkScript,
		}
		outputs = append(outputs, output)
	} else {
		changeSource, err = makeTxChangeSource(destAddr)
		if err != nil {
			log.Error(err)
			return nil, err
		}
	}

	// create tx
	unsignedTx, err := lw.wallet.NewUnsignedTransaction(outputs, txrules.DefaultRelayFeePerKb, uint32(srcAccount),
		requiredConfs, algo, changeSource)
	if err != nil {
		log.Error(err)
		return nil, translateError(err)
	}

	if unsignedTx.ChangeIndex >= 0 {
		unsignedTx.RandomizeChangePosition()
	}

	var txBuf bytes.Buffer
	txBuf.Grow(unsignedTx.Tx.SerializeSize())
	err = unsignedTx.Tx.Serialize(&txBuf)
	if err != nil {
		log.Error(err)
		return nil, err
	}

	var tx wire.MsgTx
	err = tx.Deserialize(bytes.NewReader(txBuf.Bytes()))
	if err != nil {
		log.Error(err)
		//Bytes do not represent a valid raw transaction
		return nil, err
	}

	lock := make(chan time.Time, 1)
	defer func() {
		lock <- time.Time{}
	}()

	err = lw.wallet.Unlock(privPass, lock)
	if err != nil {
		log.Error(err)
		return nil, errors.New(ErrInvalidPassphrase)
	}

	var additionalPkScripts map[wire.OutPoint][]byte

	invalidSigs, err := lw.wallet.SignTransaction(&tx, txscript.SigHashAll, additionalPkScripts, nil, nil)
	if err != nil {
		log.Error(err)
		return nil, err
	}

	invalidInputIndexes := make([]uint32, len(invalidSigs))
	for i, e := range invalidSigs {
		invalidInputIndexes[i] = e.InputIndex
	}

	var serializedTransaction bytes.Buffer
	serializedTransaction.Grow(tx.SerializeSize())
	err = tx.Serialize(&serializedTransaction)
	if err != nil {
		log.Error(err)
		return nil, err
	}

	var msgTx wire.MsgTx
	err = msgTx.Deserialize(bytes.NewReader(serializedTransaction.Bytes()))
	if err != nil {
		//Invalid tx
		log.Error(err)
		return nil, err
	}

	txHash, err := lw.wallet.PublishTransaction(&msgTx, serializedTransaction.Bytes(), n)
	if err != nil {
		return nil, err
	}
	return txHash[:], nil
}

func (lw *LibWallet) PublishUnminedTransactions() error {
	netBackend, err := lw.wallet.NetworkBackend()
	if err != nil {
		return errors.New(ErrNotConnected)
	}
	err = lw.wallet.PublishUnminedTransactions(contextWithShutdownCancel(context.Background()), netBackend)
	return err
}

func (lw *LibWallet) GetAccounts(requiredConfirmations int32) (string, error) {
	resp, err := lw.wallet.Accounts()
	if err != nil {
		return "", err
	}
	accounts := make([]Account, len(resp.Accounts))
	for i := range resp.Accounts {
		a := &resp.Accounts[i]
		bals, err := lw.wallet.CalculateAccountBalance(a.AccountNumber, requiredConfirmations)
		if err != nil {
			return "", err
		}
		balance := Balance{
			Total:                   int64(bals.Total),
			Spendable:               int64(bals.Spendable),
			ImmatureReward:          int64(bals.ImmatureCoinbaseRewards),
			ImmatureStakeGeneration: int64(bals.ImmatureStakeGeneration),
			LockedByTickets:         int64(bals.LockedByTickets),
			VotingAuthority:         int64(bals.VotingAuthority),
			UnConfirmed:             int64(bals.Unconfirmed),
		}
		accounts[i] = Account{
			Number:           int32(a.AccountNumber),
			Name:             a.AccountName,
			TotalBalance:     int64(a.TotalBalance),
			Balance:          &balance,
			ExternalKeyCount: int32(a.LastUsedExternalIndex + 20),
			InternalKeyCount: int32(a.LastUsedInternalIndex + 20),
			ImportedKeyCount: int32(a.ImportedKeyCount),
		}
	}
	accountsResponse := &Accounts{
		Count:              len(resp.Accounts),
		CurrentBlockHash:   resp.CurrentBlockHash[:],
		CurrentBlockHeight: resp.CurrentBlockHeight,
		Acc:                &accounts,
		ErrorOccurred:      false,
	}
	result, _ := json.Marshal(accountsResponse)
	return string(result), nil
}

func (lw *LibWallet) NextAccount(accountName string, privPass []byte) error {
	lock := make(chan time.Time, 1)
	defer func() {
		for i := range privPass {
			privPass[i] = 0
		}
		lock <- time.Time{} // send matters, not the value
	}()
	err := lw.wallet.Unlock(privPass, lock)
	if err != nil {
		log.Error(err)
		return errors.New(ErrInvalidPassphrase)
	}

	_, err = lw.wallet.NextAccount(accountName)
	if err != nil {
		log.Error(err)
		return err
	}
	return nil
}

func (lw *LibWallet) RenameAccount(accountNumber int32, newName string) error {
	err := lw.wallet.RenameAccount(uint32(accountNumber), newName)
	return err
}

func (lw *LibWallet) HaveAddress(address string) bool {
	addr, err := decodeAddress(address, lw.wallet.ChainParams())
	if err != nil {
		log.Error(err)
		return false
	}
	have, err := lw.wallet.HaveAddress(addr)
	if err != nil {
		return false
	}

	return have
}

func (lw *LibWallet) IsAddressValid(address string) bool {
	_, err := decodeAddress(address, lw.wallet.ChainParams())
	if err != nil {
		log.Error(err)
		return false
	}
	return true
}

func (lw *LibWallet) AccountName(account int32) string {
	name, err := lw.wallet.AccountName(uint32(account))
	if err != nil {
		log.Error(err)
		return "Account not found"
	}
	return name
}

func (lw *LibWallet) AccountOfAddress(address string) string {
	addr, err := dcrutil.DecodeAddress(address)
	if err != nil {
		log.Error(err)
		return "Address decode error"
	}
	info, _ := lw.wallet.AddressInfo(addr)
	return lw.AccountName(int32(info.Account()))
}

func (lw *LibWallet) CurrentAddress(account int32) (string, error) {
	addr, err := lw.wallet.CurrentAddress(uint32(account))
	if err != nil {
		log.Error(err)
		return "", err
	}
	return addr.EncodeAddress(), nil
}

func (lw *LibWallet) NextAddress(account int32) (string, error) {
	var callOpts []wallet.NextAddressCallOption
	callOpts = append(callOpts, wallet.WithGapPolicyWrap())

	addr, err := lw.wallet.NewExternalAddress(uint32(account), callOpts...)
	if err != nil {
		log.Error(err)
		return "", err
	}
	return addr.EncodeAddress(), nil
}

func (lw *LibWallet) SignMessage(passphrase []byte, address string, message string) ([]byte, error) {
	lock := make(chan time.Time, 1)
	defer func() {
		lock <- time.Time{}
	}()
	err := lw.wallet.Unlock(passphrase, lock)
	if err != nil {
		return nil, translateError(err)
	}

	addr, err := decodeAddress(address, lw.wallet.ChainParams())
	if err != nil {
		return nil, translateError(err)
	}

	var sig []byte
	switch a := addr.(type) {
	case *dcrutil.AddressSecpPubKey:
	case *dcrutil.AddressPubKeyHash:
		if a.DSA(a.Net()) != dcrec.STEcdsaSecp256k1 {
			return nil, errors.New(ErrInvalidAddress)
		}
	default:
		return nil, errors.New(ErrInvalidAddress)
	}

	sig, err = lw.wallet.SignMessage(message, addr)
	if err != nil {
		return nil, translateError(err)
	}

	return sig, nil
}

func (lw *LibWallet) VerifyMessage(address string, message string, signatureBase64 string) (bool, error) {
	var valid bool

	addr, err := dcrutil.DecodeAddress(address)
	if err != nil {
		return false, translateError(err)
	}

	signature, err := DecodeBase64(signatureBase64)
	if err != nil {
		return false, err
	}

	// Addresses must have an associated secp256k1 private key and therefore
	// must be P2PK or P2PKH (P2SH is not allowed).
	switch a := addr.(type) {
	case *dcrutil.AddressSecpPubKey:
	case *dcrutil.AddressPubKeyHash:
		if a.DSA(a.Net()) != dcrec.STEcdsaSecp256k1 {
			return false, errors.New(ErrInvalidAddress)
		}
	default:
		return false, errors.New(ErrInvalidAddress)
	}

	valid, err = wallet.VerifyMessage(message, addr, signature)
	if err != nil {
		return false, translateError(err)
	}

	return valid, nil
}

func (lw *LibWallet) CallJSONRPC(method string, args string, address string, username string, password string, caCert string) (string, error) {
	arguments := strings.Split(args, ",")
	params := make([]interface{}, 0)
	for _, arg := range arguments {
		if strings.TrimSpace(arg) == "" {
			continue
		}
		params = append(params, strings.TrimSpace(arg))
	}
	// Attempt to create the appropriate command using the arguments
	// provided by the user.
	cmd, err := dcrjson.NewCmd(method, params...)
	if err != nil {
		// Show the error along with its error code when it's a
		// dcrjson.Error as it reallistcally will always be since the
		// NewCmd function is only supposed to return errors of that
		// type.
		if jerr, ok := err.(dcrjson.Error); ok {
			log.Errorf("%s command: %v (code: %s)\n",
				method, err, jerr.Code)
			return "", err
		}
		// The error is not a dcrjson.Error and this really should not
		// happen.  Nevertheless, fallback to just showing the error
		// if it should happen due to a bug in the package.
		log.Errorf("%s command: %v\n", method, err)
		return "", err
	}

	// Marshal the command into a JSON-RPC byte slice in preparation for
	// sending it to the RPC server.
	marshalledJSON, err := dcrjson.MarshalCmd("1.0", 1, cmd)
	if err != nil {
		log.Error(err)
		return "", err
	}

	// Send the JSON-RPC request to the server using the user-specified
	// connection configuration.
	result, err := sendPostRequest(marshalledJSON, address, username, password, caCert)
	if err != nil {
		log.Error(err)
		return "", err
	}

	// Choose how to display the result based on its type.
	strResult := string(result)
	if strings.HasPrefix(strResult, "{") || strings.HasPrefix(strResult, "[") {
		var dst bytes.Buffer
		if err := json.Indent(&dst, result, "", "  "); err != nil {
			log.Errorf("Failed to format result: %v", err)
			return "", err
		}
		fmt.Println(dst.String())
		return dst.String(), nil

	} else if strings.HasPrefix(strResult, `"`) {
		var str string
		if err := json.Unmarshal(result, &str); err != nil {
			log.Errorf("Failed to unmarshal result: %v", err)
			return "", err
		}
		fmt.Println(str)
		return str, nil

	} else if strResult != "null" {
		fmt.Println(strResult)
		return strResult, nil
	}
	return "", nil
}

func AmountCoin(amount int64) float64 {
	return dcrutil.Amount(amount).ToCoin()
}

func AmountAtom(f float64) int64 {
	amount, err := dcrutil.NewAmount(f)
	if err != nil {
		log.Error(err)
		return -1
	}
	return int64(amount)
}

func translateError(err error) error {
	if err, ok := err.(*errors.Error); ok {
		switch err.Kind {
		case errors.InsufficientBalance:
			return errors.New(ErrInsufficientBalance)
		case errors.NotExist:
			return errors.New(ErrNotExist)
		case errors.Passphrase:
			return errors.New(ErrInvalidPassphrase)
		}
	}
	return err
}

func EncodeHex(hexBytes []byte) string {
	return hex.EncodeToString(hexBytes)
}

func EncodeBase64(text []byte) string {
	return base64.StdEncoding.EncodeToString(text)
}

func DecodeBase64(base64Text string) ([]byte, error) {
	b, err := base64.StdEncoding.DecodeString(base64Text)
	if err != nil {
		return nil, err
	}

	return b, nil
}
