package dcrlibwallet

import (
	"encoding/json"

	"github.com/decred/dcrwallet/errors/v2"
)

const smalletSplitPoint = 000.00262144

func (mw *MultiWallet) listenForTransactions(walletID int) {
	go func() {

		wallet := mw.wallets[walletID]
		n := wallet.internal.NtfnServer.TransactionNotifications()

		for {
			select {
			case v := <-n.C:
				if v == nil {
					return
				}
				for _, transaction := range v.UnminedTransactions {
					tempTransaction, err := wallet.decodeTransactionWithTxSummary(&transaction, nil)
					if err != nil {
						log.Errorf("[%d] Error ntfn parse tx: %v", wallet.ID, err)
						return
					}

					overwritten, err := wallet.txDB.SaveOrUpdate(&Transaction{}, tempTransaction)
					if err != nil {
						log.Errorf("[%d] New Tx save err: %v", wallet.ID, err)
						return
					}

					if !overwritten {
						log.Infof("[%d] New Transaction %s", wallet.ID, tempTransaction.Hash)

						result, err := json.Marshal(tempTransaction)
						if err != nil {
							log.Error(err)
						} else {
							mw.mempoolTransactionNotification(string(result))
						}
					}
				}

				for _, block := range v.AttachedBlocks {
					blockHash := block.Header.BlockHash()
					for _, transaction := range block.Transactions {
						tempTransaction, err := wallet.decodeTransactionWithTxSummary(&transaction, &blockHash)
						if err != nil {
							log.Errorf("[%d] Error ntfn parse tx: %v", wallet.ID, err)
							return
						}

						_, err = wallet.txDB.SaveOrUpdate(&Transaction{}, tempTransaction)
						if err != nil {
							log.Errorf("[%d] Incoming block replace tx error :%v", wallet.ID, err)
							return
						}
						mw.publishTransactionConfirmed(wallet.ID, transaction.Hash.String(), int32(block.Header.Height))
					}

					mw.publishBlockAttached(wallet.ID, int32(block.Header.Height))
				}

				if len(v.AttachedBlocks) > 0 {
					mw.checkWalletMixers()
				}

			case <-mw.syncData.syncCanceled:
				n.Done()
			}
		}
	}()
}

func (mw *MultiWallet) AddTxAndBlockNotificationListener(txAndBlockNotificationListener TxAndBlockNotificationListener, uniqueIdentifier string) error {
	mw.notificationListenersMu.Lock()
	defer mw.notificationListenersMu.Unlock()

	_, ok := mw.txAndBlockNotificationListeners[uniqueIdentifier]
	if ok {
		return errors.New(ErrListenerAlreadyExist)
	}

	mw.txAndBlockNotificationListeners[uniqueIdentifier] = txAndBlockNotificationListener

	return nil
}

func (mw *MultiWallet) RemoveTxAndBlockNotificationListener(uniqueIdentifier string) {
	mw.notificationListenersMu.Lock()
	defer mw.notificationListenersMu.Unlock()

	delete(mw.txAndBlockNotificationListeners, uniqueIdentifier)
}

func (mw *MultiWallet) checkWalletMixers() {
	for _, wallet := range mw.wallets {
		if wallet.IsAccountMixerActive() {
			changeAccount := wallet.ReadInt32ConfigValueForKey(AccountMixerChangeAccount, -1)
			hasMixableOutput, err := wallet.accountHasMixableOutput(changeAccount)
			if err != nil {
				log.Errorf("Error checking for mixable outputs: %v", err)
			}

			if !hasMixableOutput {
				log.Infof("[%d] change account does not have a mixable output, stopping account mixer", wallet.ID)
				err = mw.StopAccountMixer(wallet.ID)
				if err != nil {
					log.Errorf("Error stopping account mixer: %v", err)
				}
			}
		}
	}
}

func (mw *MultiWallet) mempoolTransactionNotification(transaction string) {
	mw.notificationListenersMu.RLock()
	defer mw.notificationListenersMu.RUnlock()

	for _, txAndBlockNotifcationListener := range mw.txAndBlockNotificationListeners {
		txAndBlockNotifcationListener.OnTransaction(transaction)
	}
}

func (mw *MultiWallet) publishTransactionConfirmed(walletID int, transactionHash string, blockHeight int32) {
	mw.notificationListenersMu.RLock()
	defer mw.notificationListenersMu.RUnlock()

	for _, txAndBlockNotifcationListener := range mw.txAndBlockNotificationListeners {
		txAndBlockNotifcationListener.OnTransactionConfirmed(walletID, transactionHash, blockHeight)
	}
}

func (mw *MultiWallet) publishBlockAttached(walletID int, blockHeight int32) {
	mw.notificationListenersMu.RLock()
	defer mw.notificationListenersMu.RUnlock()

	for _, txAndBlockNotifcationListener := range mw.txAndBlockNotificationListeners {
		txAndBlockNotifcationListener.OnBlockAttached(walletID, blockHeight)
	}
}
