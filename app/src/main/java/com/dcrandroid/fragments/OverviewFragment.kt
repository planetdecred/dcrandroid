package com.dcrandroid.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.AnimationDrawable
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import com.dcrandroid.BuildConfig
import com.dcrandroid.MainActivity
import com.dcrandroid.R
import com.dcrandroid.activities.TransactionDetailsActivity
import com.dcrandroid.adapter.TransactionAdapter
import com.dcrandroid.data.Account
import com.dcrandroid.data.Constants
import com.dcrandroid.util.*
import kotlinx.android.synthetic.main.content_overview.*
import mobilewallet.GetTransactionsResponse
import java.io.*
import java.util.*

class OverviewFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener, GetTransactionsResponse {

    private var transactionAdapter: TransactionAdapter? = null
    private var util: PreferenceUtil? = null
    private var constants: DcrConstants? = null
    private val transactionList = ArrayList<TransactionsResponse.TransactionItem>()
    private var recyclerViewHeight: Int = 0
    private var latestTransactionHeight: Int = 0
    private var needsUpdate = false
    private var isForeground: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.content_overview, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (activity != null) {
            activity!!.title = getString(R.string.overview)
        }

        util = PreferenceUtil(context!!)
        constants = DcrConstants.getInstance()

        swipe_refresh_layout2.setColorSchemeResources(R.color.colorPrimaryDarkBlue,
                R.color.colorPrimaryDarkBlue,
                R.color.colorPrimaryDarkBlue,
                R.color.colorPrimaryDarkBlue)
        swipe_refresh_layout2.setOnRefreshListener(this)
        transactionAdapter = TransactionAdapter(transactionList, context!!)
        iv_sync_indicator.setBackgroundResource(R.drawable.sync_animation)

        if (!constants!!.synced) {
            iv_sync_indicator.post {
                val syncAnimation = iv_sync_indicator.background as AnimationDrawable
                syncAnimation.start()
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
            if (activity != null && activity is MainActivity) {
                val mainActivity = activity as MainActivity?
                mainActivity!!.displayHistory()
            }
        }

        send.setOnClickListener {
            if (activity != null && activity is MainActivity) {
                val mainActivity = activity as MainActivity?
                mainActivity!!.displaySend()
            }
        }

        receive.setOnClickListener {
            if (activity != null && activity is MainActivity) {
                val mainActivity = activity as MainActivity?
                mainActivity!!.displayReceive()
            }
        }

        val vto = history_recycler_view2.viewTreeObserver
        if (vto.isAlive) {
            vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    recyclerViewHeight = history_recycler_view2.height
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

    private fun getMaxDisplayItems(): Int {
        if (activity == null) {
            return 0
        }
        val px = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 52f, activity!!.resources.displayMetrics))
        return recyclerViewHeight / px
    }

    private fun getBalance() {
        if (!constants!!.synced) {
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
        activity!!.runOnUiThread { swipe_refresh_layout2.isRefreshing = true }
        transactionList.clear()
        loadTransactions()
        if (!constants!!.synced) {
            no_history.setText(R.string.no_transactions_sync)
            swipe_refresh_layout2.isRefreshing = false
            return
        }
        getBalance()
        hideSyncIndicator()
        object : Thread() {
            override fun run() {
                try {
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
            val response = TransactionsResponse.parse(json)
            if (response.transactions.size == 0) {
                no_history.setText(R.string.no_transactions)
                history_recycler_view2.visibility = View.GONE
                if (swipe_refresh_layout2.isRefreshing) {
                    swipe_refresh_layout2.isRefreshing = false
                }
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
                if (swipe_refresh_layout2.isRefreshing) {
                    swipe_refresh_layout2.isRefreshing = false
                }
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

            }
        }
    }

    override fun onRefresh() {
        getBalance()
        prepareHistoryData()
    }

    override fun onResume() {
        super.onResume()
        if (context != null) {
            val filter = IntentFilter(Constants.SYNCED)
            context!!.registerReceiver(receiver, filter)
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

    private var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != null && intent.action == Constants.SYNCED) {
                if (constants!!.synced) {
                    getBalance()
                    hideSyncIndicator()
                    prepareHistoryData()
                } else {
                    iv_sync_indicator.visibility = View.VISIBLE
                    overview_av_balance.visibility = View.GONE
                    iv_sync_indicator.post {
                        val syncAnimation = iv_sync_indicator.background as AnimationDrawable
                        syncAnimation.start()
                    }
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
