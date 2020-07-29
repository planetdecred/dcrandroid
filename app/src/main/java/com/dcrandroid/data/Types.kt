/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.data

import com.dcrandroid.util.WalletData
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.io.Serializable

class Accounts : Serializable {

    @SerializedName("CurrentBlockHash")
    lateinit var currentBlockHash: String // base64

    @SerializedName("CurrentBlockHeight")
    var currentBlockHeight: Int = 0

    @SerializedName("Acc")
    lateinit var accounts: ArrayList<Account>
}

fun parseAccounts(json: String): Accounts {
    val gson = Gson()
    return gson.fromJson(json, Accounts::class.java)
}

class Account : Serializable {
    @SerializedName("WalletID")
    var walletID: Long = 0

    @SerializedName("Number")
    var accountNumber: Int = 0

    @SerializedName("Name")
    lateinit var accountName: String

    @SerializedName("Balance")
    lateinit var balance: Balance

    @SerializedName("TotalBalance")
    var totalBalance: Long = 0

    @SerializedName("ExternalKeyCount")
    var externalKeyCount: Int = 0

    @SerializedName("InternalKeyCount")
    var internalKeyCount: Int = 0

    @SerializedName("ImportedKeyCount")
    var importedKeyCount: Int = 0

    val hdPath: String
        get() = WalletData.multiWallet!!.walletWithID(walletID).hdPathForAccount(accountNumber)

    companion object {
        fun from(acc: dcrlibwallet.Account): Account {
            val account = Account()
            return account.apply {
                walletID = acc.walletID
                accountNumber = acc.number
                accountName = acc.name
                balance = Balance.from(acc.balance)
                totalBalance = acc.totalBalance
                externalKeyCount = acc.externalKeyCount
                internalKeyCount = acc.internalKeyCount
                importedKeyCount = acc.importedKeyCount
            }
        }
    }
}

fun parseAccountArray(json: String): ArrayList<Account> {
    val gson = Gson()
    val listType = object : TypeToken<ArrayList<Account>>() {}.type
    return gson.fromJson(json, listType)
}

class Balance : Serializable {
    @SerializedName("Spendable")
    var spendable: Long = 0

    @SerializedName("Total")
    var total: Long = 0

    @SerializedName("ImmatureReward")
    var immatureReward: Long = 0

    @SerializedName("ImmatureStakeGeneration")
    var immatureStakeGeneration: Long = 0

    @SerializedName("LockedByTickets")
    var lockedByTickets: Long = 0

    @SerializedName("VotingAuthority")
    var votingAuthority: Long = 0

    @SerializedName("UnConfirmed")
    var unConfirmed: Long = 0

    companion object {
        fun from(bal: dcrlibwallet.Balance): Balance {
            val balance = Balance()
            return balance.apply {
                spendable = bal.spendable
                total = bal.total
                immatureReward = bal.immatureReward
                immatureStakeGeneration = bal.immatureStakeGeneration
                lockedByTickets = bal.lockedByTickets
                votingAuthority = bal.votingAuthority
                unConfirmed = bal.unConfirmed
            }
        }
    }
}

fun parseBalanceArray(json: String): ArrayList<Balance> {
    val gson = Gson()
    val listType = object : TypeToken<ArrayList<Balance>>() {}.type
    return gson.fromJson(json, listType)
}

class DecredAddressURI {

    var address: String = ""
    var amount: Double? = null

    companion object {
        fun from(uriString: String): DecredAddressURI {

            val addressURI = DecredAddressURI()

            var address: String = uriString
            var amount: Double? = null

            val schemeSeparatedParts = uriString.split(":")
            if (schemeSeparatedParts.size == 2 && schemeSeparatedParts[0] == "decred") {

                val addressAndQuery = schemeSeparatedParts[1].split("?")

                address = addressAndQuery[0]
                if (addressAndQuery.size >= 2) {

                    // get amount from query
                    val queryParameters = addressAndQuery[1].split("&")
                    for (query in queryParameters) {
                        val nameValuePair = query.split("=")
                        val queryName = nameValuePair[0]
                        if (nameValuePair.size == 2 && queryName == Constants.AMOUNT) {
                            val amountStr = nameValuePair[1]
                            if (amountStr.trim().isNotEmpty()) {
                                try {
                                    amount = amountStr.toDouble()
                                } catch (e: NumberFormatException) {
                                    e.printStackTrace()
                                }
                            }

                            break
                        }
                    }
                }
            }

            addressURI.address = address
            addressURI.amount = amount

            return addressURI
        }
    }
}

class PeerInfo {
    @SerializedName("id")
    var id: Int = 0

    @SerializedName("addr")
    lateinit var addr: String

    @SerializedName("addr_local")
    lateinit var addrLocal: String

    @SerializedName("services")
    lateinit var services: String

    @SerializedName("version")
    var version: Int = 0

    @SerializedName("sub_ver")
    lateinit var subVer: String

    @SerializedName("starting_height")
    var startingHeight: Long = 0

    @SerializedName("ban_score")
    var banScore: Int = 0
}