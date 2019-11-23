/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.data;

/**
 * Created by afomic on 2/28/18.
 */

public class Constants {
    public static final String
            TRANSACTION_NOTIFICATION_GROUP = "com.dcrandroid.NEW_TRANSACTIONS",
            ADDRESS = "Address",
            NEW_TRANSACTION_NOTIFICATION = "new_transaction_notification",
            NBSP = " ",
            PASSPHRASE = "passphrase",
            HIDE_WALLET = "hide_wallet",
            EXTERNAL = "external",
            SYNCED = "synced",
            BADGER_DB = "badgerdb",
            TESTNET_HD_PATH = "m / 44’ / 1’ / ",
            LEGACY_TESTNET_HD_PATH = "m / 44’ / 11’ / ",
            MAINNET_HD_PATH = "m / 44’ / 42’ / ",
            LEGACY_MAINNET_HD_PATH = "m / 44’ / 20’ / ",
            USER_AGENT = "user_agent",
            SPENDING_PASSPHRASE_TYPE = "spending_passphrase_type",
            STARTUP_PASSPHRASE_TYPE = "encrypt_passphrase_type",
            PIN = "pin",
            PASSWORD = "password",
            ENCRYPT = "encrypt",
            INSECURE_PUB_PASSPHRASE = "public",
            APP_VERSION = "app_version",
            WIFI_SYNC = "wifi_sync",
            LICENSE = "license",
            WALLET_ID = "wallet_id",
            RESULT = "result",
            ANDROID_KEY_STORE = "AndroidKeyStore",
            TRANSFORMATION = "AES/GCM/NoPadding",
            ENCRYPTION_DATA = "encryption_data",
            ENCRYPTION_IV = "encryption_iv",
            USE_BIOMETRIC_AUTH = "use_biometric_auth",
            OFF = "off",
            FINGERPRINT = "fingerprint",
            FINGERPRINT_PASS = "fingerprint_pass",
            TRANSACTION_CHANNEL_ID = "new transaction";

    public static final int TRANSACTION_SUMMARY_ID = 5552478,
            REQUIRED_CONFIRMATIONS = 2,
            DEF_ACCOUNT_NUMBER = 0,
            DEF_LOG_LEVEL = 2,
            TX_NOTIFICATION_NONE = 0,
            TX_NOTIFICATION_SILENT = 1,
            TX_NOTIFICATION_VIBRATE_ONLY = 2,
            TX_NOTIFICATION_SOUND_ONLY = 3,
            TX_NOTIFICATION_SOUND_VIBRATE = 4,
            DEF_TX_NOTIFICATION = TX_NOTIFICATION_VIBRATE_ONLY,
            DEF_CURRENCY_CONVERSION = 0; // None


    public static final boolean DEF_SPEND_UNCONFIRMED = false,
                                DEF_STARTUP_SECURITY_SET = false,
                                DEF_SYNC_ON_CELLULAR = false;

}
