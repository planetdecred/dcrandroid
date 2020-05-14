package dcrlibwallet

import (
	"context"
	"encoding/json"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/asdine/storm"
	"github.com/asdine/storm/q"
	"github.com/decred/dcrd/chaincfg/v2"
	"github.com/decred/dcrwallet/errors/v2"
	w "github.com/decred/dcrwallet/wallet/v3"
	"github.com/raedahgroup/dcrlibwallet/txindex"
	"github.com/raedahgroup/dcrlibwallet/utils"
	bolt "go.etcd.io/bbolt"

	"golang.org/x/crypto/bcrypt"
)

type MultiWallet struct {
	dbDriver string
	rootDir  string
	db       *storm.DB

	chainParams *chaincfg.Params
	wallets     map[int]*Wallet
	syncData    *syncData

	notificationListenersMu         sync.RWMutex
	txAndBlockNotificationListeners map[string]TxAndBlockNotificationListener
	blocksRescanProgressListener    BlocksRescanProgressListener

	shuttingDown chan bool
	cancelFuncs  []context.CancelFunc
}

func NewMultiWallet(rootDir, dbDriver, netType string) (*MultiWallet, error) {
	errors.Separator = ":: "

	chainParams, err := utils.ChainParams(netType)
	if err != nil {
		return nil, err
	}

	rootDir = filepath.Join(rootDir, netType)
	err = os.MkdirAll(rootDir, os.ModePerm)
	if err != nil {
		return nil, errors.Errorf("failed to create rootDir: %v", err)
	}

	err = initLogRotator(filepath.Join(rootDir, logFileName))
	if err != nil {
		return nil, errors.Errorf("failed to init logRotator: %v", err.Error())
	}

	walletsDb, err := storm.Open(filepath.Join(rootDir, walletsDbName))
	if err != nil {
		log.Errorf("Error opening wallets database: %s", err.Error())
		if err == bolt.ErrTimeout {
			// timeout error occurs if storm fails to acquire a lock on the database file
			return nil, errors.E(ErrWalletDatabaseInUse)
		}
		return nil, errors.Errorf("error opening wallets database: %s", err.Error())
	}

	// init database for saving/reading wallet objects
	err = walletsDb.Init(&Wallet{})
	if err != nil {
		log.Errorf("Error initializing wallets database: %s", err.Error())
		return nil, err
	}

	mw := &MultiWallet{
		dbDriver:    dbDriver,
		rootDir:     rootDir,
		db:          walletsDb,
		chainParams: chainParams,
		wallets:     make(map[int]*Wallet),
		syncData: &syncData{
			syncCanceled:          make(chan bool),
			syncProgressListeners: make(map[string]SyncProgressListener),
		},
		txAndBlockNotificationListeners: make(map[string]TxAndBlockNotificationListener),
	}

	// read saved wallets info from db and initialize wallets
	query := mw.db.Select(q.True()).OrderBy("ID")
	var wallets []*Wallet
	err = query.Find(&wallets)
	if err != nil && err != storm.ErrNotFound {
		return nil, err
	}

	// prepare the wallets loaded from db for use
	for _, wallet := range wallets {
		err = wallet.prepare(rootDir, chainParams, mw.walletConfigSetFn(wallet.ID), mw.walletConfigReadFn(wallet.ID))
		if err != nil {
			return nil, err
		}
		mw.wallets[wallet.ID] = wallet
	}

	mw.listenForShutdown()

	logLevel := mw.ReadStringConfigValueForKey(LogLevelConfigKey)
	SetLogLevels(logLevel)

	log.Infof("Loaded %d wallets", mw.LoadedWalletsCount())

	return mw, nil
}

