package dcrlibwallet

import (
	"encoding/json"
	"sort"

	"github.com/decred/dcrd/chaincfg/chainhash"
	"github.com/raedahgroup/dcrlibwallet/txhelper"
	"github.com/raedahgroup/dcrlibwallet/txindex"
)

const (
	// Export constants for use in mobile apps
	// since gomobile excludes fields from sub packages.
	TxFilterAll         = txindex.TxFilterAll
	TxFilterSent        = txindex.TxFilterSent
	TxFilterReceived    = txindex.TxFilterReceived
	TxFilterTransferred = txindex.TxFilterTransferred
	TxFilterStaking     = txindex.TxFilterStaking
	TxFilterCoinBase    = txindex.TxFilterCoinBase
	TxFilterRegular     = txindex.TxFilterRegular

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

func (wallet *Wallet) GetTransaction(txHash []byte) (string, error) {
	transaction, err := wallet.GetTransactionRaw(txHash)
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

func (wallet *Wallet) GetTransactionRaw(txHash []byte) (*Transaction, error) {
	hash, err := chainhash.NewHash(txHash)
	if err != nil {
		log.Error(err)
		return nil, err
	}

	txSummary, _, blockHash, err := wallet.internal.TransactionSummary(wallet.shutdownContext(), hash)
	if err != nil {
		log.Error(err)
		return nil, err
	}

	return wallet.decodeTransactionWithTxSummary(txSummary, blockHash)
}

func (wallet *Wallet) GetTransactions(offset, limit, txFilter int32, newestFirst bool) (string, error) {
	transactions, err := wallet.GetTransactionsRaw(offset, limit, txFilter, newestFirst)
	if err != nil {
		return "", err
	}

	jsonEncodedTransactions, err := json.Marshal(&transactions)
	if err != nil {
		return "", err
	}

	return string(jsonEncodedTransactions), nil
}

func (wallet *Wallet) GetTransactionsRaw(offset, limit, txFilter int32, newestFirst bool) (transactions []Transaction, err error) {
	err = wallet.txDB.Read(offset, limit, txFilter, newestFirst, &transactions)
	return
}

func (mw *MultiWallet) GetTransactions(offset, limit, txFilter int32, newestFirst bool) (string, error) {
	transactions := make([]Transaction, 0)
	for _, wallet := range mw.wallets {
		walletTransactions, err := wallet.GetTransactionsRaw(offset, limit, txFilter, newestFirst)
		if err != nil {
			return "", nil
		}

		transactions = append(transactions, walletTransactions...)
	}

	// sort transaction by timestamp in descending order
	sort.Slice(transactions[:], func(i, j int) bool {
		if newestFirst {
			return transactions[i].Timestamp > transactions[j].Timestamp
		}
		return transactions[i].Timestamp < transactions[j].Timestamp
	})

	if len(transactions) > int(limit) && limit > 0 {
		transactions = transactions[:limit]
	}

	jsonEncodedTransactions, err := json.Marshal(&transactions)
	if err != nil {
		return "", err
	}

	return string(jsonEncodedTransactions), nil
}

func (wallet *Wallet) CountTransactions(txFilter int32) (int, error) {
	return wallet.txDB.Count(txFilter, &Transaction{})
}

func TxMatchesFilter(txType string, txDirection, txFilter int32) bool {
	return txindex.TxMatchesFilter(txType, txDirection, txFilter)
}
