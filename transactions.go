package dcrlibwallet

import (
	"bytes"
	"context"
	"encoding/binary"
	"encoding/json"
	"fmt"
	"math"

	"github.com/asdine/storm"
	"github.com/asdine/storm/q"
	"github.com/decred/dcrd/blockchain/stake"
	"github.com/decred/dcrd/chaincfg"
	"github.com/decred/dcrd/chaincfg/chainhash"
	"github.com/decred/dcrd/dcrutil"
	"github.com/decred/dcrd/txscript"
	"github.com/decred/dcrd/wire"
	"github.com/decred/dcrwallet/wallet"
)

type TransactionListener interface {
	OnTransaction(transaction string)
	OnTransactionConfirmed(hash string, height int32)
	OnBlockAttached(height int32, timestamp int64)
}

const (
	BucketTxInfo   = "TxIndexInfo"
	KeyEndBlock    = "EndBlock"
	MaxReOrgBlocks = 6
)

const (
	TxFilterAll         int32 = 0
	TxFilterSent        int32 = 1
	TxFilterReceived    int32 = 2
	TxFilterTransferred int32 = 3
	TxFilterStaking     int32 = 4
	TxFilterCoinBase    int32 = 5

	TxDirectionInvalid     int32 = -1
	TxDirectionSent        int32 = 0
	TxDirectionReceived    int32 = 1
	TxDirectionTransferred int32 = 2

	TxTypeRegular        = "REGULAR"
	TxTypeCoinBase       = "COINBASE"
	TxTypeTicketPurchase = "TICKET_PURCHASE"
	TxTypeVote           = "VOTE"
	TxTypeRevocation     = "REVOCATION"
)

func (lw *LibWallet) IndexTransactions(beginHeight int32, endHeight int32, afterIndexing func()) error {
	ctx, _ := contextWithShutdownCancel(context.Background())

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

			tx, err := lw.parseTxSummary(&transaction, blockHash)
			if err != nil {
				return false, err
			}

			err = lw.replaceTxIfExist(tx)
			if err != nil {
				log.Errorf("Index tx replace tx err :%v", err)
				return false, err
			}

			totalIndex++
		}

		if block.Header != nil {
			txEndHeight = block.Header.Height
			err := lw.txDB.Set(BucketTxInfo, KeyEndBlock, &txEndHeight)
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

	if beginHeight == -1 {
		var previousEndBlock int32
		err := lw.txDB.Get(BucketTxInfo, KeyEndBlock, &previousEndBlock)
		if err != nil && err != storm.ErrNotFound {
			log.Errorf("Get not found :%v", err)
			return err
		}

		beginHeight = previousEndBlock
		beginHeight -= MaxReOrgBlocks

		if beginHeight < 0 {
			beginHeight = 0
		}
	}

	if beginHeight > endHeight {
		endHeight = lw.GetBestBlock()
	}

	startBlock := wallet.NewBlockIdentifierFromHeight(beginHeight)
	endBlock := wallet.NewBlockIdentifierFromHeight(endHeight)

	defer func() {
		afterIndexing()
		count, err := lw.txDB.Count(&Transaction{})
		if err != nil {
			log.Errorf("Count Error :%v", err)
			return
		}
		log.Infof("Transaction index finished at %d, %d transaction(s) indexed in total", txEndHeight, count)
	}()

	log.Infof("Indexing transactions start height: %d, end height: %d", beginHeight, endHeight)
	return lw.wallet.GetTransactions(rangeFn, startBlock, endBlock)
}

