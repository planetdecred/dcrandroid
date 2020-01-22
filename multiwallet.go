package dcrlibwallet

import (
	"context"
	"encoding/json"
	"os"
	"path/filepath"
	"strconv"
	"strings"

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

	// read saved wallets info from db and initialize wallets
	query := walletsDb.Select(q.True()).OrderBy("ID")
	var wallets []*Wallet
	err = query.Find(&wallets)
	if err != nil && err != storm.ErrNotFound {
		return nil, err
	}

	// prepare the wallets loaded from db for use
	walletsMap := make(map[int]*Wallet)
	for _, wallet := range wallets {
		err = wallet.prepare(rootDir, chainParams)
		if err != nil {
			return nil, err
		}
		walletsMap[wallet.ID] = wallet
	}

	mw := &MultiWallet{
		dbDriver:    dbDriver,
		rootDir:     rootDir,
		db:          walletsDb,
		chainParams: chainParams,
		wallets:     walletsMap,
		syncData: &syncData{
			syncCanceled:          make(chan bool),
			syncProgressListeners: make(map[string]SyncProgressListener),
		},
		txAndBlockNotificationListeners: make(map[string]TxAndBlockNotificationListener),
	}

	mw.listenForShutdown()

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
	mw.DeleteUserConfigValue(StartupSecurityTypeConfigKey)

	return nil
}

func (mw *MultiWallet) OpenWallets(startupPassphrase []byte) error {
	if mw.syncData.activeSyncData != nil {
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

		go mw.listenForTransactions(wallet.ID)
	}

	return nil
}

func (mw *MultiWallet) CreateWatchOnlyWallet(walletName, extendedPublicKey string) (*Wallet, error) {
	wallet := &Wallet{
		Name:                  walletName,
		HasDiscoveredAccounts: true,
	}

	return mw.addNewWallet(wallet, func() error {
		return wallet.createWatchingOnlyWallet(extendedPublicKey)
	})
}

func (mw *MultiWallet) CreateNewWallet(privatePassphrase string, privatePassphraseType int32) (*Wallet, error) {
	seed, err := GenerateSeed()
	if err != nil {
		return nil, err
	}

	wallet := &Wallet{
		Seed:                  seed,
		PrivatePassphraseType: privatePassphraseType,
		HasDiscoveredAccounts: true,
	}

	return mw.addNewWallet(wallet, func() error {
		return wallet.createWallet(privatePassphrase, seed)
	})
}

func (mw *MultiWallet) RestoreWallet(seedMnemonic, privatePassphrase string, privatePassphraseType int32) (*Wallet, error) {
	wallet := &Wallet{
		PrivatePassphraseType: privatePassphraseType,
		HasDiscoveredAccounts: false,
	}

	return mw.addNewWallet(wallet, func() error {
		return wallet.createWallet(privatePassphrase, seedMnemonic)
	})
}

func (mw *MultiWallet) LinkExistingWallet(walletDataDir, originalPubPass string, privatePassphraseType int32) (*Wallet, error) {
	// check if `walletDataDir` contains wallet.db
	if !WalletExistsAt(walletDataDir, mw.chainParams.Name) {
		return nil, errors.New(ErrNotExist)
	}

	wallet := &Wallet{
		PrivatePassphraseType: privatePassphraseType,
		HasDiscoveredAccounts: false, // assume that account discovery hasn't been done
	}

	return mw.addNewWallet(wallet, func() error {
		walletDataDir = filepath.Join(walletDataDir, mw.chainParams.Name)

		// move wallet.db and tx.db files to newly created dir for the wallet
		currentWalletDbFilePath := filepath.Join(walletDataDir, walletDbName)
		newWalletDbFilePath := filepath.Join(wallet.dataDir, walletDbName)
		err := os.Rename(currentWalletDbFilePath, newWalletDbFilePath)
		if err != nil {
			return err
		}

		currentTxDbFilePath := filepath.Join(walletDataDir, txindex.DbName)
		if exists, _ := fileExists(currentTxDbFilePath); exists {
			newTxDbFilePath := filepath.Join(wallet.dataDir, txindex.DbName)
			err = os.Rename(currentTxDbFilePath, newTxDbFilePath)
			if err != nil {
				return err
			}
		}

		if originalPubPass != "" && originalPubPass != w.InsecurePubPassphrase {
			// change public passphrase for newly copied wallet db to default
			ctx, _ := mw.contextWithShutdownCancel()
			err = wallet.internal.ChangePublicPassphrase(ctx, []byte(originalPubPass), []byte(w.InsecurePubPassphrase))
			if err != nil {
				return err
			}
		}

		return wallet.openWallet()
	})
}

func (mw *MultiWallet) addNewWallet(wallet *Wallet, finalizeWalletSetup func() error) (*Wallet, error) {
	if mw.syncData.activeSyncData != nil {
		return nil, errors.New(ErrSyncAlreadyInProgress)
	}

	exists, err := mw.WalletNameExists(wallet.Name)
	if err != nil {
		return nil, err
	} else if exists {
		return nil, errors.New(ErrExist)
	}

	// Perform database save operations in batch transaction
	// for automatic rollback if error occurs at any point.
	err = mw.batchDbTransaction(func(db storm.Node) error {
		// saving struct to update ID property with an autogenerated value
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

		err = wallet.prepare(mw.rootDir, mw.chainParams)
		if err != nil {
			return err
		}

		mw.wallets[wallet.ID] = wallet

		return finalizeWalletSetup()
	})

	if err == nil {
		go mw.listenForTransactions(wallet.ID)
	} else if wallet != nil {
		delete(mw.wallets, wallet.ID)
		wallet = nil
	}

	return wallet, err
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
	if mw.syncData.activeSyncData != nil {
		return errors.New(ErrSyncAlreadyInProgress)
	}

	wallet := mw.WalletWithID(walletID)
	if wallet == nil {
		return errors.New(ErrNotExist)
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

func (mw *MultiWallet) FirstOrDefaultWallet() *Wallet {
	// todo consider implementing default wallet feature

	for _, wallet := range mw.wallets {
		return wallet
	}
	return nil
}

func (mw *MultiWallet) WalletWithID(walletID int) *Wallet {
	if wallet, ok := mw.wallets[walletID]; ok {
		return wallet
	}
	return nil
}

func (mw *MultiWallet) VerifySeedForWallet(walletID int, seedMnemonic string) error {
	wallet := mw.WalletWithID(walletID)
	if wallet == nil {
		return errors.New(ErrNotExist)
	}

	if wallet.Seed == seedMnemonic {
		wallet.Seed = ""
		return translateError(mw.db.Save(wallet))
	}

	return errors.New(ErrInvalid)
}

func (mw *MultiWallet) NumWalletsNeedingSeedBackup() int32 {
	var backupsNeeded int32
	for _, wallet := range mw.wallets {
		if wallet.WalletOpened() && wallet.Seed != "" {
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

	wallet.PrivatePassphraseType = privatePassphraseType
	return mw.db.Save(wallet)
}
