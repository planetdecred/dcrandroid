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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import com.dcrandroid.adapter.TransactionListAdapter
import com.dcrandroid.data.Constants
import com.dcrandroid.data.Transaction
import com.dcrandroid.extensions.toggleVisibility
import com.dcrandroid.extensions.totalWalletBalance
import com.dcrandroid.util.*
import com.google.gson.GsonBuilder
import dcrlibwallet.Dcrlibwallet
import dcrlibwallet.LibWallet
import kotlinx.android.synthetic.main.synced_unsynced_layout.*

class Overview : NotificationsFragment(), ViewTreeObserver.OnScrollChangedListener {

    private val requiredConfirmations: Int
        get() {
            return if (util.getBoolean(Constants.SPEND_UNCONFIRMED_FUNDS)) 0
            else Constants.REQUIRED_CONFIRMATIONS
        }

    private val walletData: WalletData = WalletData.getInstance()
    private val wallet: LibWallet
        get() = walletData.wallet

    private lateinit var util: PreferenceUtil

    private val transactions: ArrayList<Transaction> = ArrayList()
    private var adapter: TransactionListAdapter? = null
    private val gson = GsonBuilder().registerTypeHierarchyAdapter(ArrayList::class.java, Deserializer.TransactionDeserializer())
            .create()

    private lateinit var scrollView: ScrollView
    private lateinit var recyclerView: RecyclerView

    private lateinit var balanceTextView: TextView
    internal lateinit var noTransactionsTextView: TextView
    internal lateinit var transactionsLayout: LinearLayout

    internal lateinit var syncingLayout: LinearLayout
    private lateinit var showDetails: TextView
    private lateinit var syncDetails: LinearLayout

    internal lateinit var syncedLayout: LinearLayout
    private lateinit var latestBlockTextView: TextView
    private lateinit var connectedPeersTextView: TextView
    private lateinit var syncStateIcon: ImageView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_overview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        scrollView = view.findViewById(R.id.scroll_view_overview)
        recyclerView = view.findViewById(R.id.rv_transactions)

        balanceTextView = view.findViewById(R.id.tv_visble_wallet_balance)
        noTransactionsTextView = view.findViewById(R.id.tv_no_transactions)
        transactionsLayout = view.findViewById(R.id.transactions_view)

        syncedLayout = view.findViewById(R.id.synced_unsynced_layout)
        latestBlockTextView = view.findViewById(R.id.tv_latest_block)
        connectedPeersTextView = view.findViewById(R.id.connected_peers)
        syncStateIcon = view.findViewById(R.id.sync_state_icon)

        syncingLayout = view.findViewById(R.id.syncing_layout)
        showDetails = view.findViewById(R.id.show_details)
        syncDetails = view.findViewById(R.id.sync_details)
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

        balanceTextView.text = CoinFormat.format(wallet.totalWalletBalance(requiredConfirmations, context!!))

        loadTransactions()

        setupSyncLayout()

        showDetails.setOnClickListener {
            syncDetails.toggleVisibility()
            scrollView.postDelayed({
                scrollView.smoothScrollTo(0, scrollView.bottom)
            }, 200)

            showDetails.text = if(syncDetails.visibility == View.VISIBLE) "Hide Details" else "Show Details"
        }
    }

    override fun onScrollChanged() {
        val scrollY = scrollView.scrollY
        val textSize = context?.resources?.getDimension(R.dimen.visible_balance_text_size)

        if(textSize != null) {
            if (scrollY > textSize / 2) {
                setToolbarTitle(balanceTextView.text, true)
            } else {
                setToolbarTitle(R.string.overview, false)
            }
        }
    }

    private fun loadTransactions() {
        val jsonResult = wallet.getTransactions(3, Dcrlibwallet.TxFilterAll)
        val transactions = gson.fromJson(jsonResult, Array<Transaction>::class.java)

        this.transactions.let {
            it.clear()
            it.addAll(transactions)
        }

        adapter?.notifyDataSetChanged()

        if (this.transactions.size > 0) {
            showTransactionList()
        } else {
            hideTransactionList()
        }
    }

    private fun setupSyncLayout(){
        if(!wallet.isSyncing){
            showSyncingLayout()
        }else{

            hideSyncingLayout()

            val bestBlock = wallet.bestBlock
            val currentTimeSeconds = System.currentTimeMillis() / 1000
            val lastBlockRelativeTime = currentTimeSeconds - wallet.bestBlockTimeStamp
            val formattedLastBlockTime = Utils.calculateTime(lastBlockRelativeTime, context)

            val latestBlock: String
            val connectedPeers: String

            if(wallet.isSynced){
                reconnect_layout.visibility = View.GONE
                syncStateIcon.setImageResource(R.drawable.ic_checkmark)
                latestBlock = getString(R.string.synced_latest_block_time, bestBlock, formattedLastBlockTime)
                connectedPeers = getString(R.string.connected_peers, walletData.peers)
            }else{
                // not attempting to sync. (not syncing and not synced)
                reconnect_layout.visibility = View.VISIBLE
                syncStateIcon.setImageResource(R.drawable.ic_crossmark)
                latestBlock = getString(R.string.latest_block_time, bestBlock, formattedLastBlockTime)
                connectedPeers = getString(R.string.no_connected_peers)
            }

            latestBlockTextView.text = HtmlCompat.fromHtml(latestBlock, 0)
            connectedPeersTextView.text = HtmlCompat.fromHtml(connectedPeers, 0)


        }
    }
}

fun Overview.showTransactionList() {
    noTransactionsTextView.visibility = View.GONE
    transactionsLayout.visibility = View.VISIBLE
}

fun Overview.hideTransactionList() {
    noTransactionsTextView.visibility = View.VISIBLE
    transactionsLayout.visibility = View.GONE
}

fun Overview.showSyncingLayout(){
    syncingLayout.visibility = View.VISIBLE
    syncedLayout.visibility = View.GONE
}

fun Overview.hideSyncingLayout(){
    syncingLayout.visibility = View.GONE
    syncedLayout.visibility = View.VISIBLE
}