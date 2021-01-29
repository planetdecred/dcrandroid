/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.extensions

import com.dcrandroid.data.Account
import com.dcrandroid.data.parseAccounts
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dcrlibwallet.MultiWallet
import dcrlibwallet.Wallet

fun MultiWallet.openedWalletsList(): ArrayList<Wallet> {
    val wallets = ArrayList<Wallet>()

    val openedWalletsJson = this.openedWalletIDs()
    val gson = Gson()
    val listType = object : TypeToken<ArrayList<Long>>() {}.type
    val openedWalletsTemp = gson.fromJson<ArrayList<Long>>(openedWalletsJson, listType)

    for (walletId in openedWalletsTemp) {
        val wallet = this.walletWithID(walletId)
        wallets.add(wallet)
    }

    wallets.sortBy { it.id }

    return wallets
}

fun MultiWallet.firstWalletWithAValidAccount(filterAccount: (account: Account) -> Boolean): Wallet? {
    val wallets = this.openedWalletsList()

    for (wallet in wallets) {
        val accounts = parseAccounts(wallet.accounts).accounts.filter { filterAccount(it) }
        if (accounts.size > 0) {
            return wallet
        }
    }

    return null
}

fun MultiWallet.fullCoinWalletsList(): ArrayList<Wallet> {
    return this.openedWalletsList().filter { !it.isWatchingOnlyWallet } as ArrayList<Wallet>
}

fun MultiWallet.watchOnlyWalletsList(): ArrayList<Wallet> {
    return this.openedWalletsList().filter { it.isWatchingOnlyWallet } as ArrayList<Wallet>
}

fun MultiWallet.totalWalletBalance(): Long {
    val wallets = this.openedWalletsList()
    var totalBalance: Long = 0

    for (wallet in wallets) {
        totalBalance += wallet.totalWalletBalance()
    }

    return totalBalance
}
