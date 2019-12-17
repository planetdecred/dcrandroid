package dcrlibwallet

import (
	"context"
	"fmt"
	"os"
	"path/filepath"

	"github.com/decred/dcrd/chaincfg/v2"
	"github.com/decred/dcrwallet/errors/v2"
	w "github.com/decred/dcrwallet/wallet/v3"
	"github.com/decred/dcrwallet/wallet/v3/txrules"
	"github.com/decred/dcrwallet/walletseed"
	"github.com/raedahgroup/dcrlibwallet/internal/loader"
	"github.com/raedahgroup/dcrlibwallet/txindex"
)

type Wallet struct {
	ID                    int    `storm:"id,increment"`
	Name                  string `storm:"unique"`
	DataDir               string
	DbDriver              string
	Seed                  string
	PrivatePassphraseType int32
	HasDiscoveredAccounts bool

	internal    *w.Wallet
	chainParams *chaincfg.Params
	loader      *loader.Loader
	txDB        *txindex.DB

	synced     bool
	syncing    bool
	waiting    bool
	rescanning bool

	shuttingDown chan bool
	cancelFuncs  []context.CancelFunc
}

// prepare gets a wallet ready for use by opening the transactions index database
// and initializing the wallet loader which can be used subsequently to create,
// load and unload the wallet.
func (wallet *Wallet) prepare(chainParams *chaincfg.Params) (err error) {
	wallet.chainParams = chainParams

	// open database for indexing transactions for faster loading
	txDBPath := filepath.Join(wallet.DataDir, txindex.DbName)
	wallet.txDB, err = txindex.Initialize(txDBPath, &Transaction{})
	if err != nil {
		log.Error(err.Error())
		return err
	}

	// init loader
	defaultFeePerKb := txrules.DefaultRelayFeePerKb.ToCoin()
	stakeOptions := &loader.StakeOptions{
		VotingEnabled: false,
		AddressReuse:  false,
		VotingAddress: nil,
		TicketFee:     defaultFeePerKb,
	}
	wallet.loader = loader.NewLoader(wallet.chainParams, wallet.DataDir, stakeOptions, 20, false,
		defaultFeePerKb, w.DefaultAccountGapLimit, false)
	if wallet.DbDriver != "" {
		wallet.loader.SetDatabaseDriver(wallet.DbDriver)
	}

	// init cancelFuncs slice to hold cancel functions for long running
	// operations and start go routine to listen for shutdown signal
	wallet.cancelFuncs = make([]context.CancelFunc, 0)
	wallet.shuttingDown = make(chan bool)
	go func() {
		<-wallet.shuttingDown
		for _, cancel := range wallet.cancelFuncs {
			cancel()
		}
	}()

	return nil
}

func (wallet *Wallet) Shutdown() {
	// Trigger shuttingDown signal to cancel all contexts created with
	// `wallet.shutdownContext()` or `wallet.shutdownContextWithCancel()`.
	wallet.shuttingDown <- true

	if _, loaded := wallet.loader.LoadedWallet(); loaded {
		err := wallet.loader.UnloadWallet()
		if err != nil {
			log.Errorf("Failed to close wallet: %v", err)
		} else {
			log.Info("Closed wallet")
		}
	}

	if wallet.txDB != nil {
		err := wallet.txDB.Close()
		if err != nil {
			log.Errorf("tx db closed with error: %v", err)
		} else {
			log.Info("tx db closed successfully")
		}
	}
}

func (wallet *Wallet) NetType() string {
	return wallet.chainParams.Name
}

func (wallet *Wallet) WalletExists() (bool, error) {
	return wallet.loader.WalletExists()
}

func (wallet *Wallet) CreateWallet(publicPassphrase, privatePassphrase, seedMnemonic string) error {
	log.Info("Creating Wallet")
	if len(seedMnemonic) == 0 {
		return errors.New(ErrEmptySeed)
	}

	pubPass := []byte(publicPassphrase)
	privPass := []byte(privatePassphrase)
	seed, err := walletseed.DecodeUserInput(seedMnemonic)
	if err != nil {
		log.Error(err)
		return err
	}

	createdWallet, err := wallet.loader.CreateNewWallet(wallet.shutdownContext(), pubPass, privPass, seed)
	if err != nil {
		log.Error(err)
		return err
	}

	wallet.internal = createdWallet

	log.Info("Created Wallet")
	return nil
}

func (wallet *Wallet) CreateWatchingOnlyWallet(publicPassphrase, extendedPublicKey string) error {
	pubPass := []byte(publicPassphrase)

	createdWallet, err := wallet.loader.CreateWatchingOnlyWallet(wallet.shutdownContext(), extendedPublicKey, pubPass)
	if err != nil {
		log.Error(err)
		return err
	}

	wallet.internal = createdWallet

	log.Info("Created Watching Only Wallet")
	return nil
}

func (wallet *Wallet) IsWatchingOnlyWallet() bool {
	if w, ok := wallet.loader.LoadedWallet(); ok {
		return w.Manager.WatchingOnly()
	}

	return false
}

func (wallet *Wallet) OpenWallet(pubPass []byte) error {
	if pubPass == nil {
		pubPass = []byte("public")
	}

	openedWallet, err := wallet.loader.OpenExistingWallet(wallet.shutdownContext(), pubPass)
	if err != nil {
		log.Error(err)
		return translateError(err)
	}

	wallet.internal = openedWallet

	return nil
}

func (wallet *Wallet) WalletOpened() bool {
	return wallet.internal != nil
}

func (wallet *Wallet) UnlockWallet(privPass []byte) error {
	loadedWallet, ok := wallet.loader.LoadedWallet()
	if !ok {
		return fmt.Errorf("wallet has not been loaded")
	}

	defer func() {
		for i := range privPass {
			privPass[i] = 0
		}
	}()

	ctx, _ := wallet.shutdownContextWithCancel()
	err := loadedWallet.Unlock(ctx, privPass, nil)
	if err != nil {
		return translateError(err)
	}

	return nil
}

func (wallet *Wallet) LockWallet() {
	if !wallet.internal.Locked() {
		wallet.internal.Lock()
	}
}

func (wallet *Wallet) IsLocked() bool {
	return wallet.internal.Locked()
}

func (wallet *Wallet) changePrivatePassphrase(oldPass []byte, newPass []byte) error {
	defer func() {
		for i := range oldPass {
			oldPass[i] = 0
		}

		for i := range newPass {
			newPass[i] = 0
		}
	}()

	err := wallet.internal.ChangePrivatePassphrase(wallet.shutdownContext(), oldPass, newPass)
	if err != nil {
		return translateError(err)
	}
	return nil
}

func (wallet *Wallet) CloseWallet() error {
	err := wallet.loader.UnloadWallet()
	wallet.internal = nil
	return err
}

func (wallet *Wallet) deleteWallet(privatePassphrase []byte) error {
	defer func() {
		for i := range privatePassphrase {
			privatePassphrase[i] = 0
		}
	}()

	if _, loaded := wallet.loader.LoadedWallet(); !loaded {
		return errors.New(ErrWalletNotLoaded)
	}

	if !wallet.IsWatchingOnlyWallet() {
		err := wallet.internal.Unlock(wallet.shutdownContext(), privatePassphrase, nil)
		if err != nil {
			return translateError(err)
		}
		wallet.internal.Lock()
	}

	wallet.Shutdown()

	log.Info("Deleting Wallet")
	return os.RemoveAll(wallet.DataDir)
}
