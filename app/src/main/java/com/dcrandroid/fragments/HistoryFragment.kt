/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dcrandroid.R
import com.dcrandroid.activities.TransactionDetailsActivity
import com.dcrandroid.adapter.TransactionAdapter
import com.dcrandroid.data.Constants
import com.dcrandroid.data.Transaction
import com.dcrandroid.util.*
import com.google.gson.GsonBuilder
import dcrlibwallet.Dcrlibwallet
import dcrlibwallet.LibWallet
import kotlinx.android.synthetic.main.content_history.*
import java.util.*

class HistoryFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    private var latestTransactionHeight: Int = 0
    private var needsUpdate = false
    private var isForeground: Boolean = false

    private var transactionAdapter: TransactionAdapter? = null
    private var sortSpinnerAdapter: ArrayAdapter<String>? = null

    private val transactionList = ArrayList<Transaction>()
    private val availableTxTypes = ArrayList<String>()

    private var walletData: WalletData? = null
    private val wallet: LibWallet
        get() = walletData!!.wallet

    private var util: PreferenceUtil? = null

    private val ALL: String by lazy { getString(R.string.all) }
    private val SENT: String by lazy { getString(R.string.sent) }
    private val RECEIVED: String by lazy { getString(R.string.received) }
    private val YOURSELF: String by lazy { getString(R.string.yourself) }
    private val STAKING: String by lazy { getString(R.string.staking) }
    private val COINBASE: String by lazy { getString(R.string.coinbase) }

    private var transactionTypeSelected = ""
    private val selectedTxFilter: Int
        get() {

            val t = transactionTypeSelected

            return when {
                t.startsWith(SENT) -> Dcrlibwallet.TxFilterSent
                t.startsWith(RECEIVED) -> Dcrlibwallet.TxFilterReceived
                t.startsWith(YOURSELF) -> Dcrlibwallet.TxFilterTransferred
                t.startsWith(STAKING) -> Dcrlibwallet.TxFilterStaking
                t.startsWith(COINBASE) -> Dcrlibwallet.TxFilterCoinBase
                else -> Dcrlibwallet.TxFilterAll
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.content_history, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (activity != null)
            activity!!.title = getString(R.string.history)

        util = PreferenceUtil(context!!)
        walletData = WalletData.getInstance()

        swipe_refresh_layout.setColorSchemeResources(
                R.color.colorPrimaryDarkBlue,
                R.color.colorPrimaryDarkBlue,
                R.color.colorPrimaryDarkBlue,
                R.color.colorPrimaryDarkBlue)
        swipe_refresh_layout.setOnRefreshListener(this)

        transactionAdapter = TransactionAdapter(transactionList, context)

        val mLayoutManager = LinearLayoutManager(context)
        history_recycler_view.layoutManager = mLayoutManager

        history_recycler_view.itemAnimator = DefaultItemAnimator()
        history_recycler_view.addItemDecoration(DividerItemDecoration(context!!, LinearLayoutManager.VERTICAL))
        history_recycler_view.addOnItemTouchListener(RecyclerTouchListener(context, history_recycler_view, object : RecyclerTouchListener.ClickListener {
            override fun onClick(view: View, position: Int) {
                if (transactionList.size <= position || position < 0) {
                    return
                }
                val tx = transactionList[position]
                val i = Intent(context, TransactionDetailsActivity::class.java)
                i.putExtra(Constants.TRANSACTION, tx)
                startActivity(i)
            }

            override fun onLongClick(view: View, position: Int) {

            }
        }))

        history_recycler_view.adapter = transactionAdapter
        registerForContextMenu(history_recycler_view)

        sortSpinnerAdapter = ArrayAdapter(context!!, android.R.layout.simple_spinner_item, availableTxTypes)
        sortSpinnerAdapter!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerHistory.adapter = sortSpinnerAdapter

        loadTxFilters()

        transactionTypeSelected = ALL
        spinnerHistory.setSelection(0, false)

        setupSortListener()

        prepareHistoryData()
    }

    override fun onResume() {
        super.onResume()
        if (context != null) {
            val filter = IntentFilter(Constants.SYNCED)
            context!!.registerReceiver(receiver, filter)
        }

        isForeground = true
        if (needsUpdate) {
            needsUpdate = false
            prepareHistoryData()
        }

    }

    override fun onPause() {
        super.onPause()
        if (context != null) {
            context!!.unregisterReceiver(receiver)
        }
        isForeground = false
    }

    private fun loadTxFilters() {

        availableTxTypes.clear()

        val txCount = wallet.countTransactions(Dcrlibwallet.TxFilterAll)
        val sentTxCount = wallet.countTransactions(Dcrlibwallet.TxFilterSent)
        val receivedTxCount = wallet.countTransactions(Dcrlibwallet.TxFilterReceived)
        val transferredTxCount = wallet.countTransactions(Dcrlibwallet.TxFilterTransferred)
        val stakingTxCount = wallet.countTransactions(Dcrlibwallet.TxFilterStaking)
        val coinbaseTxCount = wallet.countTransactions(Dcrlibwallet.TxFilterCoinBase)

        availableTxTypes.add("$ALL ($txCount)")
        availableTxTypes.add("$SENT ($sentTxCount)")
        availableTxTypes.add("$RECEIVED ($receivedTxCount)")
        availableTxTypes.add("$YOURSELF ($transferredTxCount)")

        if (stakingTxCount > 0) {
            availableTxTypes.add("$STAKING ($stakingTxCount)")
        }

        if (coinbaseTxCount > 0) {
            availableTxTypes.add("$COINBASE ($coinbaseTxCount)")
        }

        sortSpinnerAdapter!!.notifyDataSetChanged()
    }

    private fun prepareHistoryData() {
        if (!isForeground) {
            needsUpdate = true
            return
        }

        history_recycler_view.visibility = View.GONE
        no_history.visibility = View.VISIBLE
        swipe_refresh_layout.isRefreshing = true

        if (transactionList.size == 0) {
            no_history.setText(R.string.no_transactions)
            no_history.visibility = View.VISIBLE
            history_recycler_view.visibility = View.GONE
        } else {
            no_history.visibility = View.GONE
            history_recycler_view.visibility = View.VISIBLE
        }

        if (walletData!!.multiWallet.isSyncing) {
            no_history.setText(R.string.synchronizing)
            swipe_refresh_layout.isRefreshing = false
            return
        }

        object : Thread() {
            override fun run() {
                try {
                    val jsonResult = walletData!!.multiWallet.getTransactions(0, 0, selectedTxFilter)

                    val gson = GsonBuilder().registerTypeHierarchyAdapter(ArrayList::class.java, Deserializer.TransactionDeserializer())
                            .create()
                    val transactions = gson.fromJson(jsonResult, Array<Transaction>::class.java)

                    activity!!.runOnUiThread {
                        if (transactions == null || transactions.isEmpty()) {
                            no_history.setText(R.string.no_transactions_have_occurred)
                            no_history.visibility = View.VISIBLE
                            history_recycler_view.visibility = View.GONE
                            if (swipe_refresh_layout.isRefreshing) {
                                swipe_refresh_layout.isRefreshing = false
                            }
                        } else {
                            transactionList.clear()
                            transactionList.addAll(transactions)

                            latestTransactionHeight = transactions[0].height
                            transactionList.forEach { latestTransactionHeight = if (it.height < latestTransactionHeight) it.height else latestTransactionHeight }
                            latestTransactionHeight += 1

                            if (transactionList.size > 0 && selectedTxFilter == Dcrlibwallet.TxFilterAll) {
                                val recentTransactionHash = util!!.get(Constants.RECENT_TRANSACTION_HASH)
                                if (recentTransactionHash.isNotEmpty()) {
                                    val hashIndex = transactionList.indexOfFirst { it.hash == recentTransactionHash }

                                    if (hashIndex == -1) {
                                        // All transactions in this list is new
                                        transactionList.map { it.animate = true }
                                    } else if (hashIndex != 0) {
                                        transactionList.mapIndexed { index, it -> if(index < hashIndex) it.animate = true }
                                    }
                                }

                                util!!.set(Constants.RECENT_TRANSACTION_HASH, transactionList[0].hash)
                            }

                            loadTxFilters()

                            sortSpinnerAdapter!!.notifyDataSetChanged()

                            if (transactionList.size > 0) {
                                history_recycler_view.visibility = View.VISIBLE
                                no_history.visibility = View.GONE
                            } else {
                                no_history.setText(R.string.no_transactions)
                                no_history.visibility = View.VISIBLE
                                history_recycler_view.visibility = View.GONE
                            }

                            if (swipe_refresh_layout.isRefreshing) {
                                swipe_refresh_layout.isRefreshing = false
                            }

                            transactionAdapter!!.notifyDataSetChanged()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }.start()
    }

    override fun onRefresh() {
        prepareHistoryData()
    }

    fun newTransaction(transaction: Transaction) {
        if (transactionList.find { it.hash == transaction.hash } != null) {
            // Transaction is a duplicate
            return
        }

        if (transaction.height > 0) {
            latestTransactionHeight = transaction.height + 1
        }

        transactionList.add(0, transaction)
        println("New transaction info ${transaction.hash}")
        util!!.set(Constants.RECENT_TRANSACTION_HASH, transaction.hash)

        val sameFilter = wallet.compareTxFilter(selectedTxFilter, transaction.type, transaction.direction)
        if (sameFilter) {
            transaction.animate = true
            transactionList.add(0, transaction)
            history_recycler_view.post {
                history_recycler_view.visibility = View.VISIBLE
                transactionAdapter!!.notifyDataSetChanged()
            }
        }
    }

    fun transactionConfirmed(hash: String, height: Int) {
        transactionList.forEach {
            if (it.hash == hash) {
                it.height = height
                it.animate = false
                latestTransactionHeight = it.height + 1
                activity!!.runOnUiThread { transactionAdapter!!.notifyDataSetChanged() }
                return
            }
        }
    }

    fun blockAttached(height: Int) {
        if ((height - latestTransactionHeight) < 2) {
            transactionList.forEach {
                if ((height - it.height) >= 2) {
                    it.animate = true

                    if (activity != null) {
                        activity!!.runOnUiThread { transactionAdapter!!.notifyItemChanged(transactionList.indexOf(it)) }
                    }
                }
            }
        }
    }

    private fun setupSortListener() {
        spinnerHistory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                println("onItemSelected")
                transactionTypeSelected = availableTxTypes[position]
                prepareHistoryData()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }
    }

    private var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != null && intent.action == Constants.SYNCED) {
                if (!walletData!!.multiWallet.isSyncing) {
                    prepareHistoryData()
                }
            }
        }
    }
}