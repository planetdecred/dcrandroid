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
import dcrlibwallet.Dcrlibwallet
import dcrlibwallet.LibWallet
import dcrlibwallet.SyncProgressListener
import java.lang.Exception

const val NOTIFICATION_ID = 4

class SyncService : Service(), SyncProgressListener {

    private var TAG: String = "SyncService"
    private var notification: Notification? = null
    private var wallet: LibWallet? = null
    private var walletData: WalletData? = null
    private var preferenceUtil: PreferenceUtil? = null

    private var contentTitle: String? = null
    private var contentText: String? = null

    private var peerCount: Int = 0

    private var addressDiscoveryThread: Thread? = null

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

        contentTitle = "Dcrandroid"
        contentText = getString(R.string.connecting_to_peers)

        showNotification()

        wallet!!.addSyncProgressListener(this)

        val peerAddresses = preferenceUtil!!.get(Constants.PEER_IP)

        Log.d(TAG, "Starting SPV Sync")
        wallet!!.spvSync(peerAddresses)

        return super.onStartCommand(intent, flags, startId)
    }

    private fun showNotification() {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val launchPendingIntent = PendingIntent.getActivity(this, 1, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val subText: String = when (peerCount) {
            0 -> "Syncing"
            1 -> "Connected to a peer"
            else -> "Connected to $peerCount peers"
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = Notification.Builder(this, "syncer")
                    .setContentTitle(contentTitle)
                    .setSubText(subText)
                    .setContentText(contentText)
                    .setSmallIcon(R.drawable.decred_symbol_white)
                    .setOngoing(true)
                    .setAutoCancel(true)
                    .setContentIntent(launchPendingIntent)
                    .build()
        } else {
            notification = NotificationCompat.Builder(this)
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
        }
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

    override fun onFetchedHeaders(fetchedHeadersCount: Int, lastHeaderTime: Long, state: String) {
        contentTitle = getString(R.string.synchronizing)
        if (state == Dcrlibwallet.SyncStateStart) {
            contentText = null
        } else if (state == Dcrlibwallet.SyncStateProgress) {
            contentText = Utils.getSyncTimeRemaining(walletData!!.syncRemainingTime, walletData!!.syncProgress.toInt(), false, this)
        }

        showNotification()
    }

    override fun onDiscoveredAddresses(state: String) {
        if (state == Dcrlibwallet.SyncStateStart) {
            contentText = null
            showNotification()

            addressDiscoveryThread = Thread {
                try {
                    while (!Thread.interrupted()) {
                        Thread.sleep(1000)

                        contentText = null
                        contentText = Utils.getSyncTimeRemaining(walletData!!.syncRemainingTime, walletData!!.syncProgress.toInt(), false, this)
                        showNotification()
                    }
                } catch (_: InterruptedException) {
                }
            }

            addressDiscoveryThread!!.start()
        } else {
            if (addressDiscoveryThread != null && addressDiscoveryThread!!.isAlive) {
                addressDiscoveryThread!!.interrupt()
            }
        }
    }

    override fun onPeerConnected(peerCount: Int) {
        this.peerCount = peerCount
        showNotification()
    }

    override fun onPeerDisconnected(peerCount: Int) {
        this.peerCount = peerCount
        showNotification()
    }

    override fun onFetchMissingCFilters(missingCFitlersStart: Int, missingCFitlersEnd: Int, state: String) {

    }

    override fun onRescan(rescannedThrough: Int, state: String) {
        if (state == Dcrlibwallet.SyncStateProgress) {
            contentText = Utils.getSyncTimeRemaining(walletData!!.syncRemainingTime, walletData!!.syncProgress.toInt(), false, this)
            showNotification()
        }
    }

    override fun onIndexTransactions(p0: Int) {}

    override fun onSynced(synced: Boolean) {
        println("Synced, destroying service")
        stopForeground(true)
        stopSelf()
    }

    override fun onSyncError(code: Int, err: Exception?) {}

}