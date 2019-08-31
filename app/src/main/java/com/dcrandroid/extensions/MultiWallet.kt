/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.extensions

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dcrlibwallet.LibWallet
import dcrlibwallet.MultiWallet

fun MultiWallet.openedWalletsList(): ArrayList<LibWallet> {
    val wallets = ArrayList<LibWallet>()

    val openedWalletsJson = this.openedWallets()
    val gson = Gson()
    val listType = object : TypeToken<ArrayList<Long>>() {}.type
    val openedWalletsTemp = gson.fromJson<ArrayList<Long>>(openedWalletsJson, listType)

    for(walletId in openedWalletsTemp){
        val wallet = this.getWallet(walletId)
        wallets.add(wallet)
    }

    return  wallets
}