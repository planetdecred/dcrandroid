package utils

import (
	"strings"

	"github.com/decred/dcrwallet/netparams"
)

func NetParams(netType string) *netparams.Params {
	switch strings.ToLower(netType) {
	case strings.ToLower(netparams.MainNetParams.Name):
		return &netparams.MainNetParams
	case strings.ToLower(netparams.TestNet3Params.Name):
		return &netparams.TestNet3Params
	default:
		return nil
	}
}
