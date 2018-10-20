package com.dcrandroid.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.util.DcrConstants
import com.dcrandroid.util.PreferenceUtil
import mobilewallet.LibWallet
import mobilewallet.SpvSyncResponse
import java.lang.Exception
import java.util.*

const val NOTIFICATION_ID = 4

class SyncService : Service(), SpvSyncResponse {

    private var TAG: String = "SyncService"
    private var notification: Notification? = null
    private var wallet: LibWallet? = null
    private var preferenceUtil: PreferenceUtil? = null

    private var contentTitle: String? = null
    private var contentText: String? = null

    private var peerCount: Int = 0

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service is Started")

        wallet = DcrConstants.getInstance().wallet

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

        wallet!!.addSyncResponse(this)

        val peerAddresses = preferenceUtil!!.get(Constants.PEER_IP)

        Log.d(TAG, "Starting SPV Sync")
        wallet!!.spvSync(peerAddresses)

        return super.onStartCommand(intent, flags, startId)
    }

    private fun showNotification() {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val launchPendingIntent = PendingIntent.getActivity(this, 1, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val subText: String = when(peerCount){
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

    override fun onFetchedHeaders(fetchedHeadersCount: Int, lastHeaderTime: Long, finished: Boolean) {
        if(finished){
            return
        }

        val currentTime = System.currentTimeMillis() / 1000
        val estimatedBlocks = (currentTime - lastHeaderTime) / 120 + wallet!!.bestBlock
        var fetchedPercentage = wallet!!.bestBlock.toFloat() / estimatedBlocks * 100
        fetchedPercentage = if (fetchedPercentage > 100) 100F else fetchedPercentage

        contentTitle = "(1/3) ${getString(R.string.fetching_headers)}"
        contentText = String.format(Locale.getDefault(), "%.1f%% %s", fetchedPercentage, getString(R.string.fetched))

        showNotification()
    }

    override fun onDiscoveredAddresses(finished: Boolean) {
        if (finished) {
            return
        }
        contentTitle = "(2/3) Discovering Addresses..."
        contentText = null

        showNotification()
    }

    override fun onPeerConnected(peerCount: Int) {
        this.peerCount = peerCount
        showNotification()
    }

    override fun onPeerDisconnected(peerCount: Int) {
        this.peerCount = peerCount
        showNotification()
    }

    override fun onSyncError(code: Long, err: Exception?) {

    }

    override fun onFetchMissingCFilters(missingCFitlersStart: Int, missingCFitlersEnd: Int, finished: Boolean) {

    }

    override fun onRescanProgress(rescannedThrough: Int, finished: Boolean) {
        if(!finished){
            contentTitle = "(3/3) Rescanning Blocks"

            val bestBlock = wallet!!.bestBlock
            val scannedPercentage = Math.round(rescannedThrough.toFloat() / bestBlock * 100)

            contentText = String.format(Locale.getDefault(), "%s: %d(%d%%)", getString(R.string.latest_block), bestBlock, scannedPercentage)

            showNotification()
        }
    }

    override fun onSynced(synced: Boolean) {
        if (synced) {
            println("Service is Synced")
            stopForeground(true)
            stopSelf()
        }
    }

}