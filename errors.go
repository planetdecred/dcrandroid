package dcrlibwallet

import "github.com/decred/dcrwallet/errors/v2"

const (
	// Error Codes
	ErrInsufficientBalance          = "insufficient_balance"
	ErrInvalid                      = "invalid"
	ErrWalletDatabaseInUse          = "wallet_db_in_use"
	ErrWalletNotLoaded              = "wallet_not_loaded"
	ErrWalletNameExist              = "wallet_name_exists"
	ErrReservedWalletName           = "wallet_name_reserved"
	ErrWalletIsRestored             = "wallet_is_restored"
	ErrWalletIsWatchOnly            = "watch_only_wallet"
	ErrUnusableSeed                 = "unusable_seed"
	ErrPassphraseRequired           = "passphrase_required"
	ErrInvalidPassphrase            = "invalid_passphrase"
	ErrNotConnected                 = "not_connected"
	ErrExist                        = "exists"
	ErrNotExist                     = "not_exists"
	ErrEmptySeed                    = "empty_seed"
	ErrInvalidAddress               = "invalid_address"
	ErrInvalidAuth                  = "invalid_auth"
	ErrUnavailable                  = "unavailable"
	ErrContextCanceled              = "context_canceled"
	ErrFailedPrecondition           = "failed_precondition"
	ErrSyncAlreadyInProgress        = "sync_already_in_progress"
	ErrNoPeers                      = "no_peers"
	ErrInvalidPeers                 = "invalid_peers"
	ErrListenerAlreadyExist         = "listener_already_exist"
	ErrLoggerAlreadyRegistered      = "logger_already_registered"
	ErrLogRotatorAlreadyInitialized = "log_rotator_already_initialized"
	ErrAddressDiscoveryNotDone      = "address_discovery_not_done"
)

// todo, should update this method to translate more error kinds.
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