func (mw *MultiWallet) Shutdown() {
	log.Info("Shutting down dcrlibwallet")

	// Trigger shuttingDown signal to cancel all contexts created with `shutdownContextWithCancel`.
	mw.shuttingDown <- true

	mw.CancelRescan()
	mw.CancelSync()

	for _, wallet := range mw.wallets {
		wallet.Shutdown()
	}

	if mw.db != nil {
		if err := mw.db.Close(); err != nil {
			log.Errorf("db closed with error: %v", err)
		} else {
			log.Info("db closed successfully")
		}
	}

	if logRotator != nil {
		log.Info("Shutting down log rotator")
		logRotator.Close()
	}
}

func (mw *MultiWallet) SetStartupPassphrase(passphrase []byte, passphraseType int32) error {
	return mw.ChangeStartupPassphrase([]byte(""), passphrase, passphraseType)
}

func (mw *MultiWallet) VerifyStartupPassphrase(startupPassphrase []byte) error {
	var startupPassphraseHash []byte
	err := mw.db.Get(walletsMetadataBucketName, walletstartupPassphraseField, &startupPassphraseHash)
	if err != nil && err != storm.ErrNotFound {
		return err
	}

	if startupPassphraseHash == nil {
		// startup passphrase was not previously set
		if len(startupPassphrase) > 0 {
			return errors.E(ErrInvalidPassphrase)
		}
		return nil
	}

	// startup passphrase was set, verify
	err = bcrypt.CompareHashAndPassword(startupPassphraseHash, startupPassphrase)
	if err != nil {
		return errors.E(ErrInvalidPassphrase)
	}

	return nil
}

func (mw *MultiWallet) ChangeStartupPassphrase(oldPassphrase, newPassphrase []byte, passphraseType int32) error {
	if len(newPassphrase) == 0 {
		return mw.RemoveStartupPassphrase(oldPassphrase)
	}

	err := mw.VerifyStartupPassphrase(oldPassphrase)
	if err != nil {
		return err
	}

	startupPassphraseHash, err := bcrypt.GenerateFromPassword(newPassphrase, bcrypt.DefaultCost)
	if err != nil {
		return err
	}

	err = mw.db.Set(walletsMetadataBucketName, walletstartupPassphraseField, startupPassphraseHash)
	if err != nil {
		return err
	}

	mw.SaveUserConfigValue(IsStartupSecuritySetConfigKey, true)
	mw.SaveUserConfigValue(StartupSecurityTypeConfigKey, passphraseType)

	return nil
}

func (mw *MultiWallet) RemoveStartupPassphrase(oldPassphrase []byte) error {
	err := mw.VerifyStartupPassphrase(oldPassphrase)
	if err != nil {
		return err
	}

	err = mw.db.Delete(walletsMetadataBucketName, walletstartupPassphraseField)
	if err != nil {
		return err
	}

	mw.SaveUserConfigValue(IsStartupSecuritySetConfigKey, false)
	mw.DeleteUserConfigValueForKey(StartupSecurityTypeConfigKey)

	return nil
}

func (mw *MultiWallet) IsStartupSecuritySet() bool {
	return mw.ReadBoolConfigValueForKey(IsStartupSecuritySetConfigKey, false)
}

func (mw *MultiWallet) StartupSecurityType() int32 {
	return mw.ReadInt32ConfigValueForKey(StartupSecurityTypeConfigKey, PassphraseTypePass)
}

func (mw *MultiWallet) OpenWallets(startupPassphrase []byte) error {
	if mw.IsSyncing() {
		return errors.New(ErrSyncAlreadyInProgress)
	}

	err := mw.VerifyStartupPassphrase(startupPassphrase)
	if err != nil {
		return err
	}

	for _, wallet := range mw.wallets {
		err = wallet.openWallet()
		if err != nil {
			return err
		}
	}

	return nil
}

func (mw *MultiWallet) CreateWatchOnlyWallet(walletName, extendedPublicKey string) (*Wallet, error) {
	wallet := &Wallet{
		Name:                  walletName,
		IsRestored:            true,
		HasDiscoveredAccounts: true,
	}

	return mw.saveNewWallet(wallet, func() error {
		err := wallet.prepare(mw.rootDir, mw.chainParams, mw.walletConfigSetFn(wallet.ID), mw.walletConfigReadFn(wallet.ID))
		if err != nil {
			return err
		}

		return wallet.createWatchingOnlyWallet(extendedPublicKey)
	})
}

