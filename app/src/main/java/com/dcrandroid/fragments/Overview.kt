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

        syncedLayout = view.findViewById(R.id.synced_unsynced_layout)
        latestBlockTextView = view.findViewById(R.id.tv_latest_block)
        connectedPeersTextView = view.findViewById(R.id.connected_peers)
        syncStateIcon = view.findViewById(R.id.sync_state_icon)

        syncingLayout = view.findViewById(R.id.syncing_layout)
        showDetails = view.findViewById(R.id.show_details)
        syncDetails = view.findViewById(R.id.sync_details)
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

        setupSyncLayout()

        setupBackupSeedLayout()

        setupOnlineOfflineStatus()

        setShowDetailsClickListener()

        setupSyncingLayout()

        if (!wallet.isSyncing) {
            reconnect_layout.setOnClickListener { startSyncing() }
        }
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

    fun setupSyncLayout() {
        if (wallet.isSyncing) {
            showSyncingLayout()
        } else {

            hideSyncingLayout()

            val bestBlock = wallet.bestBlock
            val currentTimeSeconds = System.currentTimeMillis() / 1000
            val lastBlockRelativeTime = currentTimeSeconds - wallet.bestBlockTimeStamp
            val formattedLastBlockTime = Utils.calculateTime(lastBlockRelativeTime, context)

            val latestBlock: String
            val connectedPeers: String

            if (wallet.isSynced) {
                reconnect_layout.visibility = View.GONE
                syncStateIcon.setImageResource(R.drawable.ic_checkmark)
                tv_sync_state.text = getString(R.string.synced)
                latestBlock = getString(R.string.synced_latest_block_time, bestBlock, formattedLastBlockTime)
                connectedPeers = getString(R.string.connected_peers, walletData.peers)
            } else {
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

    private fun setupOnlineOfflineStatus() {

        val state: String
        val drawable: Drawable

        if (isOffline()) {
            state = getString(R.string.offline)
            drawable = resources.getDrawable(R.drawable.offline_dot)
        } else {
            state = getString(R.string.online)
            drawable = resources.getDrawable(R.drawable.online_dot)
        }

        tv_online_offline_status.text = state
        view_online_offline_status.setBackgroundDrawable(drawable)
    }

    private fun setShowDetailsClickListener() {
        showDetails.setOnClickListener {
            syncDetails.toggleVisibility()
            scrollView.postDelayed({
                scrollView.smoothScrollTo(0, scrollView.bottom)
            }, 200)

            showDetails.text = if (syncDetails.visibility == View.VISIBLE) getString(R.string.hide_details) else getString(R.string.show_details)
        }
    }

    private fun setupSyncingLayout() {
        val days = TimeUnit.SECONDS.toDays(System.currentTimeMillis() / 1000 - wallet.bestBlockTimeStamp)
        val daysBehind = getString(R.string.days_behind, days)

        val peers = walletData.peers
        syncingLayout.tv_peers_count.text = peers.toString()
        syncingLayout.tv_percentage.text = getString(R.string.percentage, 0)
        syncingLayout.tv_time_left.text = Utils.getSyncTimeRemaining( 2400, context) // todo - get initial time left
        syncingLayout.tv_steps.text = getString(R.string.step_1_3)
        syncingLayout.tv_days.text = daysBehind
        syncingLayout.tv_steps_title.text = getString(R.string.fetching_block_headers, wallet.bestBlock)
        syncingLayout.tv_block_header_fetched.text = getString(R.string.block_header_fetched)
        syncingLayout.tv_fetch_discover_scan_count.text = getString(R.string.block_header_fetched_count, 0, wallet.bestBlock)
    }

    private fun startSyncing() {
        activity?.sendBroadcast(Intent(Constants.SYNCED))
        val syncIntent = Intent(activity, SyncService::class.java)
        activity?.startService(syncIntent)
        showSyncingLayout()
    }

    private fun setupBackupSeedLayout() {

        // using true as default as per backwards compatibility
        // we don't want to tell wallets created before this
        // feature to verify their seed
        if (!util.getBoolean(Constants.VERIFIED_SEED, true)) {
            backupSeedLayout.visibility = View.VISIBLE
            backupSeedLayout.imv_back_up.setOnClickListener {
                val seed = util.get(Constants.SEED)
                val i = Intent(context, VerifySeedActivity::class.java)
                        .putExtra(Constants.SEED, seed)
                startActivityForResult(i, VERIFY_SEED_REQUEST_CODE)
            }
        }
    }

    private fun publishProgress(syncProgress: Int, remainingSyncTime: Long, daysBehind: String, steps: String, stepsTitle: String, blockHeaderCount: String) {
        if (activity != null) {
            activity!!.runOnUiThread {

                val peers = walletData.peers
                syncingLayout.tv_peers_count.text = peers.toString()
                syncingLayout.pb_sync_progress.progress = syncProgress
                syncingLayout.tv_percentage.text = getString(R.string.percentage, syncProgress)
                syncingLayout.tv_time_left.text = Utils.getSyncTimeRemaining(remainingSyncTime, context)

                syncingLayout.tv_days.text = daysBehind
                syncingLayout.tv_steps.text = steps
                syncingLayout.tv_steps_title.text = stepsTitle
                syncingLayout.tv_fetch_discover_scan_count.text = blockHeaderCount
            }
        }
    }

    override fun onHeadersFetchProgress(headersFetchProgress: HeadersFetchProgressReport?) {
        super.onHeadersFetchProgress(headersFetchProgress)

        if (headersFetchProgress == null) {
            throw NullPointerException("HeadersFetchProgressReport is null")
        }

        val days = TimeUnit.SECONDS.toDays(System.currentTimeMillis() / 1000 - headersFetchProgress.currentHeaderTimestamp)
        val daysBehind = getString(R.string.days_behind, days)
        val syncProgress: Int = headersFetchProgress.generalSyncProgress.totalSyncProgress
        val remainingSyncTime: Long = headersFetchProgress.generalSyncProgress.totalTimeRemainingSeconds
        val steps = getString(R.string.step_1_3)
        val stepsTitle = getString(R.string.fetching_block_headers, headersFetchProgress.headersFetchProgress)
        val blockHeaderCount = getString(R.string.block_header_fetched_count, headersFetchProgress.fetchedHeadersCount, headersFetchProgress.totalHeadersToFetch)

        publishProgress(syncProgress, remainingSyncTime, daysBehind, steps, stepsTitle, blockHeaderCount)
    }

    override fun onAddressDiscoveryProgress(addressDiscoveryProgress: AddressDiscoveryProgressReport?) {
        super.onAddressDiscoveryProgress(addressDiscoveryProgress)
        if (addressDiscoveryProgress == null) {
            throw NullPointerException("AddressDiscoveryProgressReport is null")
        }

        val discoveryProgress = addressDiscoveryProgress.addressDiscoveryProgress.toLong()
        val syncProgress: Int = addressDiscoveryProgress.generalSyncProgress.totalSyncProgress
        val remainingSyncTime: Long = addressDiscoveryProgress.generalSyncProgress.totalTimeRemainingSeconds
        val daysBehind = getString(R.string.days_behind, 0)

        val steps = getString(R.string.step_2_3)
        val stepsTitle = getString(R.string.discovering_addresses, discoveryProgress)
        val blockHeaderCount = getString(R.string.block_header_fetched_count, wallet.bestBlock, wallet.bestBlock)

        publishProgress(syncProgress, remainingSyncTime, daysBehind, steps, stepsTitle, blockHeaderCount)
    }

    override fun onHeadersRescanProgress(headersRescanProgress: HeadersRescanProgressReport?) {
        super.onHeadersRescanProgress(headersRescanProgress)
        if (headersRescanProgress == null) {
            throw NullPointerException("HeadersRescanProgressReport is null")
        }

        val rescanProgress = headersRescanProgress.rescanProgress.toLong()
        val syncProgress: Int = headersRescanProgress.generalSyncProgress.totalSyncProgress
        val remainingSyncTime: Long = headersRescanProgress.generalSyncProgress.totalTimeRemainingSeconds
        val daysBehind = getString(R.string.days_behind, 0)

        val steps = getString(R.string.step_3_3)
        val stepsTitle = getString(R.string.scanning_block_headers, rescanProgress)
        val blockHeaderCount = getString(R.string.block_header_fetched_count, wallet.bestBlock, wallet.bestBlock)

        publishProgress(syncProgress, remainingSyncTime, daysBehind, steps, stepsTitle, blockHeaderCount)
    }

    override fun onSyncCanceled(willRestart: Boolean) {
        super.onSyncCanceled(willRestart)
        // clear sync layout if sync is not going to restart.
        if (!willRestart) {
            activity?.runOnUiThread {
                hideSyncingLayout()
            }
        }
    }

    override fun onSyncCompleted() {
        activity?.runOnUiThread {
            setupSyncLayout()
            hideSyncingLayout()
        }
    }

    override fun onSyncEndedWithError(err: java.lang.Exception?) {
        err!!.printStackTrace()
        activity?.runOnUiThread {
            hideSyncingLayout()
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
}

fun Overview.showTransactionList() {
    noTransactionsTextView.visibility = View.GONE
    transactionsLayout.visibility = View.VISIBLE
}

fun Overview.hideTransactionList() {
    noTransactionsTextView.visibility = View.VISIBLE
    transactionsLayout.visibility = View.GONE
}

fun Overview.showSyncingLayout() {
    syncingLayout.visibility = View.VISIBLE
    syncedLayout.visibility = View.GONE
}

fun Overview.hideSyncingLayout() {
    syncingLayout.visibility = View.GONE
    syncedLayout.visibility = View.VISIBLE
}