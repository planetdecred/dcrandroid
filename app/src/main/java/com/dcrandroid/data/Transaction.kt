/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.data

import com.dcrandroid.util.Utils
import com.dcrandroid.util.WalletData
import com.google.gson.annotations.SerializedName
import dcrlibwallet.Dcrlibwallet
import java.io.Serializable
import java.math.BigDecimal

class Transaction : Serializable {
    @SerializedName("walletID")
    var walletID: Long = 0
    @SerializedName("hash")
    var hash: String = ""
    @SerializedName("type")
    var type: String = ""
    @SerializedName("raw")
    var raw: String = ""
    @SerializedName("block_height")
    var height: Int = 0
    @SerializedName("direction")
    var direction: Int = 0
    @SerializedName("fee")
    var fee: Long = 0
    @SerializedName("amount")
    var amount: Long = 0
    var totalInput: Long = 0
    var totalOutput: Long = 0
    @SerializedName("timestamp")
    var timestamp: Long = 0
    val confirmations: Int
    get() {
        return if(height == Dcrlibwallet.BlockHeightInvalid){
            0
        }else{
            (WalletData.multiWallet!!.bestBlock.height - height) + 1
        }
    }

    val hashBytes: ByteArray
    get() = Utils.getHash(hash)

    val walletName: String?
    get(){
        return WalletData.multiWallet!!.getWallet(walletID)?.walletName
    }

    @Transient
    var animate = false

    @SerializedName("credits")
    var outputs: ArrayList<TransactionOutput>? = null
    @SerializedName("debits")
    var inputs: ArrayList<TransactionInput>? = null

    class TransactionInput : Serializable {
        @SerializedName("previous_account")
        var previousAccount: Long = 0
        @SerializedName("index")
        var index: Int = 0
        @SerializedName("amount")
        var amount: Long = 0
        @SerializedName("account_name")
        var accountName: String? = null
    }

    class TransactionOutput : Serializable {
        @SerializedName("index")
        var index: Int = 0
        @SerializedName("previous_account")
        var account: Int = 0
        @SerializedName("account_name")
        var amount: Long = 0
        @SerializedName("internal")
        var internal: Boolean = false
        @SerializedName("address")
        var address: String? = null
    }
}

class TransactionData {
    lateinit var dcrAmount: BigDecimal
    var exchangeDecimal: BigDecimal? = null

    var sendMax = false

    lateinit var sourceAccount: Account

    var destinationAccount: Account? = null
    lateinit var destinationAddress: String
}