func (lw *LibWallet) TransactionNotification(listener TransactionListener) {
	go func() {
		n := lw.wallet.NtfnServer.TransactionNotifications()
		defer n.Done()
		for {
			v := <-n.C
			for _, transaction := range v.UnminedTransactions {
				tempTransaction, err := lw.parseTxSummary(&transaction, nil)
				if err != nil {
					log.Errorf("Error ntfn parse tx: %v", err)
					return
				}

				err = lw.replaceTxIfExist(tempTransaction)
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
				for _, transaction := range block.Transactions {

					tempTransaction, err := lw.parseTxSummary(&transaction, nil)
					if err != nil {
						log.Errorf("Error ntfn parse tx: %v", err)
						return
					}

					err = lw.replaceTxIfExist(tempTransaction)
					if err != nil {
						log.Errorf("Incoming block replace tx error :%v", err)
						return
					}
					listener.OnTransactionConfirmed(fmt.Sprintf("%02x", reverse(transaction.Hash[:])), int32(block.Header.Height))
				}
			}
		}
	}()
}

func (lw *LibWallet) GetTransaction(txHash []byte) (string, error) {
	transaction, err := lw.GetTransactionRaw(txHash)
	if err != nil {
		log.Error(err)
		return "", err
	}

	result, err := json.Marshal(transaction)

	if err != nil {
		return "", err
	}

	return string(result), nil
}

func (lw *LibWallet) GetTransactionRaw(txHash []byte) (*Transaction, error) {
	hash, err := chainhash.NewHash(txHash)
	if err != nil {
		log.Error(err)
		return nil, err
	}

	txSummary, _, blockHash, err := lw.wallet.TransactionSummary(hash)
	if err != nil {
		log.Error(err)
		return nil, err
	}

	return lw.parseTxSummary(txSummary, blockHash)
}

func (lw *LibWallet) replaceTxIfExist(tx *Transaction) error {
	var oldTx Transaction
	err := lw.txDB.One("Hash", tx.Hash, &oldTx)
	if err != nil {
		if err != storm.ErrNotFound {
			log.Errorf("Find old tx error: %v", err)
			return err
		}
	} else {
		err = lw.txDB.DeleteStruct(&oldTx)
		if err != nil {
			log.Errorf("Delete old tx error: %v", err)
			return err
		}
	}

	err = lw.txDB.Save(tx)
	if err != nil {
		log.Errorf("Save transaction error :%v", err)
		return err
	}

	return nil
}

func (lw *LibWallet) parseTxSummary(tx *wallet.TransactionSummary, blockHash *chainhash.Hash) (*Transaction, error) {
	var inputTotal int64
	var outputTotal int64
	var amount int64

	credits := make([]TransactionCredit, len(tx.MyOutputs))
	for index, credit := range tx.MyOutputs {
		outputTotal += int64(credit.Amount)
		credits[index] = TransactionCredit{
			Index:    int32(credit.Index),
			Account:  int32(credit.Account),
			Internal: credit.Internal,
			Amount:   int64(credit.Amount),
			Address:  credit.Address.String()}
	}

	debits := make([]TransactionDebit, len(tx.MyInputs))
	for index, debit := range tx.MyInputs {
		inputTotal += int64(debit.PreviousAmount)
		debits[index] = TransactionDebit{
			Index:           int32(debit.Index),
			PreviousAccount: int32(debit.PreviousAccount),
			PreviousAmount:  int64(debit.PreviousAmount),
			AccountName:     lw.AccountName(int32(debit.PreviousAccount))}
	}

	var direction int32 = TxDirectionInvalid
	if tx.Type == wallet.TransactionTypeRegular {
		amountDifference := outputTotal - inputTotal
		if amountDifference < 0 && (float64(tx.Fee) == math.Abs(float64(amountDifference))) {
			//Transfered
			direction = TxDirectionTransferred
			amount = int64(tx.Fee)
		} else if amountDifference > 0 {
			//Received
			direction = TxDirectionReceived
			amount = outputTotal
		} else {
			//Sent
			direction = TxDirectionSent
			amount = inputTotal
			amount -= outputTotal

			amount -= int64(tx.Fee)
		}
	}

	var height int32 = -1
	if blockHash != nil {
		blockIdentifier := wallet.NewBlockIdentifierFromHash(blockHash)
		blockInfo, err := lw.wallet.BlockInfo(blockIdentifier)
		if err != nil {
			log.Error(err)
		} else {
			height = blockInfo.Height
		}
	}

	transaction := &Transaction{
		Fee:       int64(tx.Fee),
		Hash:      fmt.Sprintf("%02x", reverse(tx.Hash[:])),
		Raw:       fmt.Sprintf("%02x", tx.Transaction[:]),
		Timestamp: tx.Timestamp,
		Type:      transactionType(tx.Type),
		Credits:   &credits,
		Amount:    amount,
		Height:    height,
		Direction: direction,
		Debits:    &debits}

	return transaction, nil

}

