/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.fragments

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.AnimationDrawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dcrandroid.BuildConfig
import com.dcrandroid.MainActivity
import com.dcrandroid.R
import com.dcrandroid.activities.TransactionDetailsActivity
import com.dcrandroid.adapter.TransactionAdapter
import com.dcrandroid.data.Account
import com.dcrandroid.data.Constants
import com.dcrandroid.data.Transaction
import com.dcrandroid.util.*
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.content_overview.*
import kotlinx.android.synthetic.main.overview_sync_layout.*
import java.math.BigDecimal
import java.math.MathContext
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList

class OverviewFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    private var transactionAdapter: TransactionAdapter? = null
    private var util: PreferenceUtil? = null
    private var walletData: WalletData? = null
    private val transactionList = ArrayList<Transaction>()
    private var latestTransactionHeight: Int = 0
    private var needsUpdate = false
    private var isForeground: Boolean = false
    private var notificationManager: NotificationManager? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.content_overview, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (activity != null) {
            activity!!.title = getString(R.string.overview)
        }

        registerNotificationChannel()

        util = PreferenceUtil(context!!)
        walletData = WalletData.getInstance()

        swipe_refresh_layout2.setColorSchemeResources(R.color.colorPrimaryDarkBlue,
                R.color.colorPrimaryDarkBlue,
                R.color.colorPrimaryDarkBlue,
                R.color.colorPrimaryDarkBlue)
        swipe_refresh_layout2.setOnRefreshListener(this)
        transactionAdapter = TransactionAdapter(transactionList, context!!)
        iv_sync_indicator.setBackgroundResource(R.drawable.sync_animation)

        if (!walletData!!.syncing) {
            iv_sync_indicator.post {
                val syncAnimation = iv_sync_indicator.background as AnimationDrawable
                syncAnimation.start()
            }

            pb_sync_progress.progress = 0
            overview_sync_layout.visibility = View.VISIBLE
            tv_synchronizing.setText(R.string.starting_synchronization)
            if (walletData!!.syncStatus != null) {
                publishProgress()
            }

        } else {
            getBalance()
            iv_sync_indicator.visibility = View.GONE
            overview_av_balance.visibility = View.VISIBLE
        }

        val mLayoutManager = LinearLayoutManager(context)
        history_recycler_view2.layoutManager = mLayoutManager
        history_recycler_view2.itemAnimator = DefaultItemAnimator()
        history_recycler_view2.addItemDecoration(DividerItemDecoration(context!!, LinearLayoutManager.VERTICAL))

        history_recycler_view2.addOnItemTouchListener(RecyclerTouchListener(context, history_recycler_view2, object : RecyclerTouchListener.ClickListener {
            override fun onClick(view: View, position: Int) {
                if (transactionList.size <= position || position < 0) {
                    return
                }
                val history = transactionList[position]
                val i = Intent(context, TransactionDetailsActivity::class.java)
                val extras = Bundle()
                extras.putLong(Constants.AMOUNT, history.amount)
                extras.putLong(Constants.FEE, history.fee)
                extras.putLong(Constants.TIMESTAMP, history.timestamp)
                extras.putInt(Constants.HEIGHT, history.height)
                extras.putLong(Constants.TOTAL_INPUT, history.totalInput)
                extras.putLong(Constants.TOTAL_OUTPUT, history.totalOutput)
                extras.putString(Constants.TYPE, history.type)
                extras.putString(Constants.HASH, history.hash)
                extras.putString(Constants.RAW, history.raw)
                extras.putInt(Constants.DIRECTION, history.direction)
                extras.putSerializable(Constants.INPUTS, history.inputs)
                extras.putSerializable(Constants.OUTPUTS, history.outputs)
                i.putExtras(extras)
                startActivity(i)
            }

            override fun onLongClick(view: View, position: Int) {

            }
        }))

        show_history.setOnClickListener {
            it.postDelayed({
                if (activity != null && activity is MainActivity) {
                    val mainActivity = activity as MainActivity?
                    mainActivity!!.displayHistory()
                }
            }, 200)
        }

        send.setOnClickListener {
            it.postDelayed({
                if (activity != null && activity is MainActivity) {
                    val mainActivity = activity as MainActivity?
                    mainActivity!!.displaySend()
                }
            }, 200)
        }

        receive.setOnClickListener {
            it.postDelayed({
                if (activity != null && activity is MainActivity) {
                    val mainActivity = activity as MainActivity?
                    mainActivity!!.displayReceive()
                }
            }, 200)
        }

        tap_for_more_info.setOnClickListener {
            it.visibility = View.GONE
            pb_status_layout.visibility = View.VISIBLE
            syncing_peers.visibility = View.VISIBLE
        }

        pb_status_layout.setOnClickListener {
            it.visibility = View.GONE
            syncing_peers.visibility = View.GONE
            tap_for_more_info.visibility = View.VISIBLE
        }

        pb_status_layout.setOnLongClickListener {
            if (pb_verbose_status.visibility == View.VISIBLE) {
                pb_verbose_status.visibility = View.GONE
            } else {
                pb_verbose_status.visibility = View.VISIBLE
            }
            return@setOnLongClickListener true
        }

        syncing_peers.setOnClickListener {
            it.visibility = View.GONE
            pb_status_layout.visibility = View.GONE
            tap_for_more_info.visibility = View.VISIBLE
        }

        val vto = history_recycler_view2.viewTreeObserver
        if (vto.isAlive) {
            vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    val recyclerViewHeight = history_recycler_view2.height
                    println("Recycler View Height: ${history_recycler_view2.height}")
                    if (recyclerViewHeight != 0)
                        prepareHistoryData()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        history_recycler_view2.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    } else {
                        history_recycler_view2.viewTreeObserver.removeGlobalOnLayoutListener(this)
                    }
                }
            })
        }

        history_recycler_view2.adapter = transactionAdapter
        registerForContextMenu(history_recycler_view2)
    }


    private fun registerNotificationChannel() {
        notificationManager = context!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("new transaction", getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT)
            channel.enableLights(true)
            channel.enableVibration(true)
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            channel.importance = NotificationManager.IMPORTANCE_LOW
            notificationManager!!.createNotificationChannel(channel)
        }
    }

    private fun getMaxDisplayItems(): Int {
        if (activity == null) {
            return 0
        }
        val px = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 52f, activity!!.resources.displayMetrics))
        val max = history_recycler_view2.height / px
        if (max < 1) {
            return 5
        }


        return max
    }

    private fun getBalance() {
        if (walletData!!.syncing) {
            return
        }

        object : Thread() {
            override fun run() {
                try {
                    if (context == null) {
                        return
                    }
                    val accounts = Account.parse(walletData!!.wallet.getAccounts(if (util!!.getBoolean(Constants.SPEND_UNCONFIRMED_FUNDS)) 0 else Constants.REQUIRED_CONFIRMATIONS))
                    var totalBalance: Long = 0
                    for (i in accounts.indices) {
                        if (util!!.getBoolean(Constants.HIDE_WALLET + accounts[i].accountNumber)) {
                            continue
                        }
                        totalBalance += accounts[i].balance.total
                    }
                    val finalTotalBalance = totalBalance
                    if (activity == null) {
                        return
                    }
                    activity!!.runOnUiThread { overview_av_balance.text = CoinFormat.format(Utils.formatDecredWithComma(finalTotalBalance) + " DCR") }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }.start()
    }

    private fun prepareHistoryData() {
        if (!isForeground) {
            needsUpdate = true
            return
        }

        if (activity == null) {
            return
        }

        if (swipe_refresh_layout2.isRefreshing) {
            return
        }

        getTransactions()
    }

    private fun getTransactions() {
        activity!!.runOnUiThread { swipe_refresh_layout2.isRefreshing = true }
        if (walletData!!.syncing) {
            no_history.setText(R.string.synchronizing)
            swipe_refresh_layout2.isRefreshing = false
            return
        }

        getBalance()
        hideSyncIndicator()

        object : Thread() {
            override fun run() {
                try {

                    val jsonResult = walletData!!.wallet.getTransactions(getMaxDisplayItems())

                    val gson = GsonBuilder().registerTypeHierarchyAdapter(ArrayList::class.java, Deserializer.TransactionDeserializer())
                            .create()

                    val transactions = gson.fromJson(jsonResult, Array<Transaction>::class.java)
                    if (transactions.isEmpty()) {
                        activity!!.runOnUiThread {
                            no_history.setText(R.string.no_transactions)
                            history_recycler_view2.visibility = View.GONE
                            swipe_refresh_layout2.isRefreshing = false
                        }
                    } else {
                        activity!!.runOnUiThread {
                            transactionList.clear()
                            transactionList.addAll(transactions)
                            val latestTx = Collections.min<Transaction>(transactionList, TransactionComparator.MinConfirmationSort())
                            latestTransactionHeight = latestTx.height + 1

                            val recentTransactionHash = util!!.get(Constants.RECENT_TRANSACTION_HASH)

                            if (recentTransactionHash.isNotEmpty()) {
                                val hashIndex = transactionList.find(recentTransactionHash)
                                if (hashIndex == -1) {
                                    // All transactions in this list is new
                                    transactionList.animateNewItems(0, transactionList.size - 1)
                                } else if (hashIndex != 0) {
                                    transactionList.animateNewItems(0, hashIndex - 1)
                                }
                            } else {
                                transactionList.animateNewItems(0, transactionList.size - 1)
                            }

                            util!!.set(Constants.RECENT_TRANSACTION_HASH, transactionList[0].hash)


                            val txNotificationHash = util!!.get(Constants.TX_NOTIFICATION_HASH)
                            println("Hash $txNotificationHash")

                            if (txNotificationHash.isNotEmpty() && txNotificationHash != transactionList[0].hash) {
                                val hashIndex = transactionList.find(txNotificationHash)
                                val format = DecimalFormat(getString(R.string.you_received) + " #.######## DCR")

                                if (hashIndex > 0) {
                                    println("Hash is $hashIndex")
                                    val subList = transactionList.subList(0, hashIndex)
                                    subList.forEach {
                                        if (it.direction == 1) {
                                            val satoshi = BigDecimal.valueOf(it.amount)

                                            val amount = satoshi.divide(BigDecimal.valueOf(1e8), MathContext(100))
                                            println("Sending Notifications for ${it.hash}")
                                            Utils.sendTransactionNotification(context, notificationManager, format.format(amount), it.totalInput.toInt() + it.totalOutput.toInt() + it.timestamp.toInt())
                                        } else {
                                            println("Not Sending Notifications for ${it.hash}")
                                        }
                                    }
                                } else if (hashIndex < 0) {
                                    println("Hash is less $hashIndex")
                                    val subList = transactionList.subList(0, transactionList.size - 1)
                                    subList.forEach {
                                        if (it.direction == 1) {
                                            val satoshi = BigDecimal.valueOf(it.amount)

                                            val amount = satoshi.divide(BigDecimal.valueOf(1e8), MathContext(100))
                                            Utils.sendTransactionNotification(context, notificationManager, format.format(amount), it.totalInput.toInt() + it.totalOutput.toInt() + it.timestamp.toInt())
                                        }
                                    }
                                }
                            }

                            println("First Hash: " + transactionList[0].hash)
                            util!!.set(Constants.TX_NOTIFICATION_HASH, transactionList[0].hash)

                            history_recycler_view2.visibility = View.VISIBLE

                            transactionAdapter!!.notifyDataSetChanged()
                            swipe_refresh_layout2.isRefreshing = false
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }.start()
    }

    override fun onRefresh() {
        getTransactions()
    }

    override fun onResume() {
        super.onResume()
        if (context != null) {
            val filter = IntentFilter(Constants.SYNCED)
            context!!.registerReceiver(receiver, filter)
        }

        if (walletData!!.syncing) {
            overview_sync_layout.visibility = View.VISIBLE
        } else {
            overview_sync_layout.visibility = View.GONE
        }

        isForeground = true
        if (needsUpdate) {
            needsUpdate = false
            prepareHistoryData()
        }
    }

    override fun onPause() {
        super.onPause()
        if (context != null) {
            context!!.unregisterReceiver(receiver)
        }
        isForeground = false
    }

    fun newTransaction(transaction: Transaction) {

        if (transactionList.find(transaction.hash) != -1) {
            // Transaction is a duplicate
            return
        }

        transaction.animate = true
        transactionList.add(0, transaction)
        if (transactionList.size > getMaxDisplayItems()) {
            transactionList.removeAt(transactionList.size - 1)
        }
        latestTransactionHeight = transaction.height + 1

        if (activity == null) {
            return
        }

        activity!!.runOnUiThread {
            history_recycler_view2.visibility = View.VISIBLE
            util!!.set(Constants.RECENT_TRANSACTION_HASH, transaction.hash)
            transactionAdapter!!.notifyDataSetChanged()
            getBalance()
        }
    }

    fun transactionConfirmed(hash: String, height: Int) {
        for (i in transactionList.indices) {
            if (transactionList[i].hash == hash) {
                val transaction = transactionList[i]
                transaction.height = height
                transaction.animate = false
                latestTransactionHeight = transaction.height + 1
                transactionList[i] = transaction
                transactionAdapter!!.notifyItemChanged(i)
                break
            }
        }

        getBalance()
    }

    fun blockAttached(height: Int) {
        if (height - latestTransactionHeight < 2) {
            for (i in transactionList.indices) {
                val tx = transactionList[i]
                if (height - tx.height >= 2) {
                    continue
                }
                tx.animate = false
                activity!!.runOnUiThread {
                    transactionAdapter!!.notifyItemChanged(i)
                }
            }
        }
    }

    private fun hideSyncIndicator() {
        (iv_sync_indicator.background as AnimationDrawable).stop()
        iv_sync_indicator.visibility = View.GONE
        overview_av_balance.visibility = View.VISIBLE
    }

    fun publishProgress() {
        if (activity != null) {
            activity!!.runOnUiThread {
                tv_synchronizing.setText(R.string.synchronizing)
                pb_sync_progress.visibility = View.VISIBLE
                pb_percent_complete.visibility = View.VISIBLE
                if (pb_status_layout.visibility == View.GONE) {
                    tap_for_more_info.visibility = View.VISIBLE
                }

                pb_sync_progress.progress = walletData!!.syncProgress.toInt()

                pb_percent_complete.text = Utils.getSyncTimeRemaining(walletData!!.syncRemainingTime, walletData!!.syncProgress.toInt(), false, context)

                pb_status.text = walletData!!.syncStatus

                pb_verbose_status.text = walletData!!.syncVerbose

                if (BuildConfig.IS_TESTNET) {
                    if (walletData!!.peers == 1) {
                        syncing_peers.text = getString(R.string.one_syncing_peer_testnet)
                    } else {
                        syncing_peers.text = getString(R.string.syncing_peers_testnet, walletData!!.peers)
                    }
                } else {
                    if (walletData!!.peers == 1) {
                        syncing_peers.text = getString(R.string.one_syncing_peer_mainnet)
                    } else {
                        syncing_peers.text = getString(R.string.syncing_peers_mainnet, walletData!!.peers)
                    }
                }
            }
        }
    }

    private var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != null && intent.action == Constants.SYNCED) {
                if (walletData!!.syncing) {
                    overview_sync_layout.visibility = View.VISIBLE
                    iv_sync_indicator.visibility = View.VISIBLE
                    overview_av_balance.visibility = View.GONE
                    iv_sync_indicator.post {
                        val syncAnimation = iv_sync_indicator.background as AnimationDrawable
                        syncAnimation.start()
                    }

                    tv_synchronizing.setText(R.string.starting_synchronization)

                    if (walletData!!.syncStatus != null) {
                        publishProgress()
                    }
                } else {
                    overview_sync_layout.visibility = View.GONE
                    pb_sync_progress.visibility = View.GONE
                    pb_percent_complete.visibility = View.GONE

                    getBalance()
                    hideSyncIndicator()
                    prepareHistoryData()
                }
            }
        }
    }

    private fun ArrayList<Transaction>.find(hash: String): Int {
        for (i in this.indices) {
            val item = this[i]
            if (item.hash == hash) {
                return i
            }
        }
        return -1
    }

    private fun ArrayList<Transaction>.animateNewItems(start: Int, count: Int) {
        for (i: Int in start..count) {
            val item = this[i]
            item.animate = true
            transactionAdapter!!.notifyItemChanged(i)
        }
    }

    private fun ArrayList<Transaction>.removeDuplicates(txList: ArrayList<Transaction>) {
        for (transaction in txList) {
            val index = this.find(transaction.hash)
            if (index != -1) {
                this.removeAt(index)
            }
        }
    }
}
