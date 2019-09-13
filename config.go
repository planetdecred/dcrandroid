package dcrlibwallet

import (
	"github.com/asdine/storm"
)

const (
	userConfigDbFilename = "config.db"
	userConfigBucketName = "user_config"

	AppDataDirConfigKey = "app_data_dir"
	LogLevelConfigKey   = "log_level"

	NewWalletSetUpConfigKey                 = "new_wallet_set_up"
	InitialSyncCompletedConfigKey           = "initial_sync_complete"
	IsStartupSecuritySetConfigKey           = "startup_security_set"
	StartupSecurityTypeConfigKey            = "startup_security_type"
	SpendingPassphraseSecurityTypeConfigKey = "spending_security_type"
	DefaultWalletConfigKey                  = "default_wallet"
	HiddenWalletPrefixConfigKey             = "hidden"

	NetworkModeConfigKey                = "network_mode"
	SpvPersistentPeerAddressesConfigKey = "spv_peer_addresses"
	RemoteServerIPConfigKey             = "remote_server_ip"
	SyncOnCellularConfigKey             = "always_sync"

	SpendUnconfirmedConfigKey = "spend_unconfirmed"
	NotifyOnNewTxConfigKey    = "tx_notification_enabled"
	LastTxHashConfigKey       = "last_tx_hash"

	// todo should use this config value to implement cross-platform currency conversion feature
	CurrencyConversionConfigKey = "currency_conversion_option"

	VSPHostConfigKey = "vsp_host"
)

func (lw *LibWallet) SaveUserConfigValue(key string, value interface{}) error {
	return lw.configDB.Set(userConfigBucketName, key, value)
}

func (lw *LibWallet) ReadUserConfigValue(key string, valueOut interface{}) error {
	err := lw.configDB.Get(userConfigBucketName, key, valueOut)
	if err != nil && err != storm.ErrNotFound {
		return err
	}
	return nil
}

func (lw *LibWallet) SetBoolConfigValueForKey(key string, value bool) {
	err := lw.SaveUserConfigValue(key, value)
	if err != nil {
		log.Errorf("error setting config value: %v", err)
	}
}

func (lw *LibWallet) SetDoubleConfigValueForKey(key string, value float64) {
	err := lw.SaveUserConfigValue(key, value)
	if err != nil {
		log.Errorf("error setting config value: %v", err)
	}
}

func (lw *LibWallet) SetIntConfigValueForKey(key string, value int) {
	err := lw.SaveUserConfigValue(key, value)
	if err != nil {
		log.Errorf("error setting config value: %v", err)
	}
}

func (lw *LibWallet) SetLongConfigValueForKey(key string, value int64) {
	err := lw.SaveUserConfigValue(key, value)
	if err != nil {
		log.Errorf("error setting config value: %v", err)
	}
}

func (lw *LibWallet) SetStringConfigValueForKey(key, value string) {
	err := lw.SaveUserConfigValue(key, value)
	if err != nil {
		log.Errorf("error setting config value: %v", err)
	}
}

func (lw *LibWallet) ReadBoolConfigValueForKey(key string) (valueOut bool) {
	err := lw.ReadUserConfigValue(key, &valueOut)
	if err != nil {
		log.Errorf("error reading config value: %v", err)
	}
	return
}

func (lw *LibWallet) ReadDoubleConfigValueForKey(key string) (valueOut float64) {
	err := lw.ReadUserConfigValue(key, &valueOut)
	if err != nil {
		log.Errorf("error reading config value: %v", err)
	}
	return
}

func (lw *LibWallet) ReadIntConfigValueForKey(key string) (valueOut int) {
	err := lw.ReadUserConfigValue(key, &valueOut)
	if err != nil {
		log.Errorf("error reading config value: %v", err)
	}
	return
}

func (lw *LibWallet) ReadLongConfigValueForKey(key string) (valueOut int64) {
	err := lw.ReadUserConfigValue(key, &valueOut)
	if err != nil {
		log.Errorf("error reading config value: %v", err)
	}
	return
}

func (lw *LibWallet) ReadStringConfigValueForKey(key string) (valueOut string) {
	err := lw.ReadUserConfigValue(key, &valueOut)
	if err != nil {
		log.Errorf("error reading config value: %v", err)
	}
	return
}