func (lw *LibWallet) DecodeTransaction(txHash []byte) (string, error) {
	hash, err := chainhash.NewHash(txHash)
	if err != nil {
		log.Error(err)
		return "", err
	}
	txSummary, _, _, err := lw.wallet.TransactionSummary(hash)
	if err != nil {
		log.Error(err)
		return "", err
	}
	serializedTx := txSummary.Transaction
	var mtx wire.MsgTx
	err = mtx.Deserialize(bytes.NewReader(serializedTx))
	if err != nil {
		log.Error(err)
		return "", err
	}

	var ssGenVersion uint32
	var lastBlockValid bool
	var votebits string
	if stake.IsSSGen(&mtx) {
		ssGenVersion = voteVersion(&mtx)
		lastBlockValid = voteBits(&mtx)&uint16(BlockValid) != 0
		votebits = fmt.Sprintf("%#04x", voteBits(&mtx))
	}

	var tx = DecodedTransaction{
		Hash:           fmt.Sprintf("%02x", reverse(hash[:])),
		Type:           transactionType(wallet.TxTransactionType(&mtx)),
		Version:        int32(mtx.Version),
		LockTime:       int32(mtx.LockTime),
		Expiry:         int32(mtx.Expiry),
		Inputs:         decodeTxInputs(&mtx),
		Outputs:        decodeTxOutputs(&mtx, lw.wallet.ChainParams()),
		VoteVersion:    int32(ssGenVersion),
		LastBlockValid: lastBlockValid,
		VoteBits:       votebits,
	}
	result, _ := json.Marshal(tx)
	return string(result), nil
}

func (lw *LibWallet) GetTransactions(limit, txFilter int32) (string, error) {
	query := lw.prepareTxQuery(txFilter)
	if limit > 0 {
		query = query.Limit(int(limit))
	}

	var transactions []Transaction

	err := query.Find(&transactions)
	if err != nil {
		return "", nil
	}

	jsonEncodedTransactions, err := json.Marshal(&transactions)
	if err != nil {
		return "", err
	}

	return string(jsonEncodedTransactions), nil
}

func (lw *LibWallet) CountTransactions(txFilter int32) (int, error) {
	query := lw.prepareTxQuery(txFilter)

	count, err := query.Count(&Transaction{})
	if err != nil {
		return -1, err
	}

	return count, nil
}

func (lw *LibWallet) DetermineTxFilter(txType string, txDirection int32) int32 {
	if txType == TxTypeCoinBase {
		return TxFilterCoinBase
	}
	if txType != TxTypeRegular {
		return TxFilterStaking
	}

	switch txDirection {
	case TxDirectionSent:
		return TxFilterSent
	case TxDirectionReceived:
		return TxFilterReceived
	default:
		return TxFilterTransferred
	}
}

// - Helper Functions

