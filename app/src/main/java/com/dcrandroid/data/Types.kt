/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.data

import com.dcrandroid.BuildConfig
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
        get() = (if (BuildConfig.IS_TESTNET) Constants.TESTNET_HD_PATH else Constants.MAINNET_HD_PATH) + accountNumber + "'"

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
