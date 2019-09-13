package dcrlibwallet

import (
	"encoding/hex"
	"fmt"

	"github.com/decred/dcrd/dcrutil"
	"github.com/decred/dcrwallet/wallet"
	"github.com/decred/dcrwallet/wallet/udb"
	"github.com/raedahgroup/dcrlibwallet/addresshelper"
)

// AddressInfo holds information about an address
// If the address belongs to the querying wallet, IsMine will be true and the AccountNumber and AccountName values will be populated
type AddressInfo struct {
	Address       string
	IsMine        bool
	AccountNumber uint32
	AccountName   string
}

func (lw *LibWallet) IsAddressValid(address string) bool {
	_, err := addresshelper.DecodeForNetwork(address, lw.activeNet.Params)
	return err == nil
}

func (lw *LibWallet) HaveAddress(address string) bool {
	addr, err := addresshelper.DecodeForNetwork(address, lw.activeNet.Params)
	if err != nil {
		return false
	}
	have, err := lw.wallet.HaveAddress(addr)
	if err != nil {
		return false
	}

	return have
}

func (lw *LibWallet) AccountOfAddress(address string) string {
	addr, err := dcrutil.DecodeAddress(address)
	if err != nil {
		return err.Error()
	}
	info, _ := lw.wallet.AddressInfo(addr)
	return lw.AccountName(int32(info.Account()))
}

func (lw *LibWallet) AddressInfo(address string) (*AddressInfo, error) {
	addr, err := dcrutil.DecodeAddress(address)
	if err != nil {
		log.Error(err)
		return nil, err
	}

	addressInfo := &AddressInfo{
		Address: address,
	}

	info, _ := lw.wallet.AddressInfo(addr)
	if info != nil {
		addressInfo.IsMine = true
		addressInfo.AccountNumber = info.Account()
		addressInfo.AccountName = lw.AccountName(int32(info.Account()))
	}

	return addressInfo, nil
}

func (lw *LibWallet) CurrentAddress(account int32) (string, error) {
	addr, err := lw.wallet.CurrentAddress(uint32(account))
	if err != nil {
		log.Error(err)
		return "", err
	}
	return addr.EncodeAddress(), nil
}

func (lw *LibWallet) NextAddress(account int32) (string, error) {
	var callOpts []wallet.NextAddressCallOption
	callOpts = append(callOpts, wallet.WithGapPolicyWrap())

	addr, err := lw.wallet.NewExternalAddress(uint32(account), callOpts...)
	if err != nil {
		log.Error(err)
		return "", err
	}
	return addr.EncodeAddress(), nil
}

func (lw *LibWallet) AddressPubKey(address string) (string, error) {
	addr, err := addresshelper.DecodeForNetwork(address, lw.activeNet.Params)
	if err != nil {
		return "", err
	}
	ainfo, err := lw.wallet.AddressInfo(addr)
	if err != nil {
		return "", err
	}
	switch ma := ainfo.(type) {
	case udb.ManagedPubKeyAddress:
		pubKey := ma.ExportPubKey()
		pubKeyBytes, err := hex.DecodeString(pubKey)
		if err != nil {
			return "", err
		}
		pubKeyAddr, err := dcrutil.NewAddressSecpPubKey(pubKeyBytes, lw.activeNet.Params)
		if err != nil {
			return "", err
		}
		return pubKeyAddr.String(), nil

	default:
		return "", fmt.Errorf("address is not a managed pub key address")
	}
}
