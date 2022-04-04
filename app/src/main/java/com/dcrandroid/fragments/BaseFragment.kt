/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.fragments

import androidx.fragment.app.Fragment
import com.dcrandroid.HomeActivity
import com.dcrandroid.data.Transaction
import com.dcrandroid.util.WalletData
import com.google.gson.Gson
import dcrlibwallet.*

open class BaseFragment : Fragment(), SyncProgressListener, TxAndBlockNotificationListener {

    var TAG = this.javaClass.name
    var isForeground = false
    var requiresDataUpdate = false

    private val walletData: WalletData = WalletData.instance
    internal val multiWallet: MultiWallet?
        get() = walletData.multiWallet

    override fun onStart() {
        super.onStart()
        multiWallet?.removeSyncProgressListener(TAG)
        multiWallet?.removeTxAndBlockNotificationListener(TAG)

        multiWallet?.addSyncProgressListener(this, TAG)
        multiWallet?.addTxAndBlockNotificationListener(this, true, TAG)
    }

    override fun onResume() {
        super.onResume()
        isForeground = true
        if (requiresDataUpdate) {
            requiresDataUpdate = false
            onTxOrBalanceUpdateRequired(null)
        }
    }

    override fun onPause() {
        super.onPause()
        isForeground = false
    }

    override fun onDestroy() {
        super.onDestroy()
        multiWallet?.removeSyncProgressListener(TAG)
        multiWallet?.removeTxAndBlockNotificationListener(TAG)
    }

    fun refreshNavigationTabs() {
        if (activity is HomeActivity) {
            val homeActivity = activity as HomeActivity
            homeActivity.refreshNavigationTabs()
        }
    }

    fun restartSyncProcess() {
        if (activity is HomeActivity) {
            val homeActivity = activity as HomeActivity
            homeActivity.checkWifiSync()
        }
    }

    fun switchFragment(position: Int) {
        if (activity is HomeActivity) {
            val homeActivity = activity as HomeActivity
            homeActivity.switchFragment(position)
        }
    }

    open fun onTxOrBalanceUpdateRequired(walletID: Long?) {
        if (!isForeground) {
            requiresDataUpdate = true
            return
        }
    }

    // -- Sync Progress Listener

    override fun onSyncStarted(wasRestarted: Boolean) {
    }

    override fun onCFiltersFetchProgress(cfiltersFetchProgress: CFiltersFetchProgressReport?) {}

    override fun onHeadersFetchProgress(headersFetchProgress: HeadersFetchProgressReport?) {}

    override fun onAddressDiscoveryProgress(addressDiscoveryProgress: AddressDiscoveryProgressReport?) {}

    override fun onHeadersRescanProgress(headersRescanProgress: HeadersRescanProgressReport?) {}

    override fun onSyncCanceled(willRestart: Boolean) {}

    override fun onPeerConnectedOrDisconnected(numberOfConnectedPeers: Int) {}

    override fun onSyncCompleted() {
        onTxOrBalanceUpdateRequired(null)
    }

    override fun onSyncEndedWithError(err: Exception?) {}

    override fun debug(debugInfo: DebugInfo?) {}


    override fun onTransactionConfirmed(walletID: Long, hash: String, blockHeight: Int) {
        onTxOrBalanceUpdateRequired(walletID)
    }

    override fun onBlockAttached(walletID: Long, blockHeight: Int) {
        onTxOrBalanceUpdateRequired(walletID)
    }

    override fun onTransaction(transactionJson: String?) {
        val transaction = Gson().fromJson(transactionJson, Transaction::class.java)
        onTxOrBalanceUpdateRequired(transaction.walletID)
    }
}