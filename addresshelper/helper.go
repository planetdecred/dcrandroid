package addresshelper

import (
	"fmt"

	chaincfg "github.com/decred/dcrd/chaincfg/v2"
	dcrutil "github.com/decred/dcrd/dcrutil/v2"
	txscript "github.com/decred/dcrd/txscript/v2"
)

const scriptVersion = 0

func PkScript(address string, net dcrutil.AddressParams) ([]byte, error) {
	addr, err := dcrutil.DecodeAddress(address, net)
	if err != nil {
		return nil, fmt.Errorf("error decoding address '%s': %s", address, err.Error())
	}

	return txscript.PayToAddrScript(addr)
}

func PkScriptAddresses(params *chaincfg.Params, pkScript []byte) ([]string, error) {
	_, addresses, _, err := txscript.ExtractPkScriptAddrs(scriptVersion, pkScript, params)
	if err != nil {
		return nil, err
	}

	encodedAddresses := make([]string, len(addresses))
	for i, address := range addresses {
		encodedAddresses[i] = address.Address()
	}

	return encodedAddresses, nil
}
