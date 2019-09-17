package dcrlibwallet

import (
	"bytes"
	"fmt"
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

type TxAuthor struct {
	sendFromAccount       uint32
	destinations          []TransactionDestination
	requiredConfirmations int32
	lw                    *LibWallet
}

func (lw *LibWallet) NewUnsignedTx(sourceAccountNumber, requiredConfirmations int32) *TxAuthor {
	return &TxAuthor{
		sendFromAccount:       uint32(sourceAccountNumber),
		destinations:          make([]TransactionDestination, 0),
		requiredConfirmations: requiredConfirmations,
		lw:                    lw,
	}
}

func (tx *TxAuthor) SetSourceAccount(accountNumber int32) {
	tx.sendFromAccount = uint32(accountNumber)
}

func (tx *TxAuthor) AddSendDestination(address string, atomAmount int64, sendMax bool) {
	tx.destinations = append(tx.destinations, TransactionDestination{
		Address:    address,
		AtomAmount: atomAmount,
		SendMax:    sendMax,
	})
}

func (tx *TxAuthor) UpdateSendDestination(index int, address string, atomAmount int64, sendMax bool) {
	tx.destinations[index] = TransactionDestination{
		Address:    address,
		AtomAmount: atomAmount,
		SendMax:    sendMax,
	}
}

func (tx *TxAuthor) RemoveSendDestination(index int) {
	if len(tx.destinations) > index {
		tx.destinations = append(tx.destinations[:index], tx.destinations[index+1:]...)
	}
}

func (tx *TxAuthor) EstimateFeeAndSize() (*TxFeeAndSize, error) {
	unsignedTx, err := tx.constructTransaction()
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

func (tx *TxAuthor) EstimateMaxSendAmount() (*Amount, error) {
	txFeeAndSize, err := tx.EstimateFeeAndSize()
	if err != nil {
		return nil, err
	}

	spendableAccountBalance, err := tx.lw.SpendableForAccount(int32(tx.sendFromAccount), tx.requiredConfirmations)
	if err != nil {
		return nil, err
	}

	maxSendableAmount := spendableAccountBalance - txFeeAndSize.Fee.AtomValue

	return &Amount{
		AtomValue: maxSendableAmount,
		DcrValue:  dcrutil.Amount(maxSendableAmount).ToCoin(),
	}, nil
}

func (tx *TxAuthor) Broadcast(privatePassphrase []byte) ([]byte, error) {
	defer func() {
		for i := range privatePassphrase {
			privatePassphrase[i] = 0
		}
	}()

	n, err := tx.lw.wallet.NetworkBackend()
	if err != nil {
		log.Error(err)
		return nil, err
	}

	unsignedTx, err := tx.constructTransaction()
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

	var msgTx wire.MsgTx
	err = msgTx.Deserialize(bytes.NewReader(txBuf.Bytes()))
	if err != nil {
		log.Error(err)
		//Bytes do not represent a valid raw transaction
		return nil, err
	}

	lock := make(chan time.Time, 1)
	defer func() {
		lock <- time.Time{}
	}()

	err = tx.lw.wallet.Unlock(privatePassphrase, lock)
	if err != nil {
		log.Error(err)
		return nil, errors.New(ErrInvalidPassphrase)
	}

	var additionalPkScripts map[wire.OutPoint][]byte

	invalidSigs, err := tx.lw.wallet.SignTransaction(&msgTx, txscript.SigHashAll, additionalPkScripts, nil, nil)
	if err != nil {
		log.Error(err)
		return nil, err
	}

	invalidInputIndexes := make([]uint32, len(invalidSigs))
	for i, e := range invalidSigs {
		invalidInputIndexes[i] = e.InputIndex
	}

	var serializedTransaction bytes.Buffer
	serializedTransaction.Grow(msgTx.SerializeSize())
	err = msgTx.Serialize(&serializedTransaction)
	if err != nil {
		log.Error(err)
		return nil, err
	}

	err = msgTx.Deserialize(bytes.NewReader(serializedTransaction.Bytes()))
	if err != nil {
		//Invalid tx
		log.Error(err)
		return nil, err
	}

	txHash, err := tx.lw.wallet.PublishTransaction(&msgTx, serializedTransaction.Bytes(), n)
	if err != nil {
		return nil, translateError(err)
	}
	return txHash[:], nil
}

func (tx *TxAuthor) constructTransaction() (*txauthor.AuthoredTx, error) {
	var err error
	var outputs = make([]*wire.TxOut, 0)
	var outputSelectionAlgorithm wallet.OutputSelectionAlgorithm = wallet.OutputSelectionAlgorithmDefault
	var changeSource txauthor.ChangeSource

	for _, destination := range tx.destinations {
		// validate the amount to send to this destination address
		if !destination.SendMax && (destination.AtomAmount <= 0 || destination.AtomAmount > MaxAmountAtom) {
			return nil, errors.E(errors.Invalid, "invalid amount")
		}

		// check if multiple destinations are set to receive max amount
		if destination.SendMax && changeSource != nil {
			return nil, fmt.Errorf("cannot send max amount to multiple recipients")
		}

		if destination.SendMax {
			// This is a send max destination, set output selection algo to all.
			outputSelectionAlgorithm = wallet.OutputSelectionAlgorithmAll

			// Use this destination address to make a changeSource rather than a tx output.
			changeSource, err = txhelper.MakeTxChangeSource(destination.Address)
			if err != nil {
				log.Errorf("constructTransaction: error preparing change source: %v", err)
				return nil, fmt.Errorf("max amount change source error: %v", err)
			}

			continue // do not prepare a tx output for this destination
		}

		output, err := txhelper.MakeTxOutput(destination.Address, destination.AtomAmount)
		if err != nil {
			log.Errorf("constructTransaction: error preparing tx output: %v", err)
			return nil, fmt.Errorf("make tx output error: %v", err)
		}

		outputs = append(outputs, output)
	}

	return tx.lw.wallet.NewUnsignedTransaction(outputs, txrules.DefaultRelayFeePerKb, tx.sendFromAccount,
		tx.requiredConfirmations, outputSelectionAlgorithm, changeSource)
}
