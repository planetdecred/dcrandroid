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
            PEER_IP = "peer_ip",
            SPEND_UNCONFIRMED_FUNDS = "spend_unconfirmed_funds",
            TRANSACTION_NOTIFICATION_GROUP = "com.dcrandroid.NEW_TRANSACTIONS",
            ADDRESS = "Address",
            LOGGING_LEVEL = "logging_level",
            NEW_TRANSACTION_NOTIFICATION = "new_transaction_notification",
            NBSP = " ",
            PASSPHRASE = "passphrase",
            HIDE_WALLET = "hide_wallet",
            EXTERNAL = "external",
            SYNCED = "synced",
            RESTORE_WALLET = "restore_wallet",
            BADGER_DB = "badgerdb",
            TESTNET_HD_PATH = "m / 44' / 1' / ",
            LEGACY_TESTNET_HD_PATH = "m / 44' / 11' / ",
            MAINNET_HD_PATH = "m / 44' / 42' / ",
            LEGACY_MAINNET_HD_PATH = "m / 44' / 20' / ",
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
            OFF = "off",
            FINGERPRINT = "fingerprint",
            FINGERPRINT_PASS = "fingerprint_pass";

    public static final int TRANSACTION_SUMMARY_ID = 5552478,
            REQUIRED_CONFIRMATIONS = 2,
            DEFAULT_ACCOUNT_NUMBER = 0,
            DEFAULT_TX_NOTIFICATION = 2, // Vibrations Only
            DEFAULT_CURRENCY_CONVERSION = 0; // None

}
