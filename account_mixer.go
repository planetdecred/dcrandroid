package dcrlibwallet

import (
	"errors"

	"github.com/decred/dcrwallet/ticketbuyer/v4"
)

func (mw *MultiWallet) SetAccountMixerNotification(accountMixerNotificationListener AccountMixerNotificationListener) {
	mw.accountMixerNotificationListener = accountMixerNotificationListener
}

func (wallet *Wallet) SetAccountMixerConfig(mixedAccount, changeAccount int32) error {

	accountMixerConfigSet := wallet.ReadBoolConfigValueForKey(AccountMixerConfigSet, false)
	if accountMixerConfigSet {
		return errors.New(ErrInvalid)
	}

	_, err := wallet.GetAccount(mixedAccount)
	if err != nil {
		return err
	}

	_, err = wallet.GetAccount(changeAccount)
	if err != nil {
		return err
	}

	wallet.SetInt32ConfigValueForKey(AccountMixerMixedAccount, mixedAccount)
	wallet.SetInt32ConfigValueForKey(AccountMixerChangeAccount, changeAccount)
	wallet.SetBoolConfigValueForKey(AccountMixerConfigSet, true)

	return nil
}

// StartAccountMixer starts the automatic account mixer
func (mw *MultiWallet) StartAccountMixer(walletID int, walletPassphrase string) error {

	if !mw.IsConnectedToDecredNetwork() {
		return errors.New(ErrNotConnected)
	}

	wallet := mw.WalletWithID(walletID)
	if wallet == nil {
		return errors.New(ErrNotExist)
	}

	tb := ticketbuyer.New(wallet.internal)

	mixedAccount := wallet.ReadInt32ConfigValueForKey(AccountMixerMixedAccount, -1)
	changeAccount := wallet.ReadInt32ConfigValueForKey(AccountMixerChangeAccount, -1)

	hasMixableOutput, err := wallet.accountHasMixableOutput(changeAccount)
	if err != nil {
		return translateError(err)
	} else if !hasMixableOutput {
		return errors.New(ErrNoMixableOutput)
	}

	tb.AccessConfig(func(c *ticketbuyer.Config) {
		c.MixedAccountBranch = 0
		c.MixedAccount = uint32(mixedAccount)
		c.ChangeAccount = uint32(changeAccount)
		c.CSPPServer = "cspp.decred.org:15760"
		c.BuyTickets = false
		c.MixChange = true
	})

	err = wallet.UnlockWallet([]byte(walletPassphrase))
	if err != nil {
		return translateError(err)
	}

	go func() {
		log.Info("Running account mixer")
		if mw.accountMixerNotificationListener != nil {
			mw.accountMixerNotificationListener.OnAccountMixerStarted(walletID)
		}

		ctx, cancel := mw.contextWithShutdownCancel()
		wallet.cancelAccountMixer = cancel
		err = tb.Run(ctx, []byte(walletPassphrase))
		if err != nil {
			log.Errorf("AccountMixer instance errored: %v", err)
		}

		wallet.cancelAccountMixer = nil
		if mw.accountMixerNotificationListener != nil {
			mw.accountMixerNotificationListener.OnAccountMixerEnded(walletID)
		}
	}()

	return nil
}

// StopAccountMixer stops the active account mixer
func (mw *MultiWallet) StopAccountMixer(walletID int) error {

	wallet := mw.WalletWithID(walletID)
	if wallet == nil {
		return errors.New(ErrNotExist)
	}

	if wallet.cancelAccountMixer == nil {
		return errors.New(ErrInvalid)
	}

	wallet.cancelAccountMixer()
	wallet.cancelAccountMixer = nil
	return nil
}

func (wallet *Wallet) accountHasMixableOutput(accountNumber int32) (bool, error) {

	_, tipHeight := wallet.internal.MainChainTip(wallet.shutdownContext())

	// TODO: Review this 1 confirmation currently being used by the wallet mixer
	credits, err := wallet.internal.FindEligibleOutputs(wallet.shutdownContext(), uint32(accountNumber), 1, tipHeight)
	if err != nil {
		return false, err
	}

	hasMixableOutput := false
	for _, credit := range credits {
		log.Info("Credit: ", credit.Amount)
		if credit.Amount.ToCoin() > smalletSplitPoint {
			hasMixableOutput = true
			break
		}
	}

	if !hasMixableOutput {
		lockedOutpoints := wallet.internal.LockedOutpoints()
		log.Infof("Checking %d locked outpoints", len(lockedOutpoints))
		hasMixableOutput = len(lockedOutpoints) > 0
	}

	return hasMixableOutput, nil
}

// IsAccountMixerActive returns true if account mixer is active
func (wallet *Wallet) IsAccountMixerActive() bool {
	return wallet.cancelAccountMixer != nil
}
