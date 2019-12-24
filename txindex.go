package dcrlibwallet

import (
	"github.com/decred/dcrd/chaincfg/chainhash"
	w "github.com/decred/dcrwallet/wallet/v3"
	"github.com/raedahgroup/dcrlibwallet/txindex"
)

func (wallet *Wallet) IndexTransactions() error {
	ctx := wallet.shutdownContext()

	var totalIndex int32
	var txEndHeight uint32
	rangeFn := func(block *w.Block) (bool, error) {
		for _, transaction := range block.Transactions {

			var blockHash *chainhash.Hash
			if block.Header != nil {
				hash := block.Header.BlockHash()
				blockHash = &hash
			} else {
				blockHash = nil
			}

			tx, err := wallet.decodeTransactionWithTxSummary(&transaction, blockHash)
			if err != nil {
				return false, err
			}

			_, err = wallet.txDB.SaveOrUpdate(&Transaction{}, tx)
			if err != nil {
				log.Errorf("[%d] Index tx replace tx err : %v", wallet.ID, err)
				return false, err
			}

			totalIndex++
		}

		if block.Header != nil {
			txEndHeight = block.Header.Height
			err := wallet.txDB.SaveLastIndexPoint(int32(txEndHeight))
			if err != nil {
				log.Errorf("[%d] Set tx index end block height error: ", wallet.ID, err)
				return false, err
			}

			log.Debugf("[%d] Index saved for transactions in block %d", wallet.ID, txEndHeight)
		}

		select {
		case <-ctx.Done():
			return true, ctx.Err()
		default:
			return false, nil
		}
	}

	beginHeight, err := wallet.txDB.ReadIndexingStartBlock()
	if err != nil {
		log.Errorf("[%d] Get tx indexing start point error: %v", wallet.ID, err)
		return err
	}

	endHeight := wallet.GetBestBlock()

	startBlock := w.NewBlockIdentifierFromHeight(beginHeight)
	endBlock := w.NewBlockIdentifierFromHeight(endHeight)

	defer func() {
		count, err := wallet.txDB.Count(txindex.TxFilterAll, &Transaction{})
		if err != nil {
			log.Errorf("[%d] Post-indexing tx count error :%v", wallet.ID, err)
		} else if count > 0 {
			log.Debugf("[%d] Transaction index finished at %d, %d transaction(s) indexed in total", wallet.ID, txEndHeight, count)
		}
	}()

	log.Debugf("[%d] Indexing transactions start height: %d, end height: %d", wallet.ID, beginHeight, endHeight)
	return wallet.internal.GetTransactions(ctx, rangeFn, startBlock, endBlock)
}

func (wallet *Wallet) reindexTransactions() error {
	err := wallet.txDB.ClearSavedTransactions(&Transaction{})
	if err != nil {
		return err
	}

	return wallet.IndexTransactions()
}
