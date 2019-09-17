package txhelper

import (
	"github.com/decred/dcrd/txscript"
	"github.com/decred/dcrd/wire"
	"github.com/raedahgroup/dcrlibwallet/addresshelper"
)

func MakeTxOutput(address string, amountInAtom int64) (output *wire.TxOut, err error) {
	pkScript, err := addresshelper.PkScript(address)
	if err != nil {
		return
	}

	output = &wire.TxOut{
		Value:    amountInAtom,
		Version:  txscript.DefaultScriptVersion,
		PkScript: pkScript,
	}
	return
}
