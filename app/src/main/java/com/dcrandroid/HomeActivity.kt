/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid

import android.animation.Animator
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
import com.dcrandroid.data.Constants
import com.dcrandroid.dialog.WiFiSyncDialog
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.openedWalletsList
import com.dcrandroid.extensions.show
import com.dcrandroid.fragments.WalletsFragment
import com.dcrandroid.fragments.Overview
import com.dcrandroid.fragments.ResumeAccountDiscovery
import com.dcrandroid.fragments.TransactionsFragment
import com.dcrandroid.service.SyncService
import com.dcrandroid.util.NetworkUtil
import com.dcrandroid.util.PreferenceUtil
import com.dcrandroid.util.Utils
import com.dcrandroid.util.WalletData
import dcrlibwallet.*
import kotlinx.android.synthetic.main.activity_tabs.*
import kotlin.math.roundToInt
import kotlin.system.exitProcess

const val TAG = "HomeActivity"

class HomeActivity : BaseActivity(), SyncProgressListener {

    private var deviceWidth: Int = 0
    private var blockNotificationSound: Int = 0

    private lateinit var adapter: NavigationTabsAdapter
    private lateinit var notificationManager: NotificationManager
    internal lateinit var util: PreferenceUtil
    private lateinit var alertSound: SoundPool
    private lateinit var currentFragment: Fragment

    private val walletData: WalletData = WalletData.getInstance()

    private var multiWallet: MultiWallet? = null
        get() = walletData.multiWallet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tabs)

        setSupportActionBar(toolbar)

        util = PreferenceUtil(this)
        if (multiWallet == null) {
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

        try {
            walletData.multiWallet.removeSyncProgressListener(TAG)
            walletData.multiWallet.addSyncProgressListener(this, TAG)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        initNavigationTabs()

        checkWifiSync()
    }

    override fun onDestroy() {
        super.onDestroy()

        val syncIntent = Intent(this, SyncService::class.java)
        stopService(syncIntent)

        multiWallet?.shutdown()
        finish()
        exitProcess(1)
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

        adapter = NavigationTabsAdapter(this, 0, deviceWidth, multiWallet!!.backupsNeeded) {position ->
            switchFragment(position)
        }
        recycler_view_tabs.adapter = adapter

        switchFragment(0)

    }

    fun refreshNavigationTabs(){
        adapter.backupsNeeded = multiWallet!!.backupsNeeded
        adapter.notifyItemChanged(2) // Wallets Page
    }

    private fun setTabIndicator() {
        tab_indicator.post {
            val tabWidth = deviceWidth / 4
            val tabIndicatorWidth = resources.getDimension(R.dimen.tab_indicator_width)

            var leftMargin = tabWidth * adapter.activeTab
            leftMargin += ((tabWidth - tabIndicatorWidth) / 2f).roundToInt()

            ObjectAnimator.ofFloat(tab_indicator, "translationX", leftMargin.toFloat()).apply {
                duration = 350
                start()
            }
        }
    }

    private fun showOrHideFab(position: Int){
        send_receive_layout.post {
            if(position < 2){ // show send and receive buttons for overview & transactions page
                send_receive_layout.show()
                ObjectAnimator.ofFloat(send_receive_layout, "translationY", 0f).setDuration(350).start() // bring view down
            }else{
                val objectAnimator = ObjectAnimator.ofFloat(send_receive_layout, "translationY", send_receive_layout.height.toFloat())

                objectAnimator.addListener(object : Animator.AnimatorListener{
                    override fun onAnimationRepeat(animation: Animator?) {
                    }

                    override fun onAnimationEnd(animation: Animator?) {
                        send_receive_layout.hide()
                    }

                    override fun onAnimationCancel(animation: Animator?) {
                    }

                    override fun onAnimationStart(animation: Animator?) {
                    }
                })

                objectAnimator.duration = 350

                objectAnimator.start()
            }
        }
    }

    fun switchFragment(position: Int) {

        currentFragment = when (position) {
            0 -> Overview()
            1 -> TransactionsFragment()
            2 -> WalletsFragment()
            else -> Fragment()
        }

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.frame, currentFragment)
                .commit()

        setTabIndicator()

        showOrHideFab(position)

        adapter.changeActiveTab(position)
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

    fun checkWifiSync() {
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

                })

        wifiSyncDialog.setOnCancelListener {
            sendBroadcast(Intent(Constants.SYNCED))
        }

        wifiSyncDialog.show()
    }

    fun startSyncing() {
        for (w in multiWallet!!.openedWalletsList()){
            w.walletExists()
            if(!w.hasDiscoveredAccounts() && w.isLocked){
                ResumeAccountDiscovery()
                        .setWalletID(w.walletID)
                        .show(supportFragmentManager, ResumeAccountDiscovery::javaClass.name)
                return
            }
        }
        sendBroadcast(Intent(Constants.SYNCED))
        val syncIntent = Intent(this, SyncService::class.java)
        startService(syncIntent)
    }

    // -- Block Notification

    override fun onTransactionConfirmed(walletID: Long, hash: String?) {
    }

    override fun onTransaction(transaction: String?) {
    }

    // -- Sync Progress Listener

    override fun onSyncStarted() {
    }

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