package com.dcrandroid.data

import java.io.Serializable

class TransactionResponse {

    var minedTransactions: ArrayList<Transaction> = ArrayList()
    var unminedTransactions: ArrayList<Transaction> = ArrayList()

    class Transaction : Serializable {
        var hash: String = ""
        var type: String = ""
        var raw: String = ""
        var height: Int = 0
        var direction: Int = 0

        var fee: Long = 0
        var amount: Long = 0
        var totalInput: Long = 0
        var totalOutput: Long = 0
        var timestamp: Long = 0

        @Transient
        var animate = false

        var outputs: ArrayList<TransactionOutput> = ArrayList()
        var inputs: ArrayList<TransactionInput>? = ArrayList()

        class TransactionInput : Serializable {
            var previousAccount: Long = 0
            var index: Int = 0
            var previousAmount: Long = 0
            var accountName: String? = null
        }

        class TransactionOutput : Serializable {
            var index: Int = 0
            var account: Int = 0
            var amount: Long = 0
            var internal: Boolean = false
            var address: String? = null
        }
    }
}