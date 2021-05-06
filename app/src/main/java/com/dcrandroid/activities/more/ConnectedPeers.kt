/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities.more

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.dcrandroid.R
import com.dcrandroid.activities.BaseActivity
import com.dcrandroid.adapter.PeerInfoAdapter
import com.dcrandroid.data.PeerInfo
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.show
import com.google.gson.Gson
import dcrlibwallet.*
import kotlinx.android.synthetic.main.activity_connected_peers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ConnectedPeers : BaseActivity(), SyncProgressListener {

    private val peersInfo = ArrayList<PeerInfo>()

    private lateinit var adapter: PeerInfoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connected_peers)

        adapter = PeerInfoAdapter(peersInfo)
        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.adapter = adapter

        go_back.setOnClickListener {
            finish()
        }

        updateConnectedPeers()

        multiWallet?.removeSyncProgressListener(this.javaClass.name)
        multiWallet?.addSyncProgressListener(this, this.javaClass.name)
    }

    override fun onDestroy() {
        super.onDestroy()
        multiWallet?.removeSyncProgressListener(this.javaClass.name)
    }

    private fun updateConnectedPeers() = GlobalScope.launch(Dispatchers.Main) {
        val peersJson = multiWallet!!.peerInfo()

        peersInfo.clear()
        peersInfo.addAll(Gson().fromJson(peersJson, Array<PeerInfo>::class.java))

        adapter.notifyDataSetChanged()

        if (peersInfo.size == 0) {
            no_peers_available.show()
        } else {
            no_peers_available.hide()
        }
    }

    override fun onHeadersRescanProgress(p0: HeadersRescanProgressReport?) {

    }

    override fun onAddressDiscoveryProgress(p0: AddressDiscoveryProgressReport?) {

    }

    override fun onCFiltersFetchProgress(p0: CFiltersFetchProgressReport?) {

    }

    override fun onSyncCanceled(p0: Boolean) {

    }

    override fun onPeerConnectedOrDisconnected(p0: Int) {
        updateConnectedPeers()
    }

    override fun onSyncCompleted() {

    }

    override fun onSyncStarted(p0: Boolean) {

    }

    override fun onHeadersFetchProgress(p0: HeadersFetchProgressReport?) {

    }

    override fun onSyncEndedWithError(p0: Exception?) {

    }

    override fun debug(p0: DebugInfo?) {

    }
}