package dcrlibwallet

import (
	"context"
	"fmt"
	"path/filepath"

	"github.com/asdine/storm"
	"github.com/decred/dcrwallet/errors"
	"github.com/decred/dcrwallet/netparams"
	"github.com/decred/dcrwallet/wallet"
	"github.com/decred/dcrwallet/wallet/txrules"
	"github.com/raedahgroup/dcrlibwallet/utils"
	"go.etcd.io/bbolt"
)

const (
	logFileName = "dcrlibwallet.log"
	txDbName    = "tx.db"

	BlockValid = 1 << 0
)

type LibWallet struct {
	walletDataDir string
	activeNet     *netparams.Params
	walletLoader  *WalletLoader
	wallet        *wallet.Wallet
	txDB          *storm.DB
	*syncData

	shuttingDown chan bool
	cancelFuncs  []context.CancelFunc
}

func NewLibWallet(homeDir string, dbDriver string, netType string) (*LibWallet, error) {
	activeNet := utils.NetParams(netType)
	if activeNet == nil {
		return nil, fmt.Errorf("unsupported network type: %s", netType)
	}

	walletDataDir := filepath.Join(homeDir, activeNet.Name)
	return newLibWallet(walletDataDir, dbDriver, activeNet)
}

func NewLibWalletWithDbPath(walletDataDir string, activeNet *netparams.Params) (*LibWallet, error) {
	return newLibWallet(walletDataDir, "", activeNet)
}

func newLibWallet(walletDataDir, walletDbDriver string, activeNet *netparams.Params) (*LibWallet, error) {
	errors.Separator = ":: "
	initLogRotator(filepath.Join(walletDataDir, logFileName))

	// open database for indexing transactions for faster loading
	txDB, err := storm.Open(filepath.Join(walletDataDir, txDbName))
	if err != nil {
		log.Errorf("Error opening tx database for wallet: %s", err.Error())
		if err == bolt.ErrTimeout {
			// timeout error occurs if storm fails to acquire a lock on the database file
			return nil, fmt.Errorf("tx index database is in use by another process")
		}
		return nil, fmt.Errorf("error opening tx index database: %s", err.Error())
	}

	// init database for saving/reading transaction objects
	err = txDB.Init(&Transaction{})
	if err != nil {
		log.Errorf("Error initializing tx database for wallet: %s", err.Error())
		return nil, err
	}

	// init walletLoader
	defaultFees := txrules.DefaultRelayFeePerKb.ToCoin()

	stakeOptions := &StakeOptions{
		VotingEnabled: false,
		AddressReuse:  false,
		VotingAddress: nil,
		TicketFee:     defaultFees,
	}

	walletLoader := NewLoader(activeNet.Params, walletDataDir, stakeOptions, 20, false,
		defaultFees, wallet.DefaultAccountGapLimit)

	if walletDbDriver != "" {
		walletLoader.SetDatabaseDriver(walletDbDriver)
	}

	syncData := &syncData{
		syncCanceled:          make(chan bool),
		syncProgressListeners: make(map[string]SyncProgressListener),
	}

	// Finally Init LibWallet
	lw := &LibWallet{
		walletDataDir: walletDataDir,
		txDB:          txDB,
		activeNet:     activeNet,
		walletLoader:  walletLoader,
		syncData:      syncData,
	}

	lw.listenForShutdown()

	return lw, nil
}

func (lw *LibWallet) Shutdown() {
	log.Info("Shutting down dcrlibwallet")

	// Trigger shuttingDown signal to cancel all contexts created with `contextWithShutdownCancel`.
	lw.shuttingDown <- true

	if lw.rpcClient != nil {
		lw.rpcClient.Stop()
	}

	lw.CancelSync()

	if logRotator != nil {
		log.Info("Shutting down log rotator")
		logRotator.Close()
	}

	if _, loaded := lw.walletLoader.LoadedWallet(); loaded {
		err := lw.walletLoader.UnloadWallet()
		if err != nil {
			log.Errorf("Failed to close wallet: %v", err)
		} else {
			log.Info("Closed wallet")
		}
	}

	if lw.txDB != nil {
		err := lw.txDB.Close()
		if err != nil {
			log.Errorf("tx db closed with error: %v", err)
		} else {
			log.Info("tx db closed successfully")
		}
	}
}
