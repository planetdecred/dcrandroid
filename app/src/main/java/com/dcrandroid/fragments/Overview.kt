/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.fragments

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.*
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import com.dcrandroid.activities.VerifySeedActivity
import com.dcrandroid.adapter.TransactionListAdapter
import com.dcrandroid.data.Constants
import com.dcrandroid.data.Transaction
import com.dcrandroid.extensions.toggleVisibility
import com.dcrandroid.extensions.totalWalletBalance
import com.dcrandroid.service.SyncService
import com.dcrandroid.util.*
import com.google.gson.GsonBuilder
import dcrlibwallet.*
import kotlinx.android.synthetic.main.backup_seed_prompt_layout.view.*
import kotlinx.android.synthetic.main.fragment_overview.*
import kotlinx.android.synthetic.main.synced_unsynced_layout.*
import kotlinx.android.synthetic.main.syncing_layout.view.*
import java.util.concurrent.TimeUnit

class Overview : NotificationsFragment(), ViewTreeObserver.OnScrollChangedListener {

    private val requiredConfirmations: Int
        get() {
            return if (util.getBoolean(Constants.SPEND_UNCONFIRMED_FUNDS)) 0
            else Constants.REQUIRED_CONFIRMATIONS
        }

    private val walletData: WalletData = WalletData.getInstance()
    private val wallet: LibWallet
        get() = walletData.wallet
    private val multiWallet: MultiWallet
        get() = walletData.multiWallet

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

    private lateinit var syncLayout: LinearLayout
    private var syncLayoutUtil: SyncLayoutUtil? = null

    private lateinit var backupSeedLayout: RelativeLayout

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

        backupSeedLayout = view.findViewById(R.id.backup_seed_prompt_layout)
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
    }

    override fun onResume() {
        super.onResume()
        syncLayoutUtil = SyncLayoutUtil(syncLayout)
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

    private fun loadTransactions() {
        val jsonResult = wallet.getTransactions(3, Dcrlibwallet.TxFilterAll)
        var transactions = gson.fromJson(jsonResult, Array<Transaction>::class.java)

        if (transactions == null) {
            transactions = arrayOf()
        } else {
            this.transactions.let {
                it.clear()
                it.addAll(transactions)
            }
            adapter?.notifyDataSetChanged()
        }

        if (this.transactions.size > 0) {
            showTransactionList()
        } else {
            hideTransactionList()
        }
    }

    private fun setShowDetailsClickListener() {
//        showDetails.setOnClickListener {
//            syncDetails.toggleVisibility()
//            scrollView.postDelayed({
//                scrollView.smoothScrollTo(0, scrollView.bottom)
//            }, 200)
//
//            showDetails.text = if (syncDetails.visibility == View.VISIBLE) getString(R.string.hide_details) else getString(R.string.show_details)
//        }
    }
//
//    private fun setupBackupSeedLayout() {
//
//        // using true as default as per backwards compatibility
//        // we don't want to tell wallets created before this
//        // feature to verify their seed
//        if (!util.getBoolean(Constants.VERIFIED_SEED, true)) {
//            backupSeedLayout.visibility = View.VISIBLE
//            backupSeedLayout.imv_back_up.setOnClickListener {
//                val seed = util.get(Constants.SEED)
//                val i = Intent(context, VerifySeedActivity::class.java)
//                        .putExtra(Constants.SEED, seed)
//                startActivityForResult(i, VERIFY_SEED_REQUEST_CODE)
//            }
//        }
//    }

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
}

fun Overview.showTransactionList() {
    noTransactionsTextView.visibility = View.GONE
    transactionsLayout.visibility = View.VISIBLE
}

fun Overview.hideTransactionList() {
    noTransactionsTextView.visibility = View.VISIBLE
    transactionsLayout.visibility = View.GONE
}
