/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.util

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import androidx.core.text.HtmlCompat
import com.dcrandroid.R
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.isShowing
import com.dcrandroid.extensions.show
import com.dcrandroid.extensions.toggleVisibility
import dcrlibwallet.*
import kotlinx.android.synthetic.main.fragment_overview.view.*
import kotlinx.android.synthetic.main.multi_wallet_sync_details.view.*
import kotlinx.android.synthetic.main.single_wallet_sync_details.view.*
import kotlinx.android.synthetic.main.synced_unsynced_layout.view.*
import kotlinx.android.synthetic.main.syncing_layout.view.*
import kotlinx.coroutines.*

class SyncLayoutUtil(private val syncLayout: LinearLayout) : SyncProgressListener {

    private val context: Context
        get() = syncLayout.context

    private val multiWallet: MultiWallet
        get() = WalletData.getInstance().multiWallet

    private var blockUpdateJob: Job? = null

    init {
        multiWallet.removeSyncProgressListener(this.javaClass.name)
        multiWallet.addSyncProgressListener(this, this.javaClass.name)

        if (multiWallet.isSyncing) {
            displaySyncingLayoutIfNotShowing()
            multiWallet.publishLastSyncProgress(this.javaClass.name)
        } else {
            displaySyncedUnsynced()
        }

        syncLayout.show_details.setOnClickListener {
            syncLayout.sync_details.toggleVisibility()

            syncLayout.show_details.text = if (syncLayout.sync_details.isShowing()) context.getString(R.string.hide_details)
            else context.getString(R.string.show_details)
        }
    }

    fun destroy() {
        blockUpdateJob?.cancel()
        multiWallet.removeSyncProgressListener(this.javaClass.name)
    }

    // this function basically prepares the sync layout for onHeadersFetchProgress
    private fun resetSyncingLayout() = GlobalScope.launch(Dispatchers.Main) {
        syncLayout.pb_sync_progress.progress = 0
        syncLayout.tv_percentage.text = context.getString(R.string.percentage, 0)
        syncLayout.tv_time_left.text = context.getString(R.string.time_left_seconds, 0)
        syncLayout.sync_details.hide()
        syncLayout.show_details.text = context.getString(R.string.show_details)

        syncLayout.tv_steps_title.text = context.getString(R.string.starting_up)
        syncLayout.tv_steps.text = context.getString(R.string.step_1_3)

        // single wallet setup
        showSyncVerboseExtras()

        // block headers fetched
        syncLayout.tv_block_header_fetched.setText(R.string.block_header_fetched)
        syncLayout.tv_fetch_discover_scan_count.text = "0"

        // syncing progress
        syncLayout.tv_progress.setText(R.string.syncing_progress)
        syncLayout.tv_days.text = null

        // connected peers count
        syncLayout.tv_syncing_layout_connected_peer.text = multiWallet.connectedPeers().toString()
    }

    private fun startupBlockUpdate() = GlobalScope.launch(Dispatchers.Default) {
        if (blockUpdateJob != null)
            return@launch

        blockUpdateJob = launch {
            while (true) {
                updateLatestBlock()
                delay(5000)
            }
        }
    }

    private fun updateLatestBlock() = GlobalScope.launch(Dispatchers.Main) {
        val blockInfo = multiWallet.bestBlock
        val currentTimeSeconds = System.currentTimeMillis() / 1000
        val lastBlockRelativeTime = currentTimeSeconds - blockInfo.timestamp
        val formattedLastBlockTime = Utils.calculateTime(lastBlockRelativeTime, syncLayout.context)

        val latestBlock: String
        latestBlock = if (multiWallet.isSynced) {
            context.getString(R.string.synced_latest_block_time, blockInfo.height, formattedLastBlockTime)
        } else {
            syncLayout.context.getString(R.string.latest_block_time, blockInfo.height, formattedLastBlockTime)
        }

        syncLayout.tv_latest_block.text = HtmlCompat.fromHtml(latestBlock, 0)
    }

    private fun displaySyncedUnsynced() {
        syncLayout.post {
            syncLayout.syncing_layout.hide()
            syncLayout.synced_unsynced_layout.show()

            val connectedPeers: String

            if (multiWallet.isSynced) {

                syncLayout.tv_online_offline_status.setText(R.string.online)
                syncLayout.view_online_offline_status.setBackgroundResource(R.drawable.online_dot)
                syncLayout.sync_state_icon.setImageResource(R.drawable.ic_checkmark)
                syncLayout.tv_sync_state.setText(R.string.synced)
                connectedPeers = context.getString(R.string.connected_peers, multiWallet.connectedPeers())

            } else {

                syncLayout.tv_online_offline_status.setText(R.string.offline)
                syncLayout.view_online_offline_status.setBackgroundResource(R.drawable.offline_dot)
                syncLayout.sync_state_icon.setImageResource(R.drawable.ic_crossmark)
                syncLayout.tv_sync_state.setText(R.string.not_syncing)
                connectedPeers = syncLayout.context.getString(R.string.no_connected_peers)
            }

            syncLayout.connected_peers.text = HtmlCompat.fromHtml(connectedPeers, 0)
            updateLatestBlock()
            if (multiWallet.isSynced)
                startupBlockUpdate()
        }
    }

    private fun displaySyncingLayout() {
        blockUpdateJob?.cancel()
        blockUpdateJob = null
        resetSyncingLayout()

        this.displaySyncingLayoutIfNotShowing()
    }

    private fun displaySyncingLayoutIfNotShowing() = GlobalScope.launch(Dispatchers.Main){
        syncLayout.syncing_layout.visibility = View.VISIBLE
        syncLayout.synced_unsynced_layout.visibility = View.GONE

        syncLayout.tv_online_offline_status.setText(R.string.online)
        syncLayout.view_online_offline_status.setBackgroundResource(R.drawable.online_dot)
    }

