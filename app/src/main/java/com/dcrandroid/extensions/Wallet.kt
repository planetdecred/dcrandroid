/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.extensions

import com.dcrandroid.data.Account
import com.dcrandroid.data.Constants
import com.dcrandroid.data.parseAccounts
import com.dcrandroid.util.WalletData
import dcrlibwallet.Dcrlibwallet
import dcrlibwallet.Wallet

fun Wallet.walletAccounts(): ArrayList<Account> {

    val requiredConfirmations = when {
        WalletData.multiWallet!!.readBoolConfigValueForKey(Dcrlibwallet.SpendUnconfirmedConfigKey, Constants.DEF_SPEND_UNCONFIRMED) -> 0
        else -> Constants.REQUIRED_CONFIRMATIONS
    }
    return parseAccounts(this.accounts).accounts
}

fun Wallet.totalWalletBalance(): Long {
    val visibleAccounts = this.walletAccounts()

    return visibleAccounts.map { it.balance.total }.reduce { sum, element -> sum + element }
}