func (mw *MultiWallet) CreateNewWallet(walletName, privatePassphrase string, privatePassphraseType int32) (*Wallet, error) {
	seed, err := GenerateSeed()
	if err != nil {
		return nil, err
	}

	encryptedSeed, err := encryptWalletSeed([]byte(privatePassphrase), seed)
	if err != nil {
		return nil, err
	}
	wallet := &Wallet{
		Name:                  walletName,
		CreatedAt:             time.Now(),
		EncryptedSeed:         encryptedSeed,
		PrivatePassphraseType: privatePassphraseType,
		HasDiscoveredAccounts: true,
	}

	return mw.saveNewWallet(wallet, func() error {
		err := wallet.prepare(mw.rootDir, mw.chainParams, mw.walletConfigSetFn(wallet.ID), mw.walletConfigReadFn(wallet.ID))
		if err != nil {
			return err
		}

		return wallet.createWallet(privatePassphrase, seed)
	})
}

func (mw *MultiWallet) RestoreWallet(walletName, seedMnemonic, privatePassphrase string, privatePassphraseType int32) (*Wallet, error) {

	encryptedSeed, err := encryptWalletSeed([]byte(privatePassphrase), seedMnemonic)
	if err != nil {
		return nil, err
	}

	wallet := &Wallet{
		Name:                  walletName,
		PrivatePassphraseType: privatePassphraseType,
		IsRestored:            true,
		HasDiscoveredAccounts: false,
		EncryptedSeed:         encryptedSeed,
	}

	return mw.saveNewWallet(wallet, func() error {
		err := wallet.prepare(mw.rootDir, mw.chainParams, mw.walletConfigSetFn(wallet.ID), mw.walletConfigReadFn(wallet.ID))
		if err != nil {
			return err
		}

		return wallet.createWallet(privatePassphrase, seedMnemonic)
	})
}

func (mw *MultiWallet) LinkExistingWallet(walletName, walletDataDir, originalPubPass string, privatePassphraseType int32) (*Wallet, error) {
	// check if `walletDataDir` contains wallet.db
	if !WalletExistsAt(walletDataDir) {
		return nil, errors.New(ErrNotExist)
	}

	ctx, _ := mw.contextWithShutdownCancel()

	// verify the public passphrase for the wallet being linked before proceeding
	if err := mw.loadWalletTemporarily(ctx, walletDataDir, originalPubPass, nil); err != nil {
		return nil, err
	}

	wallet := &Wallet{
		Name:                  walletName,
		PrivatePassphraseType: privatePassphraseType,
		IsRestored:            true,
		HasDiscoveredAccounts: false, // assume that account discovery hasn't been done
	}

	return mw.saveNewWallet(wallet, func() error {
		// move wallet.db and tx.db files to newly created dir for the wallet
		currentWalletDbFilePath := filepath.Join(walletDataDir, walletDbName)
		newWalletDbFilePath := filepath.Join(wallet.dataDir, walletDbName)
		if err := moveFile(currentWalletDbFilePath, newWalletDbFilePath); err != nil {
			return err
		}

		currentTxDbFilePath := filepath.Join(walletDataDir, txindex.DbName)
		newTxDbFilePath := filepath.Join(wallet.dataDir, txindex.DbName)
		if err := moveFile(currentTxDbFilePath, newTxDbFilePath); err != nil {
			return err
		}

		// prepare the wallet for use and open it
		err := (func() error {
			err := wallet.prepare(mw.rootDir, mw.chainParams, mw.walletConfigSetFn(wallet.ID), mw.walletConfigReadFn(wallet.ID))
			if err != nil {
				return err
			}

			if originalPubPass == "" || originalPubPass == w.InsecurePubPassphrase {
				return wallet.openWallet()
			}

			err = mw.loadWalletTemporarily(ctx, wallet.dataDir, originalPubPass, func(tempWallet *w.Wallet) error {
				return tempWallet.ChangePublicPassphrase(ctx, []byte(originalPubPass), []byte(w.InsecurePubPassphrase))
			})
			if err != nil {
				return err
			}

			return wallet.openWallet()
		})()

		// restore db files to their original location if there was an error
		// in the wallet setup process above
		if err != nil {
			moveFile(newWalletDbFilePath, currentWalletDbFilePath)
			moveFile(newTxDbFilePath, currentTxDbFilePath)
		}

		return err
	})
}

