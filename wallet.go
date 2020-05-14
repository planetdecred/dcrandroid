package dcrlibwallet

import (
	"context"
	"fmt"
	"os"
	"path/filepath"
	"strconv"
	"time"

	"github.com/decred/dcrd/chaincfg/v2"
	"github.com/decred/dcrwallet/errors/v2"
	w "github.com/decred/dcrwallet/wallet/v3"
	"github.com/decred/dcrwallet/walletseed"
	"github.com/raedahgroup/dcrlibwallet/internal/loader"
	"github.com/raedahgroup/dcrlibwallet/txindex"
)

type Wallet struct {
	ID                    int       `storm:"id,increment"`
	Name                  string    `storm:"unique"`
	CreatedAt             time.Time `storm:"index"`
	DbDriver              string
	EncryptedSeed         []byte
	IsRestored            bool
	HasDiscoveredAccounts bool
	PrivatePassphraseType int32

	internal    *w.Wallet
	chainParams *chaincfg.Params
	dataDir     string
	loader      *loader.Loader
	txDB        *txindex.DB

	synced  bool
	syncing bool
	waiting bool

	shuttingDown chan bool
	cancelFuncs  []context.CancelFunc

	// setUserConfigValue saves the provided key-value pair to a config database.
	// This function is ideally assigned when the `wallet.prepare` method is
	// called from a MultiWallet instance.
	setUserConfigValue configSaveFn

	// readUserConfigValue returns the previously saved value for the provided
	// key from a config database. Returns nil if the key wasn't previously set.
	// This function is ideally assigned when the `wallet.prepare` method is
	// called from a MultiWallet instance.
	readUserConfigValue configReadFn
}

// prepare gets a wallet ready for use by opening the transactions index database
// and initializing the wallet loader which can be used subsequently to create,
// load and unload the wallet.
func (wallet *Wallet) prepare(rootDir string, chainParams *chaincfg.Params,
	setUserConfigValueFn configSaveFn, readUserConfigValueFn configReadFn) (err error) {

	wallet.chainParams = chainParams
	wallet.dataDir = filepath.Join(rootDir, strconv.Itoa(wallet.ID))
	wallet.setUserConfigValue = setUserConfigValueFn
	wallet.readUserConfigValue = readUserConfigValueFn

	// open database for indexing transactions for faster loading
	txDBPath := filepath.Join(wallet.dataDir, txindex.DbName)
	wallet.txDB, err = txindex.Initialize(txDBPath, &Transaction{})
	if err != nil {
		log.Error(err.Error())
		return err
	}

	// init loader
	wallet.loader = initWalletLoader(wallet.chainParams, wallet.dataDir, wallet.DbDriver)

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

// WalletCreationTimeInMillis returns the wallet creation time for new
// wallets. Restored wallets would return an error.
func (wallet *Wallet) WalletCreationTimeInMillis() (int64, error) {
	if wallet.IsRestored {
		return 0, errors.New(ErrWalletIsRestored)
	}

	return wallet.CreatedAt.UnixNano() / int64(time.Millisecond), nil
}

func (wallet *Wallet) NetType() string {
	return wallet.chainParams.Name
}

func (wallet *Wallet) WalletExists() (bool, error) {
	return wallet.loader.WalletExists()
}

func (wallet *Wallet) createWallet(privatePassphrase, seedMnemonic string) error {
	log.Info("Creating Wallet")
	if len(seedMnemonic) == 0 {
		return errors.New(ErrEmptySeed)
	}

	pubPass := []byte(w.InsecurePubPassphrase)
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

func (wallet *Wallet) createWatchingOnlyWallet(extendedPublicKey string) error {
	pubPass := []byte(w.InsecurePubPassphrase)

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

func (wallet *Wallet) openWallet() error {
	pubPass := []byte(w.InsecurePubPassphrase)

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
	return os.RemoveAll(wallet.dataDir)
}

// DecryptSeed decrypts wallet.EncryptedSeed using privatePassphrase
func (wallet *Wallet) DecryptSeed(privatePassphrase []byte) (string, error) {
	if wallet.EncryptedSeed == nil {
		return "", errors.New(ErrInvalid)
	}

	return decryptWalletSeed(privatePassphrase, wallet.EncryptedSeed)
}
