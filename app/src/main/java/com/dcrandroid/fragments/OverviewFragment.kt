/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.fragments

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import com.dcrandroid.adapter.TransactionListAdapter
import com.dcrandroid.data.Transaction
import com.dcrandroid.dialog.InfoDialog
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.show
import com.dcrandroid.extensions.totalWalletBalance
import com.dcrandroid.util.CoinFormat
import com.dcrandroid.util.Deserializer
import com.dcrandroid.util.NetworkUtil
import com.dcrandroid.util.SyncLayoutUtil
import com.google.gson.GsonBuilder
import dcrlibwallet.Dcrlibwallet
import kotlinx.android.synthetic.main.overview_backup_warning.*
import kotlinx.android.synthetic.main.transactions_overview_layout.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val MAX_TRANSACTIONS = 3

class OverviewFragment : BaseFragment(), ViewTreeObserver.OnScrollChangedListener {

    companion object {
        private var closedBackupWarning = false
        const val FRAGMENT_POSITION = 0

        // Tx hash received during this session is saved here.
        // Not all new tx hashes will be present here,
        // only the ones that came in while Overview page is visible.
        private val newTxHashes = ArrayList<String>()
    }

    private val transactions: ArrayList<Transaction> = ArrayList()
    private var adapter: TransactionListAdapter? = null
    private val gson = GsonBuilder().registerTypeHierarchyAdapter(ArrayList::class.java, Deserializer.TransactionDeserializer())
            .create()

    private lateinit var scrollView: NestedScrollView
    private lateinit var recyclerView: RecyclerView

    private lateinit var balanceTextView: TextView
    internal lateinit var noTransactionsTextView: TextView
    internal lateinit var transactionsLayout: LinearLayout

    private lateinit var syncLayout: LinearLayout
    private var syncLayoutUtil: SyncLayoutUtil? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_overview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        scrollView = view.findViewById(R.id.scroll_view_overview)
        recyclerView = view.findViewById(R.id.rv_transactions)

        balanceTextView = view.findViewById(R.id.tv_visible_wallet_balance)
        noTransactionsTextView = view.findViewById(R.id.tv_no_transactions)
        transactionsLayout = view.findViewById(R.id.transactions_view)

        syncLayout = view.findViewById(R.id.sync_layout)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        adapter = TransactionListAdapter(context!!, transactions)

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.isNestedScrollingEnabled = false
        recyclerView.adapter = adapter

        setToolbarTitle(R.string.overview, false)
        scrollView.viewTreeObserver.addOnScrollChangedListener(this)

        loadBalance()
        loadTransactions()

        btn_view_all_transactions.setOnClickListener {
            switchFragment(1) // Transactions Fragment
        }