// saveNewWallet performs the following tasks using a db batch operation to ensure
// that db changes are rolled back if any of the steps below return an error.
//
// - saves the initial wallet info to mw.walletsDb to get a wallet id
// - creates a data directory for the wallet using the auto-generated wallet id
// - updates the initial wallet info with name, dataDir (created above), db driver
//   and saves the updated info to mw.walletsDb
// - calls the provided `setupWallet` function to perform any necessary creation,
//   restoration or linking of the just saved wallet
//
// IFF all the above operations succeed, the wallet info will be persisted to db
// and the wallet will be added to `mw.wallets`.
func (mw *MultiWallet) saveNewWallet(wallet *Wallet, setupWallet func() error) (*Wallet, error) {
	exists, err := mw.WalletNameExists(wallet.Name)
	if err != nil {
		return nil, err
	} else if exists {
		return nil, errors.New(ErrExist)
	}

	if mw.IsConnectedToDecredNetwork() {
		mw.CancelSync()
		defer mw.SpvSync()
	}
	// Perform database save operations in batch transaction
	// for automatic rollback if error occurs at any point.
	err = mw.batchDbTransaction(func(db storm.Node) error {
		// saving struct to update ID property with an auto-generated value
		err := db.Save(wallet)
		if err != nil {
			return err
		}

		walletDataDir := filepath.Join(mw.rootDir, strconv.Itoa(wallet.ID))
		os.MkdirAll(walletDataDir, os.ModePerm) // create wallet dir

		if wallet.Name == "" {
			wallet.Name = "wallet-" + strconv.Itoa(wallet.ID) // wallet-#
		}
		wallet.dataDir = walletDataDir
		wallet.DbDriver = mw.dbDriver

		err = db.Save(wallet) // update database with complete wallet information
		if err != nil {
			return err
		}

		return setupWallet()
	})

	if err != nil {
		return nil, translateError(err)
	}

	mw.wallets[wallet.ID] = wallet

	return wallet, nil
}

func (mw *MultiWallet) RenameWallet(walletID int, newName string) error {
	if strings.HasPrefix(newName, "wallet-") {
		return errors.E(ErrReservedWalletName)
	}

	if exists, err := mw.WalletNameExists(newName); err != nil {
		return translateError(err)
	} else if exists {
		return errors.New(ErrExist)
	}

	wallet := mw.WalletWithID(walletID)
	if wallet == nil {
		return errors.New(ErrInvalid)
	}

	wallet.Name = newName
	return mw.db.Save(wallet) // update WalletName field
}

func (mw *MultiWallet) DeleteWallet(walletID int, privPass []byte) error {

	wallet := mw.WalletWithID(walletID)
	if wallet == nil {
		return errors.New(ErrNotExist)
	}

	if mw.IsConnectedToDecredNetwork() {
		mw.CancelSync()
		defer func() {
			if mw.OpenedWalletsCount() > 0 {
				mw.SpvSync()
			}
		}()
	}

	err := wallet.deleteWallet(privPass)
	if err != nil {
		return translateError(err)
	}

	err = mw.db.DeleteStruct(wallet)
	if err != nil {
		return translateError(err)
	}

	delete(mw.wallets, walletID)

	return nil
}

func (mw *MultiWallet) WalletWithID(walletID int) *Wallet {
	if wallet, ok := mw.wallets[walletID]; ok {
		return wallet
	}
	return nil
}

