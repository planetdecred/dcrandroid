/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.*
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import com.dcrandroid.adapter.TransactionListAdapter
import com.dcrandroid.data.Constants
import com.dcrandroid.data.Transaction
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.show
import com.dcrandroid.extensions.totalWalletBalance
import com.dcrandroid.util.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dcrlibwallet.*
import kotlinx.android.synthetic.main.transactions_overview_layout.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


const val MAX_TRANSACTIONS = 3

class Overview : BaseFragment(), ViewTreeObserver.OnScrollChangedListener {

    private lateinit var util: PreferenceUtil

    private val transactions: ArrayList<Transaction> = ArrayList()
    private var adapter: TransactionListAdapter? = null
    private val gson = GsonBuilder().registerTypeHierarchyAdapter(ArrayList::class.java, Deserializer.TransactionDeserializer())
            .create()

    private lateinit var scrollView: NestedScrollView
    private lateinit var recyclerView: RecyclerView

    private lateinit var balanceTextView: TextView
    internal lateinit var noTransactionsTextView: TextView
    internal lateinit var transactionsLayout: LinearLayout

    private lateinit var syncLayout: LinearLayout
    private var syncLayoutUtil: SyncLayoutUtil? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_overview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        scrollView = view.findViewById(R.id.scroll_view_overview)
        recyclerView = view.findViewById(R.id.rv_transactions)

        balanceTextView = view.findViewById(R.id.tv_visible_wallet_balance)
        noTransactionsTextView = view.findViewById(R.id.tv_no_transactions)
        transactionsLayout = view.findViewById(R.id.transactions_view)

        syncLayout = view.findViewById(R.id.sync_layout)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        util = PreferenceUtil(context!!)

        adapter = TransactionListAdapter(context!!, transactions)

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.isNestedScrollingEnabled = false
        recyclerView.adapter = adapter

        setToolbarTitle(R.string.overview, false)
        scrollView.viewTreeObserver.addOnScrollChangedListener(this)

        balanceTextView.text = CoinFormat.format(multiWallet.totalWalletBalance(context!!))

        loadTransactions()

        btn_view_all_transactions.setOnClickListener {
            switchFragment(1) // Transactions Fragment
        }
    }

    override fun onResume() {
        super.onResume()
        syncLayoutUtil = SyncLayoutUtil(syncLayout, {restartSyncProcess()}, {
            scrollView.postDelayed({
                scrollView.smoothScrollTo(0, scrollView.bottom)
            }, 200)
        })
    }

    override fun onPause() {
        super.onPause()
        syncLayoutUtil?.destroy()
        syncLayoutUtil = null
    }

    override fun onScrollChanged() {
        val scrollY = scrollView.scrollY
        val textSize = context?.resources?.getDimension(R.dimen.visible_balance_text_size)

        if (textSize != null) {
            if (scrollY > textSize / 2) {
                setToolbarTitle(balanceTextView.text, true)
            } else {
                setToolbarTitle(R.string.overview, false)
            }
        }
    }

    private fun loadTransactions() = GlobalScope.launch(Dispatchers.Default) {
        val jsonResult = multiWallet.getTransactions(0, MAX_TRANSACTIONS, Dcrlibwallet.TxFilterRegular, true)
        var tempTxList = gson.fromJson(jsonResult, Array<Transaction>::class.java)

        if (tempTxList == null) {
            tempTxList = arrayOf()
        } else {

            transactions.let {
                it.clear()
                it.addAll(tempTxList)
            }
            withContext(Dispatchers.Main) {
                adapter?.notifyDataSetChanged()
            }
        }

        if (transactions.size > 0) {
            showTransactionList()
        } else {
            hideTransactionList()
        }
    }

    private fun isOffline(): Boolean {

        val isWifiConnected = context?.let { NetworkUtil.isWifiConnected(it) }
        val isDataConnected = context?.let { NetworkUtil.isMobileDataConnected(it) }
        val syncAnyways = util.getBoolean(Constants.WIFI_SYNC, false)

        // 1. If the user chooses not to sync(means the user syncs only on wifi and wifi is off or the user tapped NO on the dialog)
        if (!isWifiConnected!! && !syncAnyways) {
            return true
        }

        // 2. If the wifi is off, user chose sync anyways but mobile data isn’t activated, offline
        if (!isWifiConnected && syncAnyways && !isDataConnected!!) {
            return true
        }

        return false
    }

    override fun onTransaction(transactionJson: String?) {
        val transaction = gson.fromJson(transactionJson, Transaction::class.java)
        transaction.animate = true

        GlobalScope.launch(Dispatchers.Main){
            transactions.add(0, transaction)

            // remove last item if more than max
            if(transactions.size > MAX_TRANSACTIONS){
                transactions.removeAt(transactions.size - 1)
            }

            adapter?.notifyDataSetChanged()
        }
    }

    override fun onSyncCompleted() {
        GlobalScope.launch(Dispatchers.Main){
            balanceTextView.text = CoinFormat.format(multiWallet.totalWalletBalance(context!!))
        }
        loadTransactions()
    }
}

fun Overview.showTransactionList() = GlobalScope.launch(Dispatchers.Main) {
    noTransactionsTextView.hide()
    transactionsLayout.show()
}

fun Overview.hideTransactionList() = GlobalScope.launch(Dispatchers.Main) {
    noTransactionsTextView.show()
    transactionsLayout.hide()
}
