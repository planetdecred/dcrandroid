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
import com.dcrandroid.util.*
import dcrlibwallet.GetTransactionsResponse
import kotlinx.android.synthetic.main.content_overview.*
import kotlinx.android.synthetic.main.overview_sync_layout.*
import java.io.*
import java.math.BigDecimal
import java.math.MathContext
import java.text.DecimalFormat
import java.util.*

class OverviewFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener, GetTransactionsResponse {

    private var transactionAdapter: TransactionAdapter? = null
    private var util: PreferenceUtil? = null
    private var constants: WalletData? = null
    private val transactionList = ArrayList<TransactionsResponse.TransactionItem>()
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
        constants = WalletData.getInstance()

        swipe_refresh_layout2.setColorSchemeResources(R.color.colorPrimaryDarkBlue,
                R.color.colorPrimaryDarkBlue,
                R.color.colorPrimaryDarkBlue,
                R.color.colorPrimaryDarkBlue)
        swipe_refresh_layout2.setOnRefreshListener(this)
        transactionAdapter = TransactionAdapter(transactionList, context!!)
        iv_sync_indicator.setBackgroundResource(R.drawable.sync_animation)

        if (!constants!!.syncing) {
            iv_sync_indicator.post {
                val syncAnimation = iv_sync_indicator.background as AnimationDrawable
                syncAnimation.start()
            }

            pb_sync_progress.progress = 0
            overview_sync_layout.visibility = View.VISIBLE
            tv_synchronizing.setText(R.string.starting_synchronization)
            if(constants!!.syncStatus != null){
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
                extras.putLong(Constants.AMOUNT, history.getAmount())
                extras.putLong(Constants.FEE, history.getFee())
                extras.putLong(Constants.TIMESTAMP, history.getTimestamp())
                extras.putInt(Constants.HEIGHT, history.getHeight())
                extras.putLong(Constants.TOTAL_INPUT, history.totalInput)
                extras.putLong(Constants.TOTAL_OUTPUT, history.totalOutputs)
                extras.putString(Constants.TYPE, history.type)
                extras.putString(Constants.HASH, history.hash)
                extras.putString(Constants.RAW, history.raw)
                extras.putInt(Constants.DIRECTION, history.getDirection())
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
            pb_status.visibility = View.VISIBLE
            syncing_peers.visibility = View.VISIBLE
        }

        pb_status.setOnClickListener {
            it.visibility = View.GONE
            syncing_peers.visibility = View.GONE
            tap_for_more_info.visibility = View.VISIBLE
        }

        syncing_peers.setOnClickListener {
            it.visibility = View.GONE
            pb_status.visibility = View.GONE
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
        if (max < 1){
            return 5
        }


        return max
    }

    private fun getBalance() {
        if (constants!!.syncing) {
            return
        }

        object : Thread() {
            override fun run() {
                try {
                    if (context == null) {
                        return
                    }
                    val accounts = Account.parse(constants!!.wallet.getAccounts(if (util!!.getBoolean(Constants.SPEND_UNCONFIRMED_FUNDS)) 0 else Constants.REQUIRED_CONFIRMATIONS))
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

    private fun getTransactions(){
        activity!!.runOnUiThread { swipe_refresh_layout2.isRefreshing = true }
        transactionList.clear()
        loadTransactions()
        if (constants!!.syncing) {
            no_history.setText(R.string.synchronizing)
            println("Going back, Hiding swipe to refresh")
            swipe_refresh_layout2.isRefreshing = false
            return
        }
        getBalance()
        hideSyncIndicator()
        object : Thread() {
            override fun run() {
                try {
                    println("Getting transactions")
                    constants!!.wallet.getTransactions(this@OverviewFragment)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }.start()
    }

    private fun saveTransactions(transactions: ArrayList<TransactionsResponse.TransactionItem>) {
        try {
            if (activity == null || context == null) {
                return
            }

            val path = File(context!!.filesDir.toString() + "/" + BuildConfig.NetType + "/" + "savedata/")
            path.mkdirs()
            val file = File(path, "transactions")
            file.createNewFile()
            val objectOutputStream = ObjectOutputStream(FileOutputStream(file))
            objectOutputStream.writeObject(transactions)
            objectOutputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadTransactions() {
        try {
            if (activity == null || context == null) {
                return
            }

            val path = File(context!!.filesDir.toString() + "/" + BuildConfig.NetType + "/" + "savedata/")
            path.mkdirs()
            val file = File(path, "transactions")
            if (file.exists()) {
                val objectInputStream = ObjectInputStream(FileInputStream(file))
                val temp = objectInputStream.readObject() as ArrayList<TransactionsResponse.TransactionItem>
                if (temp.size > 0) {
                    if (temp.size > getMaxDisplayItems()) {
                        transactionList.addAll(temp.subList(0, getMaxDisplayItems()))
                    } else {
                        transactionList.addAll(temp)
                    }
                    val latestTx = Collections.min<TransactionsResponse.TransactionItem>(temp, TransactionComparator.MinConfirmationSort())
                    latestTransactionHeight = latestTx.getHeight() + 1
                }
                activity!!.runOnUiThread { transactionAdapter!!.notifyDataSetChanged() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (transactionList.size == 0) {
            no_history.setText(R.string.no_transactions)
            history_recycler_view2.visibility = View.GONE
        } else {
            history_recycler_view2.visibility = View.VISIBLE
        }
    }

    override fun onResult(json: String) {
        if (activity == null || context == null) {
            return
        }
        activity!!.runOnUiThread {
            println("Got response")
            if (swipe_refresh_layout2.isRefreshing) {
                println("Hiding Layout")
                swipe_refresh_layout2.isRefreshing = false
            }
            val response = TransactionsResponse.parse(json)
            if (response.transactions.size == 0) {
                no_history.setText(R.string.no_transactions)
                history_recycler_view2.visibility = View.GONE
            } else {
                val transactions = response.transactions
                Collections.sort<TransactionsResponse.TransactionItem>(transactions, TransactionComparator.TimestampSort())
                val latestTx = Collections.min<TransactionsResponse.TransactionItem>(transactions, TransactionComparator.MinConfirmationSort())
                latestTransactionHeight = latestTx.getHeight() + 1
                transactionList.clear()
                if (transactions.size > getMaxDisplayItems()) {
                    transactionList.addAll(transactions.subList(0, getMaxDisplayItems()))
                } else {
                    transactionList.addAll(transactions)
                }
                history_recycler_view2.visibility = View.VISIBLE

                transactionAdapter!!.notifyDataSetChanged()
                saveTransactions(transactions)

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

                if (txNotificationHash.isNotEmpty() && txNotificationHash != transactions[0].hash){
                    val hashIndex = transactions.find(txNotificationHash)
                    val format = DecimalFormat(getString(R.string.you_received) + " #.######## DCR")

                    if (hashIndex > 0){
                        println("Hash is $hashIndex")
                        val subList = transactions.subList(0, hashIndex)
                        subList.forEach {
                            if(it.direction == 1) {
                                val satoshi = BigDecimal.valueOf(it.amount)

                                val amount = satoshi.divide(BigDecimal.valueOf(1e8), MathContext(100))
                                println("Sending Notifications for ${it.hash}")
                                Utils.sendTransactionNotification(context, notificationManager, format.format(amount), it.totalInput.toInt() + it.totalOutputs.toInt() + it.timestamp.toInt())
                            }else{
                                println("Not Sending Notifications for ${it.hash}")
                            }
                        }
                    }else if (hashIndex < 0){
                        println("Hash is less $hashIndex")
                        val subList = transactions.subList(0, transactions.size - 1)
                        subList.forEach {
                            if (it.direction == 1) {
                                val satoshi = BigDecimal.valueOf(it.amount)

                                val amount = satoshi.divide(BigDecimal.valueOf(1e8), MathContext(100))
                                Utils.sendTransactionNotification(context, notificationManager, format.format(amount), it.totalInput.toInt() + it.totalOutputs.toInt() + it.timestamp.toInt())
                            }
                        }
                    }
                }

                println("First Hash: "+ transactions[0].hash)
                util!!.set(Constants.TX_NOTIFICATION_HASH, transactions[0].hash)

            }
        }
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

        if (constants!!.syncing) {
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

    fun newTransaction(transaction: TransactionsResponse.TransactionItem) {

        if (transactionList.find(transaction.hash) != -1) {
            // Transaction is a duplicate
            return
        }

        transaction.animate = true
        transactionList.add(0, transaction)
        if (transactionList.size > getMaxDisplayItems()) {
            transactionList.removeAt(transactionList.size - 1)
        }
        latestTransactionHeight = transaction.getHeight() + 1

        if (activity == null) {
            return
        }

        activity!!.runOnUiThread {
            history_recycler_view2.visibility = View.VISIBLE
            util!!.set(Constants.RECENT_TRANSACTION_HASH, transaction.hash)
            transactionAdapter!!.notifyDataSetChanged()
            saveTransactions(transactionList)
            getBalance()
        }
    }

    fun transactionConfirmed(hash: String, height: Int) {
        for (i in transactionList.indices) {
            if (transactionList[i].hash == hash) {
                val transaction = transactionList[i]
                transaction.height = height
                transaction.animate = false
                latestTransactionHeight = transaction.getHeight() + 1
                transactionList[i] = transaction
                transactionAdapter!!.notifyItemChanged(i)
                break
            }
        }

        saveTransactions(transactionList)
        getBalance()
    }

    fun blockAttached(height: Int) {
        if (height - latestTransactionHeight < 2) {
            for (i in transactionList.indices) {
                val tx = transactionList[i]
                if (height - tx.getHeight() >= 2) {
                    continue
                }
                tx.animate = false
                activity!!.runOnUiThread {
                    transactionAdapter!!.notifyItemChanged(i)
                    saveTransactions(transactionList)
                }
            }
        }
    }

    private fun hideSyncIndicator() {
        (iv_sync_indicator.background as AnimationDrawable).stop()
        iv_sync_indicator.visibility = View.GONE
        overview_av_balance.visibility = View.VISIBLE
    }

    fun publishProgress(){
        if(activity != null) {
            activity!!.runOnUiThread {
                tv_synchronizing.setText(R.string.synchronizing)
                pb_sync_progress.visibility = View.VISIBLE
                pb_percent_complete.visibility = View.VISIBLE
                if (pb_status.visibility == View.GONE) {
                    tap_for_more_info.visibility = View.VISIBLE
                }

                pb_sync_progress.progress = constants!!.syncProgress.toInt()

                pb_percent_complete.text = Utils.getTimeRemaining(constants!!.syncRemainingTime, constants!!.syncProgress.toInt(), false, context)

                pb_status.text = constants!!.syncStatus

                if (BuildConfig.IS_TESTNET) {
                    if (constants!!.peers == 1) {
                        syncing_peers.text = getString(R.string.one_syncing_peer_testnet)
                    } else {
                        syncing_peers.text = getString(R.string.syncing_peers_testnet, constants!!.peers)
                    }
                } else {
                    if (constants!!.peers == 1) {
                        syncing_peers.text = getString(R.string.one_syncing_peer_mainnet)
                    } else {
                        syncing_peers.text = getString(R.string.syncing_peers_mainnet, constants!!.peers)
                    }
                }
            }
        }
    }

    private var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != null && intent.action == Constants.SYNCED) {
                if (constants!!.syncing) {
                    overview_sync_layout.visibility = View.VISIBLE
                    iv_sync_indicator.visibility = View.VISIBLE
                    overview_av_balance.visibility = View.GONE
                    iv_sync_indicator.post {
                        val syncAnimation = iv_sync_indicator.background as AnimationDrawable
                        syncAnimation.start()
                    }

                    tv_synchronizing.setText(R.string.starting_synchronization)

                    if(constants!!.syncStatus != null){
                        publishProgress()
                    }
                }else {
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

    private fun ArrayList<TransactionsResponse.TransactionItem>.find(hash: String): Int {
        for (i in this.indices) {
            val item = this[i]
            if (item.hash == hash) {
                return i
            }
        }
        return -1
    }

    private fun ArrayList<TransactionsResponse.TransactionItem>.animateNewItems(start: Int, count: Int) {
        for (i: Int in start..count) {
            val item = this[i]
            item.animate = true
            transactionAdapter!!.notifyItemChanged(i)
        }
    }
}
