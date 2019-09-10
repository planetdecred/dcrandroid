package dcrlibwallet

import (
	"encoding/json"
)

func (lw *LibWallet) TransactionNotification(listener TransactionListener) {
	go func() {
		n := lw.wallet.NtfnServer.TransactionNotifications()
		defer n.Done() // disassociate this notification client from server when this goroutine exits.

		for {
			v := <-n.C

			for _, transaction := range v.UnminedTransactions {
				tempTransaction, err := lw.decodeTransactionWithTxSummary(&transaction, nil)
				if err != nil {
					log.Errorf("Error ntfn parse tx: %v", err)
					return
				}

				err = lw.txDB.SaveOrUpdate(tempTransaction.Hash, tempTransaction)
				if err != nil {
					log.Errorf("Tx ntfn replace tx err: %v", err)
				}

				log.Info("New Transaction")

				result, err := json.Marshal(tempTransaction)
				if err != nil {
					log.Error(err)
				} else {
					listener.OnTransaction(string(result))
				}
			}

			for _, block := range v.AttachedBlocks {
				listener.OnBlockAttached(int32(block.Header.Height), block.Header.Timestamp.UnixNano())
				blockHash := block.Header.BlockHash()
				for _, transaction := range block.Transactions {
					tempTransaction, err := lw.decodeTransactionWithTxSummary(&transaction, &blockHash)
					if err != nil {
						log.Errorf("Error ntfn parse tx: %v", err)
						return
					}

					err = lw.txDB.SaveOrUpdate(tempTransaction.Hash, tempTransaction)
					if err != nil {
						log.Errorf("Incoming block replace tx error :%v", err)
						return
					}
					listener.OnTransactionConfirmed(transaction.Hash.String(), int32(block.Header.Height))
				}
			}
		}
	}()
}
