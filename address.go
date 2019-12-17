package dcrlibwallet

import (
	"encoding/hex"
	"fmt"

	"github.com/decred/dcrd/dcrutil/v2"
	w "github.com/decred/dcrwallet/wallet/v3"
	"github.com/decred/dcrwallet/wallet/v3/udb"
)

// AddressInfo holds information about an address
// If the address belongs to the querying wallet, IsMine will be true and the AccountNumber and AccountName values will be populated
type AddressInfo struct {
	Address       string
	IsMine        bool
	AccountNumber uint32
	AccountName   string
}

func (wallet *Wallet) IsAddressValid(address string) bool {
	_, err := dcrutil.DecodeAddress(address, wallet.chainParams)
	return err == nil
}

func (wallet *Wallet) HaveAddress(address string) bool {
	addr, err := dcrutil.DecodeAddress(address, wallet.chainParams)
	if err != nil {
		return false
	}

	have, err := wallet.internal.HaveAddress(wallet.shutdownContext(), addr)
	if err != nil {
		return false
	}

	return have
}

func (wallet *Wallet) AccountOfAddress(address string) string {
	addr, err := dcrutil.DecodeAddress(address, wallet.chainParams)
	if err != nil {
		return err.Error()
	}

	info, _ := wallet.internal.AddressInfo(wallet.shutdownContext(), addr)
	return wallet.AccountName(int32(info.Account()))
}

func (wallet *Wallet) AddressInfo(address string) (*AddressInfo, error) {
	addr, err := dcrutil.DecodeAddress(address, wallet.chainParams)
	if err != nil {
		log.Error(err)
		return nil, err
	}

	addressInfo := &AddressInfo{
		Address: address,
	}

	info, _ := wallet.internal.AddressInfo(wallet.shutdownContext(), addr)
	if info != nil {
		addressInfo.IsMine = true
		addressInfo.AccountNumber = info.Account()
		addressInfo.AccountName = wallet.AccountName(int32(info.Account()))
	}

	return addressInfo, nil
}

func (wallet *Wallet) CurrentAddress(account int32) (string, error) {
	addr, err := wallet.internal.CurrentAddress(uint32(account))
	if err != nil {
		log.Error(err)
		return "", err
	}
	return addr.Address(), nil
}

func (wallet *Wallet) NextAddress(account int32) (string, error) {
	addr, err := wallet.internal.NewExternalAddress(wallet.shutdownContext(), uint32(account), w.WithGapPolicyWrap())
	if err != nil {
		log.Error(err)
		return "", err
	}
	return addr.Address(), nil
}

func (wallet *Wallet) AddressPubKey(address string) (string, error) {
	addr, err := dcrutil.DecodeAddress(address, wallet.chainParams)
	if err != nil {
		return "", err
	}

	ainfo, err := wallet.internal.AddressInfo(wallet.shutdownContext(), addr)
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
		pubKeyAddr, err := dcrutil.NewAddressSecpPubKey(pubKeyBytes, wallet.chainParams)
		if err != nil {
			return "", err
		}
		return pubKeyAddr.String(), nil

	default:
		return "", fmt.Errorf("address is not a managed pub key address")
	}
}
