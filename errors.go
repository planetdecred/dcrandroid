package dcrlibwallet

import "github.com/decred/dcrwallet/errors"

const (
	// Error Codes
	ErrInsufficientBalance   = "insufficient_balance"
	ErrInvalid               = "invalid"
	ErrWalletNotLoaded       = "wallet_not_loaded"
	ErrPassphraseRequired    = "passphrase_required"
	ErrInvalidPassphrase     = "invalid_passphrase"
	ErrNotConnected          = "not_connected"
	ErrNotExist              = "not_exists"
	ErrEmptySeed             = "empty_seed"
	ErrInvalidAddress        = "invalid_address"
	ErrInvalidAuth           = "invalid_auth"
	ErrUnavailable           = "unavailable"
	ErrContextCanceled       = "context_canceled"
	ErrFailedPrecondition    = "failed_precondition"
	ErrSyncAlreadyInProgress = "sync_already_in_progress"
	ErrNoPeers               = "no_peers"
	ErrInvalidPeers          = "invalid_peers"
	ErrListenerAlreadyExist  = "listener_already_exist"
)

func translateError(err error) error {
	if err, ok := err.(*errors.Error); ok {
		switch err.Kind {
		case errors.InsufficientBalance:
			return errors.New(ErrInsufficientBalance)
		case errors.NotExist:
			return errors.New(ErrNotExist)
		case errors.Passphrase:
			return errors.New(ErrInvalidPassphrase)
		case errors.NoPeers:
			return errors.New(ErrNoPeers)
		}
	}
	return err
}
