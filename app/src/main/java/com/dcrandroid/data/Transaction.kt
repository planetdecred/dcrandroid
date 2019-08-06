/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.data

import com.dcrandroid.util.WalletData
import com.google.gson.annotations.SerializedName
import java.io.Serializable

class Transaction : Serializable {
    @SerializedName("Hash")
    var hash: String = ""
    @SerializedName("Type")
    var type: String = ""
    @SerializedName("Raw")
    var raw: String = ""
    @SerializedName("Height")
    var height: Int = 0
    @SerializedName("Direction")
    var direction: Int = 0
    @SerializedName("Fee")
    var fee: Long = 0
    @SerializedName("Amount")
    var amount: Long = 0
    var totalInput: Long = 0
    var totalOutput: Long = 0
    @SerializedName("Timestamp")
    var timestamp: Long = 0
    val confirmations: Int
    get() {
        return if(height == 0){
            0
        }else{
            (WalletData.getInstance().wallet.bestBlock - height) + 1
        }
    }

    @Transient
    var animate = false

    @SerializedName("Credits")
    var outputs: ArrayList<TransactionOutput>? = null
    @SerializedName("Debits")
    var inputs: ArrayList<TransactionInput>? = null

    class TransactionInput : Serializable {
        @SerializedName("PreviousAccount")
        var previousAccount: Long = 0
        @SerializedName("Index")
        var index: Int = 0
        @SerializedName("PreviousAmount")
        var previousAmount: Long = 0
        @SerializedName("AccountName")
        var accountName: String? = null
    }

    class TransactionOutput : Serializable {
        @SerializedName("Index")
        var index: Int = 0
        @SerializedName("Account")
        var account: Int = 0
        @SerializedName("Amount")
        var amount: Long = 0
        @SerializedName("Internal")
        var internal: Boolean = false
        @SerializedName("Address")
        var address: String? = null
    }
}