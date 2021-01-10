/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.extensions

import com.dcrandroid.data.Account
import com.dcrandroid.data.parseAccounts
import com.google.gson.Gson
import dcrlibwallet.Dcrlibwallet
import dcrlibwallet.Wallet

fun Wallet.walletAccounts(): ArrayList<Account> {
    return parseAccounts(this.accounts).accounts
}

fun Wallet.totalWalletBalance(): Long {
    val visibleAccounts = this.walletAccounts()

    return visibleAccounts.map { it.balance.total }.reduce { sum, element -> sum + element }
}

fun Wallet.findCSPPAccounts(): IntArray? {
    val accounts = Gson().fromJson(findLastUsedCSPPAccounts(), IntArray::class.java)

    return if (accounts.size == 2) {
        accounts
    } else null
}

fun Wallet.requiresPrivacySetup(): Boolean {
    return findCSPPAccounts() != null && isRestored && !readBoolConfigValueForKey(Dcrlibwallet.AccountMixerConfigSet, false)
}