        if (multiWallet!!.numWalletsNeedingSeedBackup() > 0 && !closedBackupWarning) {
            backup_warning_layout?.show()

            backup_warning_title?.text = when (multiWallet!!.numWalletsNeedingSeedBackup()) {
                1 -> getString(R.string.a_wallet_needs_backup)
                else -> getString(R.string.n_wallets_need_backup, multiWallet!!.numWalletsNeedingSeedBackup())
            }

            go_to_wallets_btn?.setOnClickListener {
                switchFragment(2) // Wallets Fragment
            }

            iv_close_backup_warning?.setOnClickListener {
                InfoDialog(context!!)
                        .setMessage(getString(R.string.close_backup_warning_dialog_message))
                        .setPositiveButton(getString(R.string.got_it), DialogInterface.OnClickListener { _, _ ->
                            closedBackupWarning = true
                            backup_warning_layout?.hide()
                        })
                        .show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        syncLayoutUtil = SyncLayoutUtil(syncLayout, { restartSyncProcess() }, {
            if (multiWallet!!.isSyncing) {
                scrollView.postDelayed({
                    scrollView.smoothScrollTo(0, scrollView.bottom)
                }, 200)
            }
        })
    }

    override fun onPause() {
        super.onPause()
        syncLayoutUtil?.destroy()
        syncLayoutUtil = null
    }

    private fun mainBalanceIsVisible(): Boolean {
        val scrollY = this.scrollView.scrollY
        val textSize = context?.resources?.getDimension(R.dimen.visible_balance_text_size)

        if (textSize != null) {
            if (scrollY > textSize / 2) {
                return true
            }
        }

        return false
    }

    override fun onScrollChanged() {
        if (mainBalanceIsVisible()) {
            setToolbarTitle(balanceTextView.text, true)
        } else {
            setToolbarTitle(R.string.overview, false)
        }
    }

    private fun loadBalance() = GlobalScope.launch(Dispatchers.Main) {
        balanceTextView.text = CoinFormat.format(multiWallet!!.totalWalletBalance())
        if (mainBalanceIsVisible()) {
            setToolbarTitle(balanceTextView.text, true)
        }
    }

    private fun loadTransactions() = GlobalScope.launch(Dispatchers.Default) {
        val jsonResult = multiWallet!!.getTransactions(0, MAX_TRANSACTIONS, Dcrlibwallet.TxFilterRegular, true)
        var tempTxList = gson.fromJson(jsonResult, Array<Transaction>::class.java)

        if (tempTxList == null) {
            tempTxList = arrayOf()
        } else {

            transactions.let {
                it.clear()
                it.addAll(tempTxList)
            }
            withContext(Dispatchers.Main) {
                adapter?.notifyDataSetChanged()
            }
        }

        if (transactions.size > 0) {
            showTransactionList()
        } else {
            hideTransactionList()
        }
    }

    override fun onTxOrBalanceUpdateRequired(walletID: Long?) {
        loadBalance()
        loadTransactions()
    }

    private fun isOffline(): Boolean {

        val isWifiConnected = context?.let { NetworkUtil.isWifiConnected(it) }
        val isDataConnected = context?.let { NetworkUtil.isMobileDataConnected(it) }
        val syncAnyways = multiWallet!!.readBoolConfigValueForKey(Dcrlibwallet.SyncOnCellularConfigKey, false)

        // 1. If the user chooses not to sync(means the user syncs only on wifi and wifi is off or the user tapped NO on the dialog)
        if (!isWifiConnected!! && !syncAnyways) {
            return true
        }

        // 2. If the wifi is off, user chose sync anyways but mobile data isn’t activated, offline
        if (!isWifiConnected && syncAnyways && !isDataConnected!!) {
            return true
        }

        return false
    }

    override fun onTransaction(transactionJson: String?) {
        if (!isForeground) {
            requiresDataUpdate = true
            return
        }
        loadBalance()

        val transaction = gson.fromJson(transactionJson, Transaction::class.java)
        if (newTxHashes.indexOf(transaction.hash) == -1) {
            newTxHashes.add(transaction.hash)
            transaction.animate = true

            GlobalScope.launch(Dispatchers.Main) {
                transactions.add(0, transaction)

                // remove last item if more than max
                if (transactions.size > MAX_TRANSACTIONS) {
                    transactions.removeAt(transactions.size - 1)
                }

                adapter?.notifyDataSetChanged()
            }
        }
    }

    override fun onTransactionConfirmed(walletID: Long, hash: String, blockHeight: Int) {
        if (!isForeground) {
            requiresDataUpdate = true
            return
        }
        loadBalance()

        GlobalScope.launch(Dispatchers.Main) {

            for (i in 0 until transactions.size) {
                if (transactions[i].hash == hash && transactions[i].walletID == walletID) {
                    transactions[i].height = blockHeight
                    adapter?.notifyItemChanged(i)
                }
            }
        }
    }

    override fun onBlockAttached(walletID: Long, blockHeight: Int) {
        if (!isForeground) {
            requiresDataUpdate = true
            return
        }
        loadBalance()

        GlobalScope.launch(Dispatchers.Main) {
            val unconfirmedTransactions = transactions.filter { it.confirmations <= 2 }.count()
            if (unconfirmedTransactions > 0) {
                adapter?.notifyDataSetChanged()
            }
        }
    }
}

fun OverviewFragment.showTransactionList() = GlobalScope.launch(Dispatchers.Main) {
    noTransactionsTextView.hide()
    transactionsLayout.show()
}

fun OverviewFragment.hideTransactionList() = GlobalScope.launch(Dispatchers.Main) {
    noTransactionsTextView.show()
    transactionsLayout.hide()
}