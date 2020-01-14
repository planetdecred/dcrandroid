package dcrlibwallet

import (
	"fmt"

	"github.com/decred/dcrd/chaincfg/chainhash"
	w "github.com/decred/dcrwallet/wallet/v3"
)

const BlockHeightInvalid int32 = -1

func (wallet *Wallet) decodeTransactionWithTxSummary(txSummary *w.TransactionSummary,
	blockHash *chainhash.Hash) (*Transaction, error) {

	var blockHeight int32 = BlockHeightInvalid
	if blockHash != nil {
		blockIdentifier := w.NewBlockIdentifierFromHash(blockHash)
		blockInfo, err := wallet.internal.BlockInfo(wallet.shutdownContext(), blockIdentifier)
		if err != nil {
			log.Error(err)
		} else {
			blockHeight = blockInfo.Height
		}
	}

	walletInputs := make([]*WalletInput, len(txSummary.MyInputs))
	for i, input := range txSummary.MyInputs {
		accountNumber := int32(input.PreviousAccount)
		walletInputs[i] = &WalletInput{
			Index:    int32(input.Index),
			AmountIn: int64(input.PreviousAmount),
			WalletAccount: &WalletAccount{
				AccountNumber: accountNumber,
				AccountName:   wallet.AccountName(accountNumber),
			},
		}
	}

	walletOutputs := make([]*WalletOutput, len(txSummary.MyOutputs))
	for i, output := range txSummary.MyOutputs {
		accountNumber := int32(output.Account)
		walletOutputs[i] = &WalletOutput{
			Index:     int32(output.Index),
			AmountOut: int64(output.Amount),
			Internal:  output.Internal,
			Address:   output.Address.Address(),
			WalletAccount: &WalletAccount{
				AccountNumber: accountNumber,
				AccountName:   wallet.AccountName(accountNumber),
			},
		}
	}

	walletTx := &TxInfoFromWallet{
		WalletID:    wallet.ID,
		BlockHeight: blockHeight,
		Timestamp:   txSummary.Timestamp,
		Hex:         fmt.Sprintf("%x", txSummary.Transaction),
		Inputs:      walletInputs,
		Outputs:     walletOutputs,
	}

	return DecodeTransaction(walletTx, wallet.chainParams)
}