func (lw *LibWallet) prepareTxQuery(txFilter int32) (query storm.Query) {
	switch txFilter {
	case TxFilterSent:
		query = lw.txDB.Select(
			q.Eq("Direction", TxDirectionSent),
		)
	case TxFilterReceived:
		query = lw.txDB.Select(
			q.Eq("Direction", TxDirectionReceived),
		)
	case TxFilterTransferred:
		query = lw.txDB.Select(
			q.Eq("Direction", TxDirectionTransferred),
		)
	case TxFilterStaking:
		query = lw.txDB.Select(
			q.Not(
				q.Eq("Type", TxTypeRegular),
				q.Eq("Type", TxTypeCoinBase),
			),
		)
	case TxFilterCoinBase:
		query = lw.txDB.Select(
			q.Eq("Type", TxTypeCoinBase),
		)
	default:
		query = lw.txDB.Select(
			q.True(),
		)
	}

	query = query.OrderBy("Timestamp").Reverse()
	return
}

func decodeTxInputs(mtx *wire.MsgTx) []DecodedInput {
	inputs := make([]DecodedInput, len(mtx.TxIn))
	for i, txIn := range mtx.TxIn {
		inputs[i] = DecodedInput{
			PreviousTransactionHash:  fmt.Sprintf("%02x", reverse(txIn.PreviousOutPoint.Hash[:])),
			PreviousTransactionIndex: int32(txIn.PreviousOutPoint.Index),
			AmountIn:                 txIn.ValueIn,
		}
	}
	return inputs
}

func decodeTxOutputs(mtx *wire.MsgTx, chainParams *chaincfg.Params) []DecodedOutput {
	outputs := make([]DecodedOutput, len(mtx.TxOut))
	txType := stake.DetermineTxType(mtx)
	for i, v := range mtx.TxOut {

		var addrs []dcrutil.Address
		var encodedAddrs []string
		var scriptClass txscript.ScriptClass
		if (txType == stake.TxTypeSStx) && (stake.IsStakeSubmissionTxOut(i)) {
			scriptClass = txscript.StakeSubmissionTy
			addr, err := stake.AddrFromSStxPkScrCommitment(v.PkScript,
				chainParams)
			if err != nil {
				encodedAddrs = []string{fmt.Sprintf(
					"[error] failed to decode ticket "+
						"commitment addr output for tx hash "+
						"%v, output idx %v", mtx.TxHash(), i)}
			} else {
				encodedAddrs = []string{addr.EncodeAddress()}
			}
		} else {
			// Ignore the error here since an error means the script
			// couldn't parse and there is no additional information
			// about it anyways.
			scriptClass, addrs, _, _ = txscript.ExtractPkScriptAddrs(
				v.Version, v.PkScript, chainParams)
			encodedAddrs = make([]string, len(addrs))
			for j, addr := range addrs {
				encodedAddrs[j] = addr.EncodeAddress()
			}
		}

		outputs[i] = DecodedOutput{
			Index:      int32(i),
			Value:      v.Value,
			Version:    int32(v.Version),
			Addresses:  encodedAddrs,
			ScriptType: scriptClass.String(),
		}
	}

	return outputs
}

func voteVersion(mtx *wire.MsgTx) uint32 {
	if len(mtx.TxOut[1].PkScript) < 8 {
		return 0 // Consensus version absent
	}

	return binary.LittleEndian.Uint32(mtx.TxOut[1].PkScript[4:8])
}

func voteBits(mtx *wire.MsgTx) uint16 {
	return binary.LittleEndian.Uint16(mtx.TxOut[1].PkScript[2:4])
}

func reverse(hash []byte) []byte {
	for i := 0; i < len(hash)/2; i++ {
		j := len(hash) - i - 1
		hash[i], hash[j] = hash[j], hash[i]
	}
	return hash
}

func transactionType(txType wallet.TransactionType) string {
	switch txType {
	case wallet.TransactionTypeCoinbase:
		return TxTypeCoinBase
	case wallet.TransactionTypeTicketPurchase:
		return TxTypeTicketPurchase
	case wallet.TransactionTypeVote:
		return TxTypeVote
	case wallet.TransactionTypeRevocation:
		return TxTypeRevocation
	default:
		return TxTypeRegular
	}
}
