package utils

import (
	"strings"

	"github.com/decred/dcrd/chaincfg/v2"
	"github.com/decred/dcrwallet/errors"
)

var (
	mainnetParams = chaincfg.MainNetParams()
	testnetParams = chaincfg.TestNet3Params()
)

func ChainParams(netType string) (*chaincfg.Params, error) {
	switch strings.ToLower(netType) {
	case strings.ToLower(mainnetParams.Name):
		return mainnetParams, nil
	case strings.ToLower(testnetParams.Name):
		return testnetParams, nil
	default:
		return nil, errors.New("invalid net type")
	}
}
