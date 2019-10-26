/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.extensions

import android.content.Context
import com.dcrandroid.data.Account
import com.dcrandroid.data.Constants
import com.dcrandroid.data.parseAccounts
import com.dcrandroid.util.PreferenceUtil
import com.dcrandroid.util.WalletData
import dcrlibwallet.Dcrlibwallet
import dcrlibwallet.LibWallet

fun LibWallet.walletAccounts(requiredConfirmations: Int) : ArrayList<Account>{
    return parseAccounts(this.getAccounts(requiredConfirmations)).accounts
}

fun LibWallet.visibleWalletAccounts(context: Context) : List<Account>{

    val util = PreferenceUtil(context)

    val requiredConfirmations= when {
        WalletData.multiWallet!!.readBoolConfigValueForKey(Dcrlibwallet.SpendUnconfirmedConfigKey, Constants.DEF_SPEND_UNCONFIRMED) -> 0
        else -> Constants.REQUIRED_CONFIRMATIONS
    }

    return this.walletAccounts(requiredConfirmations).filter { !util.getBoolean(Constants.HIDE_WALLET + it.accountNumber) }
}

fun LibWallet.totalWalletBalance(context: Context): Long{
    val visibleAccounts = this.visibleWalletAccounts(context)

   return visibleAccounts.map { it.balance.total }.reduce { sum, element -> sum + element}
}