// VerifySeedForWallet compares seedMnemonic with the decrypted wallet.EncryptedSeed and clears wallet.EncryptedSeed if they match.
func (mw *MultiWallet) VerifySeedForWallet(walletID int, seedMnemonic string, privpass []byte) (bool, error) {
	wallet := mw.WalletWithID(walletID)
	if wallet == nil {
		return false, errors.New(ErrNotExist)
	}

	decryptedSeed, err := decryptWalletSeed(privpass, wallet.EncryptedSeed)
	if err != nil {
		return false, err
	}

	if decryptedSeed == seedMnemonic {
		wallet.EncryptedSeed = nil
		return true, translateError(mw.db.Save(wallet))
	}

	return false, errors.New(ErrInvalid)
}

// NumWalletsNeedingSeedBackup returns the number of opened wallets whose seed haven't been verified.
func (mw *MultiWallet) NumWalletsNeedingSeedBackup() int32 {
	var backupsNeeded int32
	for _, wallet := range mw.wallets {
		if wallet.WalletOpened() && wallet.EncryptedSeed != nil {
			backupsNeeded++
		}
	}
	return backupsNeeded
}

func (mw *MultiWallet) LoadedWalletsCount() int32 {
	return int32(len(mw.wallets))
}

func (mw *MultiWallet) OpenedWalletIDsRaw() []int {
	walletIDs := make([]int, 0)
	for _, wallet := range mw.wallets {
		if wallet.WalletOpened() {
			walletIDs = append(walletIDs, wallet.ID)
		}
	}
	return walletIDs
}

func (mw *MultiWallet) OpenedWalletIDs() string {
	walletIDs := mw.OpenedWalletIDsRaw()
	jsonEncoded, _ := json.Marshal(&walletIDs)
	return string(jsonEncoded)
}

func (mw *MultiWallet) OpenedWalletsCount() int32 {
	return int32(len(mw.OpenedWalletIDsRaw()))
}

func (mw *MultiWallet) SyncedWalletsCount() int32 {
	var syncedWallets int32
	for _, wallet := range mw.wallets {
		if wallet.WalletOpened() && wallet.synced {
			syncedWallets++
		}
	}

	return syncedWallets
}

func (mw *MultiWallet) WalletNameExists(walletName string) (bool, error) {
	if strings.HasPrefix(walletName, "wallet-") {
		return false, errors.E(ErrReservedWalletName)
	}

	err := mw.db.One("Name", walletName, &Wallet{})
	if err == nil {
		return true, nil
	} else if err != storm.ErrNotFound {
		return false, err
	}

	return false, nil
}

func (mw *MultiWallet) UnlockWallet(walletID int, privPass []byte) error {
	wallet := mw.WalletWithID(walletID)
	if wallet == nil {
		return errors.New(ErrNotExist)
	}

	return wallet.UnlockWallet(privPass)
}

// ChangePrivatePassphraseForWallet attempts to change the wallet's passphrase and re-encrypts the seed with the new passphrase.
func (mw *MultiWallet) ChangePrivatePassphraseForWallet(walletID int, oldPrivatePassphrase, newPrivatePassphrase []byte, privatePassphraseType int32) error {
	if privatePassphraseType != PassphraseTypePin && privatePassphraseType != PassphraseTypePass {
		return errors.New(ErrInvalid)
	}

	wallet := mw.WalletWithID(walletID)
	if wallet == nil {
		return errors.New(ErrInvalid)
	}

	err := wallet.changePrivatePassphrase(oldPrivatePassphrase, newPrivatePassphrase)
	if err != nil {
		return translateError(err)
	}

	decryptedSeed, err := decryptWalletSeed(newPrivatePassphrase, wallet.EncryptedSeed)
	if err != nil {
		return err
	}

	encrytedSeed, err := encryptWalletSeed(newPrivatePassphrase, decryptedSeed)
	if err != nil {
		return err
	}

	wallet.EncryptedSeed = encrytedSeed

	wallet.PrivatePassphraseType = privatePassphraseType
	return mw.db.Save(wallet)
}
