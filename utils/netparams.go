package utils

import (
	"strings"

	"github.com/decred/dcrd/chaincfg/v2"
)

var (
	mainnetParams = chaincfg.MainNetParams()
	testnetParams = chaincfg.TestNet3Params()
)

func ChainParams(netType string) *chaincfg.Params {
	switch strings.ToLower(netType) {
	case strings.ToLower(mainnetParams.Name):
		return mainnetParams
	case strings.ToLower(testnetParams.Name):
		return testnetParams
	default:
		return nil
	}
}
