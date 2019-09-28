package dcrlibwallet

import (
	"context"
	"fmt"
	"os"
	"path/filepath"

	"github.com/asdine/storm"
	"github.com/decred/dcrwallet/errors"
	"github.com/decred/dcrwallet/netparams"
	"github.com/decred/dcrwallet/wallet"
	"github.com/decred/dcrwallet/wallet/txrules"
	"github.com/raedahgroup/dcrlibwallet/txindex"
	"github.com/raedahgroup/dcrlibwallet/utils"
	"go.etcd.io/bbolt"
)

const logFileName = "dcrlibwallet.log"

type LibWallet struct {
	walletDataDir string
	activeNet     *netparams.Params
	walletLoader  *WalletLoader
	wallet        *wallet.Wallet
	txDB          *txindex.DB
	configDB      *storm.DB
	*syncData

	shuttingDown chan bool
	cancelFuncs  []context.CancelFunc
}

func NewLibWallet(appDataDir, walletDbDriver string, netType string) (*LibWallet, error) {
	// initialize logger before returning any error
	errors.Separator = ":: "
	initLogRotator(filepath.Join(appDataDir, logFileName))

	activeNet := utils.NetParams(netType)
	if activeNet == nil {
		return nil, fmt.Errorf("unsupported network type: %s", netType)
	}

	lw := &LibWallet{
		activeNet:     activeNet,
		walletDataDir: filepath.Join(appDataDir, activeNet.Name),
	}

	err := os.MkdirAll(lw.walletDataDir, 0700)
	if err != nil {
		return nil, fmt.Errorf("failed to create wallet db directory: %v", err)
	}

	configDbPath := filepath.Join(lw.walletDataDir, userConfigDbFilename)
	lw.configDB, err = storm.Open(configDbPath)
	if err != nil {
		if err == bolt.ErrTimeout {
			// timeout error occurs if storm fails to acquire a lock on the database file
			return nil, fmt.Errorf("settings db is in use by another process")
		}
		return nil, fmt.Errorf("error opening settings db store: %s", err.Error())
	}

	logLevel := lw.ReadStringConfigValueForKey(LogLevelConfigKey)
	SetLogLevels(logLevel)

	// open database for indexing transactions for faster loading
	txDBPath := filepath.Join(lw.walletDataDir, txindex.DbName)
	lw.txDB, err = txindex.Initialize(txDBPath, &Transaction{})
	if err != nil {
		log.Error(err.Error())
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

	lw.walletLoader = NewLoader(activeNet.Params, lw.walletDataDir, stakeOptions, 20, false,
		defaultFees, wallet.DefaultAccountGapLimit)
	if walletDbDriver != "" {
		lw.walletLoader.SetDatabaseDriver(walletDbDriver)
	}

	lw.syncData = &syncData{
		syncCanceled:          make(chan bool),
		syncProgressListeners: make(map[string]SyncProgressListener),
	}

	// todo add interrupt listener
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
