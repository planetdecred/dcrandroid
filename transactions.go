package dcrlibwallet

import (
	"encoding/json"
	"github.com/decred/dcrd/chaincfg/chainhash"
	"github.com/raedahgroup/dcrlibwallet/txhelper"
	"github.com/raedahgroup/dcrlibwallet/txindex"
)

type TransactionListener interface {
	OnTransaction(transaction string)
	OnTransactionConfirmed(hash string, height int32)
	OnBlockAttached(height int32, timestamp int64)
}

const (
	// Export constants for use in mobile apps
	// since gomobile excludes fields from sub packages.
	TxFilterAll         = txindex.TxFilterAll
	TxFilterSent        = txindex.TxFilterSent
	TxFilterReceived    = txindex.TxFilterReceived
	TxFilterTransferred = txindex.TxFilterTransferred
	TxFilterStaking     = txindex.TxFilterStaking
	TxFilterCoinBase    = txindex.TxFilterCoinBase

	TxDirectionInvalid     = txhelper.TxDirectionInvalid
	TxDirectionSent        = txhelper.TxDirectionSent
	TxDirectionReceived    = txhelper.TxDirectionReceived
	TxDirectionTransferred = txhelper.TxDirectionTransferred

	TxTypeRegular        = txhelper.TxTypeRegular
	TxTypeCoinBase       = txhelper.TxTypeCoinBase
	TxTypeTicketPurchase = txhelper.TxTypeTicketPurchase
	TxTypeVote           = txhelper.TxTypeVote
	TxTypeRevocation     = txhelper.TxTypeRevocation
)

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

	return lw.decodeTransactionWithTxSummary(txSummary, blockHash)
}

func (lw *LibWallet) GetTransactions(offset, limit, txFilter int32) (string, error) {
	var transactions []Transaction
	err := lw.txDB.Read(offset, limit, txFilter, &transactions)
	if err != nil {
		return "", err
	}

	jsonEncodedTransactions, err := json.Marshal(&transactions)
	if err != nil {
		return "", err
	}

	return string(jsonEncodedTransactions), nil
}

func (lw *LibWallet) CountTransactions(txFilter int32) (int, error) {
	return lw.txDB.Count(txFilter, &Transaction{})
}

func (lw *LibWallet) DetermineTxFilter(txType string, txDirection int32) int32 {
	return txindex.DetermineTxFilter(txType, txDirection)
}
