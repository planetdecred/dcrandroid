/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.data

import com.dcrandroid.BuildConfig
import com.dcrandroid.R
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

    @SerializedName("timestamp")
    var timestamp: Long = 0
    val confirmations: Int
        get() {
            return if (height == Dcrlibwallet.BlockHeightInvalid) {
                0
            } else {
                (WalletData.multiWallet!!.bestBlock.height - height) + 1
            }
        }

    fun getConfirmationIconRes(spendUnconfirmedFunds: Boolean): Int {
        return if (confirmations > 1 || spendUnconfirmedFunds)
            R.drawable.ic_confirmed
        else R.drawable.ic_pending
    }

    val hashBytes: ByteArray
        get() = Utils.getHash(hash)!!

    val walletName: String?
        get() {
            return WalletData.multiWallet!!.walletWithID(walletID)?.name
        }

    val timestampMillis: Long
        get() = timestamp * 1000

    val iconResource: Int
        get() {
            var res = when (direction) {
                Dcrlibwallet.TxDirectionSent -> R.drawable.ic_send
                Dcrlibwallet.TxDirectionReceived -> R.drawable.ic_receive
                else -> R.drawable.ic_wallet
            }

            // replace icon for staking tx types
            if (Dcrlibwallet.txMatchesFilter(type, direction, Dcrlibwallet.TxFilterStaking)) {

                res = when (type) {
                    Dcrlibwallet.TxTypeTicketPurchase -> {
                        if (confirmations < BuildConfig.TicketMaturity) {
                            R.drawable.ic_ticket_immature
                        } else {
                            R.drawable.ic_ticket_live
                        }
                    }
                    Dcrlibwallet.TxTypeVote -> R.drawable.ic_ticket_voted
                    else -> R.drawable.ic_ticket_revoked
                }

            }

            return res
        }

    @Transient
    var animate = false

    @SerializedName("outputs")
    var outputs: Array<TransactionOutput>? = null
    @SerializedName("inputs")
    var inputs: Array<TransactionInput>? = null

    class TransactionInput : Serializable {
        @SerializedName("previous_transaction_index")
        var index: Int = 0
        @SerializedName("amount")
        var amount: Long = 0
        @SerializedName("account_name")
        var accountName: String? = null
        @SerializedName("account_number")
        var accountNumber: Int? = null
        @SerializedName("previous_outpoint")
        var previousOutpoint: String? = null
    }

    class TransactionOutput : Serializable {
        @SerializedName("index")
        var index: Int = 0
        @SerializedName("account_number")
        var account: Int = 0
        @SerializedName("account_name")
        var accountName: String? = null
        @SerializedName("amount")
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