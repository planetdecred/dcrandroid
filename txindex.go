package dcrlibwallet

import (
	"context"

	"github.com/decred/dcrd/chaincfg/chainhash"
	"github.com/decred/dcrwallet/wallet"
	"github.com/raedahgroup/dcrlibwallet/txindex"
)

func (lw *LibWallet) IndexTransactions(afterIndexing func()) error {
	ctx, _ := lw.contextWithShutdownCancel(context.Background())

	var totalIndex int32
	var txEndHeight uint32
	rangeFn := func(block *wallet.Block) (bool, error) {
		for _, transaction := range block.Transactions {

			var blockHash *chainhash.Hash
			if block.Header != nil {
				hash := block.Header.BlockHash()
				blockHash = &hash
			} else {
				blockHash = nil
			}

			tx, err := lw.decodeTransactionWithTxSummary(&transaction, blockHash)
			if err != nil {
				return false, err
			}

			err = lw.txDB.SaveOrUpdate(&Transaction{}, tx)
			if err != nil {
				log.Errorf("Index tx replace tx err :%v", err)
				return false, err
			}

			totalIndex++
		}

		if block.Header != nil {
			txEndHeight = block.Header.Height
			err := lw.txDB.SaveLastIndexPoint(int32(txEndHeight))
			if err != nil {
				log.Errorf("Set tx index end block height error: ", err)
				return false, err
			}

			log.Infof("Index saved for transactions in block %d", txEndHeight)
		}

		select {
		case <-ctx.Done():
			return true, ctx.Err()
		default:
			return false, nil
		}
	}

	beginHeight, err := lw.txDB.ReadIndexingStartBlock()
	if err != nil {
		log.Errorf("Get tx indexing start point error: %v", err)
		return err
	}

	endHeight := lw.GetBestBlock()

	startBlock := wallet.NewBlockIdentifierFromHeight(beginHeight)
	endBlock := wallet.NewBlockIdentifierFromHeight(endHeight)

	defer func() {
		afterIndexing()
		count, err := lw.txDB.Count(txindex.TxFilterAll, &Transaction{})
		if err != nil {
			log.Errorf("Post-indexing tx count error :%v", err)
			return
		}
		log.Infof("Transaction index finished at %d, %d transaction(s) indexed in total", txEndHeight, count)
	}()

	log.Infof("Indexing transactions start height: %d, end height: %d", beginHeight, endHeight)
	return lw.wallet.GetTransactions(rangeFn, startBlock, endBlock)
}
