/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.extensions

import android.content.Context
import com.dcrandroid.data.Account
import com.dcrandroid.data.Constants
import com.dcrandroid.util.PreferenceUtil
import dcrlibwallet.LibWallet

fun LibWallet.walletAccounts(requiredConfirmations: Int) : ArrayList<Account>{
    return Account.parse(this.getAccounts(requiredConfirmations))
}

fun LibWallet.visibleWalletAccounts(requiredConfirmations: Int, context: Context) : List<Account>{
    val util = PreferenceUtil(context)
    return this.walletAccounts(requiredConfirmations).filter { !util.getBoolean(Constants.HIDE_WALLET + it.accountNumber) }
}

fun LibWallet.totalWalletBalance(requiredConfirmations: Int, context: Context): Long{
    val visibleAccounts = this.visibleWalletAccounts(requiredConfirmations, context)

   return visibleAccounts.map { it.balance.total }.reduce { sum, element -> sum + element}
}
