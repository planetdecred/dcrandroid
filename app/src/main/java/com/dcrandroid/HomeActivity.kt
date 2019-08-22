/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid

import android.animation.ObjectAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.activities.BaseActivity
import com.dcrandroid.adapter.NavigationTabsAdapter
import com.dcrandroid.adapter.OnTabSelectedListener
import com.dcrandroid.data.Constants
import com.dcrandroid.dialog.WiFiSyncDialog
import com.dcrandroid.fragments.Overview
import com.dcrandroid.service.SyncService
import com.dcrandroid.util.NetworkUtil
import com.dcrandroid.util.PreferenceUtil
import com.dcrandroid.util.Utils
import com.dcrandroid.util.WalletData
import dcrlibwallet.*
import kotlinx.android.synthetic.main.activity_tabs.*
import kotlin.math.roundToInt

const val TAG = "HomeActivity"

class HomeActivity : BaseActivity(), TransactionListener, SyncProgressListener {

    private var deviceWidth: Int = 0
    private var blockNotificationSound: Int = 0

    private lateinit var adapter: NavigationTabsAdapter
    private lateinit var notificationManager: NotificationManager
    internal lateinit var util: PreferenceUtil
    private lateinit var alertSound: SoundPool
    private lateinit var currentFragment: Fragment

    private val walletData: WalletData = WalletData.getInstance()
    private val wallet: LibWallet
        get() = walletData.wallet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tabs)

        util = PreferenceUtil(this)
        if (wallet == null) {
            println("Restarting app")
            Utils.restartApp(this)
        }

        registerNotificationChannel()

        alertSound = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val builder = SoundPool.Builder().setMaxStreams(3)
            val attributes = AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION).build()
            builder.setAudioAttributes(attributes)
            builder.build()
        } else {
            SoundPool(3, AudioManager.STREAM_NOTIFICATION, 0)
        }

        blockNotificationSound = alertSound.load(this, R.raw.beep, 1)

        walletData.wallet.transactionNotification(this)
        try {
            walletData.wallet.removeSyncProgressListener(TAG)
            walletData.wallet.addSyncProgressListener(this, TAG)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        initNavigationTabs()

        checkWifiSync()
    }

    private fun registerNotificationChannel() {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("new transaction", getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT)
            channel.enableLights(true)
            channel.enableVibration(true)
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            channel.importance = NotificationManager.IMPORTANCE_LOW
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun initNavigationTabs() {

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay
                .getMetrics(displayMetrics)
        deviceWidth = displayMetrics.widthPixels

        val mLayoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        recycler_view_tabs.layoutManager = mLayoutManager

        adapter = NavigationTabsAdapter(this, 0, deviceWidth)
        adapter.activeTab = 0
        recycler_view_tabs.adapter = adapter

        switchFragment(0)

        adapter.onTabSelectedListener = object : OnTabSelectedListener {
            override fun onTabSelected(position: Int) {
                switchFragment(position)
            }
        }
    }

    private fun setTabIndicator() {
        val tabWidth = deviceWidth / 4
        val tabIndicatorWidth = resources.getDimension(R.dimen.tab_indicator_width)

        var leftMargin = tabWidth * adapter.activeTab
        leftMargin += ((tabWidth - tabIndicatorWidth) / 2f).roundToInt()

        ObjectAnimator.ofFloat(tab_indicator, "translationX", leftMargin.toFloat()).apply {
            duration = 350
            start()
        }
    }

    private fun switchFragment(position: Int) {

        currentFragment = when (position) {
            0 -> Overview()
            else -> Fragment()
        }

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.frame, currentFragment)
                .commit()

        setTabIndicator()
    }

    fun setToolbarTitle(title: CharSequence, showShadow: Boolean) {
        toolbar_title.text = title
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            app_bar.elevation = if (showShadow) {
                resources.getDimension(R.dimen.app_bar_elevation)
            } else {
                0f
            }
        }
    }

    private fun checkWifiSync() {
        if (!util.getBoolean(Constants.WIFI_SYNC, false)) {
            // Check if wifi is connected
            val isWifiConnected = this.let { NetworkUtil.isWifiConnected(it) }
            if (!isWifiConnected) {
                showWifiNotice()
                return
            }
        }

        startSyncing()
    }

    private fun showWifiNotice() {
        val wifiSyncDialog = WiFiSyncDialog(this)
                .setPositiveButton(DialogInterface.OnClickListener { dialog, which ->
                    startSyncing()

                    val syncDialog = dialog as WiFiSyncDialog
                    util.setBoolean(Constants.WIFI_SYNC, syncDialog.checked)

                    if (currentFragment is Overview) {
                        val overviewFragment = currentFragment as Overview
                        overviewFragment.setupSyncLayout()
                    }
                })

        wifiSyncDialog.setOnCancelListener {
            sendBroadcast(Intent(Constants.SYNCED))
            if (currentFragment is Overview) {
                val overviewFragment = currentFragment as Overview
                overviewFragment.onSyncCanceled(false)
            }
        }

        wifiSyncDialog.show()
    }

    private fun startSyncing() {
        sendBroadcast(Intent(Constants.SYNCED))
        val syncIntent = Intent(this, SyncService::class.java)
        startService(syncIntent)
    }

    // -- Block Notification

    override fun onBlockAttached(height: Int, timestamp: Long) {
    }

    override fun onTransactionConfirmed(hash: String?, height: Int) {
    }

    override fun onTransaction(transaction: String?) {
    }

    // -- Sync Progress Listener

    override fun onHeadersRescanProgress(headersRescanProgress: HeadersRescanProgressReport?) {
    }

    override fun onAddressDiscoveryProgress(addressDiscoveryProgress: AddressDiscoveryProgressReport?) {
    }

    override fun onSyncCanceled(willRestart: Boolean) {
    }

    override fun onPeerConnectedOrDisconnected(numberOfConnectedPeers: Int) {
        walletData.peers = numberOfConnectedPeers
    }

    override fun onSyncCompleted() {
    }

    override fun onHeadersFetchProgress(headersFetchProgress: HeadersFetchProgressReport?) {
    }

    override fun onSyncEndedWithError(err: java.lang.Exception?) {
    }

    override fun debug(debugInfo: DebugInfo?) {
    }
}