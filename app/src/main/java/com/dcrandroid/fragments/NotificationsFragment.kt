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

open class NotificationsFragment : Fragment(), SyncProgressListener {

    private val walletData: WalletData = WalletData.getInstance()
    private val multiWallet: MultiWallet
        get() = walletData.multiWallet

    override fun onStart() {
        super.onStart()
        multiWallet.addSyncProgressListener(this, this.javaClass.name)
    }

    override fun onStop() {
        super.onStop()
        multiWallet.removeSyncProgressListener(this.javaClass.name)
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


    override fun onBlockAttached(height: Int, timestamp: Long) {}

    override fun onTransactionConfirmed(hash: String?, height: Int) {}

    override fun onTransaction(transaction: String?) {}
}