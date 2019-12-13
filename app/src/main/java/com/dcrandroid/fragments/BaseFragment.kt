/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.fragments

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.dcrandroid.HomeActivity
import com.dcrandroid.util.WalletData
import dcrlibwallet.*

open class BaseFragment : Fragment(), SyncProgressListener, TxAndBlockNotificationListener {

    var TAG = this.javaClass.name

    private val walletData: WalletData = WalletData.instance
    internal val multiWallet: MultiWallet
        get() = walletData.multiWallet!!

    override fun onStart() {
        super.onStart()
        multiWallet.removeSyncProgressListener(TAG)
        multiWallet.removeTxAndBlockNotificationListener(TAG)

        multiWallet.addSyncProgressListener(this, TAG)
        multiWallet.addTxAndBlockNotificationListener(this, TAG)
    }

    override fun onStop() {
        super.onStop()
        multiWallet.removeSyncProgressListener(TAG)
    }

    fun setToolbarTitle(title: CharSequence, showShadow: Boolean) {
        if (activity is HomeActivity) {
            val homeActivity = activity as HomeActivity
            homeActivity.setToolbarTitle(title, showShadow)
        }
    }

    fun setToolbarTitle(@StringRes title: Int, showShadow: Boolean) {
        if (context != null) {
            setToolbarTitle(context!!.getString(title), showShadow)
        }
    }

    fun refreshNavigationTabs(){
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

    fun switchFragment(position: Int){
        if (activity is HomeActivity) {
            val homeActivity = activity as HomeActivity
            homeActivity.switchFragment(position)
        }
    }

    // -- Sync Progress Listener

    override fun onSyncStarted() {
    }

    override fun onHeadersRescanProgress(headersRescanProgress: HeadersRescanProgressReport?) {
    }

    override fun onAddressDiscoveryProgress(addressDiscoveryProgress: AddressDiscoveryProgressReport?) {}

    override fun onSyncCanceled(willRestart: Boolean) {}

    override fun onPeerConnectedOrDisconnected(numberOfConnectedPeers: Int) {}

    override fun onSyncCompleted() {}

    override fun onHeadersFetchProgress(headersFetchProgress: HeadersFetchProgressReport?) {}

    override fun onSyncEndedWithError(err: Exception?) {}

    override fun debug(debugInfo: DebugInfo?) {}


    override fun onTransactionConfirmed(walletID: Long, hash: String, blockHeight: Int) {}

    override fun onBlockAttached(walletID: Long, blockHeight: Int) {}

    override fun onTransaction(transactionJson: String?) {}
}