package txhelper

import (
	"github.com/decred/dcrd/dcrutil"
	"github.com/decred/dcrd/txscript"
	"github.com/decred/dcrd/wire"
	"github.com/raedahgroup/dcrlibwallet/addresshelper"
)

func MakeTxOutputs(destinations []TransactionDestination) (outputs []*wire.TxOut, err error) {
	for _, destination := range destinations {
		var output *wire.TxOut
		output, err = MakeTxOutput(destination)
		if err != nil {
			return
		}

		outputs = append(outputs, output)
	}
	return
}

func MakeTxOutput(destination TransactionDestination) (output *wire.TxOut, err error) {
	pkScript, err := addresshelper.PkScript(destination.Address)
	if err != nil {
		return
	}

	amountInAtom, err := dcrutil.NewAmount(destination.Amount)
	if err != nil {
		return
	}

	output = &wire.TxOut{
		Value:    int64(amountInAtom),
		Version:  txscript.DefaultScriptVersion,
		PkScript: pkScript,
	}
	return
}
