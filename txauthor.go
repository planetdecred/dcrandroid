package dcrlibwallet

import (
	"bytes"
	"context"
	"time"

	"github.com/decred/dcrd/dcrutil"
	"github.com/decred/dcrd/txscript"
	"github.com/decred/dcrd/wire"
	"github.com/decred/dcrwallet/errors"
	"github.com/decred/dcrwallet/wallet"
	"github.com/decred/dcrwallet/wallet/txauthor"
	"github.com/decred/dcrwallet/wallet/txrules"
	"github.com/raedahgroup/dcrlibwallet/txhelper"
)

func (lw *LibWallet) EstimateMaxSendAmount(fromAccount int32, toAddress string, requiredConfirmations int32) (*Amount, error) {
	txFeeAndSize, err := lw.CalculateNewTxFeeAndSize(0, fromAccount, toAddress, requiredConfirmations, true)
	if err != nil {
		return nil, err
	}

	spendableAccountBalance, err := lw.SpendableForAccount(fromAccount, requiredConfirmations)
	if err != nil {
		return nil, err
	}

	maxSendableAmount := spendableAccountBalance - txFeeAndSize.Fee.AtomValue

	return &Amount{
		AtomValue: maxSendableAmount,
		DcrValue:  dcrutil.Amount(maxSendableAmount).ToCoin(),
	}, nil
}

func (lw *LibWallet) CalculateNewTxFeeAndSize(amount int64, fromAccount int32, toAddress string, requiredConfirmations int32,
	spendAllFundsInAccount bool) (*TxFeeAndSize, error) {

	unsignedTx, err := lw.constructTransaction(amount, fromAccount, toAddress, requiredConfirmations, spendAllFundsInAccount)
	if err != nil {
		return nil, translateError(err)
	}

	feeToSendTx := txrules.FeeForSerializeSize(txrules.DefaultRelayFeePerKb, unsignedTx.EstimatedSignedSerializeSize)
	feeAmount := &Amount{
		AtomValue: int64(feeToSendTx),
		DcrValue:  feeToSendTx.ToCoin(),
	}

	return &TxFeeAndSize{
		EstimatedSignedSize: unsignedTx.EstimatedSignedSerializeSize,
		Fee:                 feeAmount,
	}, nil
}

func (lw *LibWallet) SendTransaction(amount int64, fromAccount int32, toAddress string, requiredConfirmations int32, spendAllFundsInAccount bool, privatePassphrase []byte) ([]byte, error) {
	defer func() {
		for i := range privatePassphrase {
			privatePassphrase[i] = 0
		}
	}()

	n, err := lw.wallet.NetworkBackend()
	if err != nil {
		log.Error(err)
		return nil, err
	}

	unsignedTx, err := lw.constructTransaction(amount, fromAccount, toAddress, requiredConfirmations, spendAllFundsInAccount)
	if err != nil {
		return nil, translateError(err)
	}

	if unsignedTx.ChangeIndex >= 0 {
		unsignedTx.RandomizeChangePosition()
	}

	var txBuf bytes.Buffer
	txBuf.Grow(unsignedTx.Tx.SerializeSize())
	err = unsignedTx.Tx.Serialize(&txBuf)
	if err != nil {
		log.Error(err)
		return nil, err
	}

	var tx wire.MsgTx
	err = tx.Deserialize(bytes.NewReader(txBuf.Bytes()))
	if err != nil {
		log.Error(err)
		//Bytes do not represent a valid raw transaction
		return nil, err
	}

	lock := make(chan time.Time, 1)
	defer func() {
		lock <- time.Time{}
	}()

	err = lw.wallet.Unlock(privatePassphrase, lock)
	if err != nil {
		log.Error(err)
		return nil, errors.New(ErrInvalidPassphrase)
	}

	var additionalPkScripts map[wire.OutPoint][]byte

	invalidSigs, err := lw.wallet.SignTransaction(&tx, txscript.SigHashAll, additionalPkScripts, nil, nil)
	if err != nil {
		log.Error(err)
		return nil, err
	}

	invalidInputIndexes := make([]uint32, len(invalidSigs))
	for i, e := range invalidSigs {
		invalidInputIndexes[i] = e.InputIndex
	}

	var serializedTransaction bytes.Buffer
	serializedTransaction.Grow(tx.SerializeSize())
	err = tx.Serialize(&serializedTransaction)
	if err != nil {
		log.Error(err)
		return nil, err
	}

	var msgTx wire.MsgTx
	err = msgTx.Deserialize(bytes.NewReader(serializedTransaction.Bytes()))
	if err != nil {
		//Invalid tx
		log.Error(err)
		return nil, err
	}

	txHash, err := lw.wallet.PublishTransaction(&msgTx, serializedTransaction.Bytes(), n)
	if err != nil {
		return nil, translateError(err)
	}
	return txHash[:], nil
}

func (lw *LibWallet) constructTransaction(amount int64, fromAccount int32, toAddress string, requiredConfirmations int32,
	spendAllFundsInAccount bool) (unsignedTx *txauthor.AuthoredTx, err error) {

	if !spendAllFundsInAccount && (amount <= 0 || amount > MaxAmountAtom) {
		return nil, errors.E(errors.Invalid, "invalid amount")
	}

	// `outputSelectionAlgorithm` specifies the algorithm to use when selecting outputs to construct a transaction.
	// If spendAllFundsInAccount == true, `outputSelectionAlgorithm` will be `wallet.OutputSelectionAlgorithmAll`.
	// Else, the default algorithm (`wallet.OutputSelectionAlgorithmDefault`) will be used.
	var outputSelectionAlgorithm wallet.OutputSelectionAlgorithm

	// If spendAllFundsInAccount == false, `outputs` will contain destination address and amount to send.
	// Else, the destination address will be used to make a `changeSource`.
	var outputs []*wire.TxOut
	var changeSource txauthor.ChangeSource

	if spendAllFundsInAccount {
		outputSelectionAlgorithm = wallet.OutputSelectionAlgorithmAll
		changeSource, err = txhelper.MakeTxChangeSource(toAddress)
	} else {
		outputSelectionAlgorithm = wallet.OutputSelectionAlgorithmDefault
		outputs, err = txhelper.MakeTxOutputs([]txhelper.TransactionDestination{
			{Address: toAddress, Amount: dcrutil.Amount(amount).ToCoin()},
		})
	}

	if err != nil {
		log.Error(err)
		return
	}

	return lw.wallet.NewUnsignedTransaction(outputs, txrules.DefaultRelayFeePerKb, uint32(fromAccount),
		requiredConfirmations, outputSelectionAlgorithm, changeSource)
}

func (lw *LibWallet) PublishUnminedTransactions() error {
	netBackend, err := lw.wallet.NetworkBackend()
	if err != nil {
		return errors.New(ErrNotConnected)
	}
	ctx, _ := contextWithShutdownCancel(context.Background())
	err = lw.wallet.PublishUnminedTransactions(ctx, netBackend)
	return err
}
