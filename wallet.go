package dcrlibwallet

import (
	"fmt"
	"os"

	"github.com/decred/dcrwallet/errors"
	"github.com/decred/dcrwallet/wallet"
	"github.com/decred/dcrwallet/walletseed"
)

func (lw *LibWallet) WalletExists() (bool, error) {
	return lw.walletLoader.WalletExists()
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

	w, err := lw.walletLoader.CreateNewWallet(pubPass, privPass, seed)
	if err != nil {
		log.Error(err)
		return err
	}
	lw.wallet = w

	log.Info("Created Wallet")
	return nil
}

func (lw *LibWallet) CreateWatchingOnlyWallet(publicPassphrase, extendedPublicKey string) error {

	pubPass := []byte(publicPassphrase)

	w, err := lw.walletLoader.CreateWatchingOnlyWallet(extendedPublicKey, pubPass)
	if err != nil {
		log.Error(err)
		return err
	}
	lw.wallet = w

	log.Info("Created Watching Only Wallet")
	return nil
}

func (lw *LibWallet) IsWatchingOnlyWallet() bool {
	if w, ok := lw.walletLoader.LoadedWallet(); ok {
		return w.Manager.WatchingOnly()
	}

	return false
}

func (lw *LibWallet) OpenWallet(pubPass []byte) error {
	w, err := lw.walletLoader.OpenExistingWallet(pubPass)
	if err != nil {
		log.Error(err)
		return translateError(err)
	}
	lw.wallet = w
	return nil
}

func (lw *LibWallet) WalletOpened() bool {
	return lw.wallet != nil
}

func (lw *LibWallet) UnlockWallet(privPass []byte) error {
	loadedWallet, ok := lw.walletLoader.LoadedWallet()
	if !ok {
		return fmt.Errorf("wallet has not been loaded")
	}

	defer func() {
		for i := range privPass {
			privPass[i] = 0
		}
	}()

	err := loadedWallet.Unlock(privPass, nil)
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

func (lw *LibWallet) CloseWallet() error {
	err := lw.walletLoader.UnloadWallet()
	return err
}

func (lw *LibWallet) DeleteWallet(privatePassphrase []byte) error {
	defer func() {
		for i := range privatePassphrase {
			privatePassphrase[i] = 0
		}
	}()

	wallet, loaded := lw.walletLoader.LoadedWallet()
	if !loaded {
		return errors.New(ErrWalletNotLoaded)
	}

	err := wallet.Unlock(privatePassphrase, nil)
	if err != nil {
		return translateError(err)
	}
	wallet.Lock()

	lw.Shutdown()

	log.Info("Deleting Wallet")
	return os.RemoveAll(lw.walletDataDir)
}
