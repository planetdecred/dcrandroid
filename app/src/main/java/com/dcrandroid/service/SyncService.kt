/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.util.PreferenceUtil
import com.dcrandroid.util.Utils
import com.dcrandroid.util.WalletData
import dcrlibwallet.*
import java.lang.Exception

const val NOTIFICATION_ID = 4
const val TAG = "SyncService"

class SyncService : Service(), SyncProgressListener {

    private var notification: Notification? = null
    private var wallet: LibWallet? = null
    private var walletData: WalletData? = null
    private var preferenceUtil: PreferenceUtil? = null

    private var contentTitle: String? = null
    private var contentText: String? = null

    private var peerCount: Int = 0

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service is Started")

        walletData = WalletData.getInstance()
        wallet = walletData!!.wallet

        if (wallet == null) {
            Log.d(TAG, "Wallet is null")
            return super.onStartCommand(intent, flags, startId)
        }

        preferenceUtil = PreferenceUtil(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel("syncer", "Wallet Syncer", NotificationManager.IMPORTANCE_DEFAULT)
            channel.enableLights(false)
            channel.setSound(null, null)
            channel.enableVibration(false)
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            channel.importance = NotificationManager.IMPORTANCE_LOW
            manager.createNotificationChannel(channel)
        }

        contentTitle = getString(R.string.app_name)
        contentText = getString(R.string.connecting_to_peers)

        showNotification()

        wallet!!.removeSyncProgressListener(TAG)
        wallet!!.addSyncProgressListener(this, TAG)

        if (Integer.parseInt(preferenceUtil!!.get(Constants.NETWORK_MODES, "0")) == 0) {
            val peerAddresses = preferenceUtil!!.get(Constants.PEER_IP)
            Log.d(TAG, "Starting SPV Sync")
            wallet!!.spvSync(peerAddresses)
        } else {
            val remoteNodeAddress = preferenceUtil!!.get(Constants.REMOTE_NODE_ADDRESS)
            Log.d(TAG, "Starting RPC Sync")
            wallet!!.rpcSync(remoteNodeAddress, "dcrwallet", "dcrwallet", Utils.getRemoteCertificate(this).toByteArray())
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun showNotification() {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val launchPendingIntent = PendingIntent.getActivity(this, 1, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val subText: String = when (peerCount) {
            0 -> getString(R.string.syncing)
            1 -> getString(R.string.connected_to_a_peer)
            else -> getString(R.string.connected_to_n_peer, peerCount)
        }

        notification = NotificationCompat.Builder(this, "syncer")
                .setContentTitle(contentTitle)
                .setSubText(subText)
                .setContentText(contentText)
                .setSmallIcon(R.drawable.decred_symbol_white)
                .setOngoing(true)
                .setAutoCancel(true)
                .setSound(null)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(launchPendingIntent)
                .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service Destroyed")
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        println("Task Removed")
    }

    private fun publishProgress(remainingTime: Long, syncProgress: Int){
        contentTitle = getString(R.string.synchronizing)
        contentText = Utils.getSyncTimeRemaining(remainingTime, syncProgress, false, this)
        showNotification()
    }

    override fun onHeadersFetchProgress(headersFetchProgress: HeadersFetchProgressReport) {
        publishProgress(headersFetchProgress.generalSyncProgress.totalTimeRemainingSeconds, headersFetchProgress.generalSyncProgress.totalSyncProgress)
    }

    override fun onAddressDiscoveryProgress(addressDiscoveryProgress: AddressDiscoveryProgressReport) {
        publishProgress(addressDiscoveryProgress.generalSyncProgress.totalTimeRemainingSeconds, addressDiscoveryProgress.generalSyncProgress.totalSyncProgress)
    }

    override fun onHeadersRescanProgress(headersRescanProgress: HeadersRescanProgressReport) {
        publishProgress(headersRescanProgress.generalSyncProgress.totalTimeRemainingSeconds, headersRescanProgress.generalSyncProgress.totalSyncProgress)
    }

    override fun onSyncEndedWithError(err: Exception) {
        err.printStackTrace()
    }

    override fun onSyncCanceled(willRestart: Boolean) {
        if(willRestart){
            println("Sync Restarting")
            return
        }
        println("Sync Canceled, destroying service")
        stopForeground(true)
        stopSelf()
    }

    override fun onPeerConnectedOrDisconnected(numberOfConnectedPeers: Int) {
        this.peerCount = numberOfConnectedPeers
        showNotification()
    }

    override fun onSyncCompleted() {
        println("Synced, destroying service")
        stopForeground(true)
        stopSelf()
    }

    override fun debug(debugInfo: DebugInfo?) {}
}