    private fun showSyncVerboseExtras(){
        syncLayout.syncing_layout_connected_peers_row.hide()

        syncLayout.sync_verbose.show()
        syncLayout.multi_wallet_sync_verbose.hide()
    }

    private fun hideSyncVerboseExtras(){
        syncLayout.syncing_layout_connected_peers_row.show()

        syncLayout.sync_verbose.hide()
        syncLayout.multi_wallet_sync_verbose.hide()
    }

    private fun publishSyncProgress(syncProgress: GeneralSyncProgress) = GlobalScope.launch(Dispatchers.Main) {
        syncLayout.pb_sync_progress.progress = syncProgress.totalSyncProgress
        syncLayout.tv_percentage.text = context.getString(R.string.percentage, syncProgress.totalSyncProgress)
        syncLayout.tv_time_left.text = Utils.getSyncTimeRemaining(syncProgress.totalTimeRemainingSeconds, context)
    }

    override fun onSyncStarted() {
        displaySyncingLayout()
    }

    override fun onHeadersFetchProgress(headersFetchProgress: HeadersFetchProgressReport?) {
        GlobalScope.launch(Dispatchers.Main){

            // stage title
            val syncStageTitle = context.getString(R.string.fetching_block_headers, headersFetchProgress?.headersFetchProgress)
            syncLayout.tv_steps_title.text = HtmlCompat.fromHtml(syncStageTitle, 0)

            syncLayout.tv_steps.text = context.getString(R.string.step_1_3)

            if(multiWallet.openedWalletsCount() > 0)
                syncLayout.syncing_layout_connected_peers_row.show()
            else
                syncLayout.syncing_layout_connected_peers_row.hide()

            // single wallet fetch headers layout
            if(multiWallet.openedWalletsCount() == 1){
                showSyncVerboseExtras()

                // block headers fetched
                syncLayout.tv_block_header_fetched.setText(R.string.block_header_fetched)
                syncLayout.tv_fetch_discover_scan_count.text = context.getString(R.string.block_header_fetched_count,
                        headersFetchProgress!!.fetchedHeadersCount, headersFetchProgress.totalHeadersToFetch)

                // syncing progress
                syncLayout.tv_progress.setText(R.string.syncing_progress)
                val lastHeaderRelativeTime = (System.currentTimeMillis() / 1000) - headersFetchProgress.currentHeaderTimestamp
                syncLayout.tv_days.text = Utils.getDaysBehind(lastHeaderRelativeTime, context)

            }
        }

        publishSyncProgress(headersFetchProgress!!.generalSyncProgress)
        displaySyncingLayoutIfNotShowing()
    }

    override fun onAddressDiscoveryProgress(addressDiscoveryProgress: AddressDiscoveryProgressReport?) {
        GlobalScope.launch(Dispatchers.Main) {
            // stage title
            val syncStageTitle = context.getString(R.string.discovering_addresses, addressDiscoveryProgress?.addressDiscoveryProgress)
            syncLayout.tv_steps_title.text = HtmlCompat.fromHtml(syncStageTitle, 0)

            syncLayout.tv_steps.text = context.getString(R.string.step_2_3)

            hideSyncVerboseExtras()

        }

        publishSyncProgress(addressDiscoveryProgress!!.generalSyncProgress)
        displaySyncingLayoutIfNotShowing()
    }

    override fun onHeadersRescanProgress(headersRescanProgress: HeadersRescanProgressReport?) {
        GlobalScope.launch(Dispatchers.Main) {
            // stage title
            val syncStageTitle = context.getString(R.string.scanning_block_headers, headersRescanProgress?.rescanProgress)
            syncLayout.tv_steps_title.text = HtmlCompat.fromHtml(syncStageTitle, 0)

            syncLayout.tv_steps.text = context.getString(R.string.step_3_3)

            showSyncVerboseExtras()

            // blocks scanned
            syncLayout.tv_block_header_fetched.setText(R.string.scanned_blocks)
            syncLayout.tv_fetch_discover_scan_count.text = headersRescanProgress!!.currentRescanHeight.toString()

            // scan progress
            syncLayout.tv_progress.setText(R.string.syncing_progress)
            syncLayout.tv_days.text = context.getString(R.string.blocks_left,
                    headersRescanProgress.totalHeadersToScan - headersRescanProgress.currentRescanHeight)
        }

        publishSyncProgress(headersRescanProgress!!.generalSyncProgress)
        displaySyncingLayoutIfNotShowing()
    }

    override fun onSyncCanceled(willRestart: Boolean) {
        if (!willRestart) {
            displaySyncedUnsynced()
        }
    }

    override fun onSyncEndedWithError(err: Exception?) {
        err?.printStackTrace()
        displaySyncedUnsynced()
    }

    override fun onSyncCompleted() {
        displaySyncedUnsynced()
    }

    override fun onPeerConnectedOrDisconnected(numberOfConnectedPeers: Int) {
        GlobalScope.launch(Dispatchers.Main){
            if (multiWallet.isSynced) {
                syncLayout.connected_peers.text = context.getString(R.string.connected_peers, multiWallet.connectedPeers())
            } else if (multiWallet.isSyncing) {
                syncLayout.tv_syncing_layout_connected_peer.text = numberOfConnectedPeers.toString()
                syncLayout.tv_single_wallet_peers_count.text = numberOfConnectedPeers.toString()
            }
        }
    }

    override fun debug(debugInfo: DebugInfo?) {}

    override fun onBlockAttached(height: Int, timestamp: Long) {
        updateLatestBlock()
    }

    override fun onTransactionConfirmed(hash: String?, height: Int) {}

    override fun onTransaction(transaction: String?) {}
}