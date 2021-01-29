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
import com.dcrandroid.adapter.MixerStatusAdapter
import com.dcrandroid.adapter.TransactionListAdapter
import com.dcrandroid.data.Constants
import com.dcrandroid.data.Transaction
import com.dcrandroid.dialog.InfoDialog
import com.dcrandroid.extensions.*
import com.dcrandroid.util.*
import com.google.gson.GsonBuilder
import dcrlibwallet.AccountMixerNotificationListener
import dcrlibwallet.Dcrlibwallet
import kotlinx.android.synthetic.main.overview_backup_warning.*
import kotlinx.android.synthetic.main.overview_backup_warning.view.*
import kotlinx.android.synthetic.main.overview_mixer_status_card.*
import kotlinx.android.synthetic.main.overview_privacy_introduction.*
import kotlinx.android.synthetic.main.transactions_overview_layout.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val MAX_TRANSACTIONS = 3

class OverviewFragment : BaseFragment(), ViewTreeObserver.OnScrollChangedListener, AccountMixerNotificationListener {

    companion object {
        private var closedBackupWarning = false
        private var closedPrivacyReminder = false
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
        recyclerView.setDivider(R.drawable.recycler_view_divider_pad_56)
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

            backup_warning_layout.go_to_wallets_btn.setOnClickListener {
                switchFragment(2) // Wallets fragment
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

        if (!WalletData.multiWallet!!.readBoolConfigValueForKey(Constants.HAS_SETUP_PRIVACY, false) && !closedPrivacyReminder) {
            privacy_intro_card.show()
            btn_dismiss_privacy_intro.setOnClickListener {
                closedPrivacyReminder = true
                privacy_intro_card.hide()
            }

            btn_setup_mixer.setOnClickListener {
                multiWallet!!.setBoolConfigValueForKey(Constants.SHOWN_PRIVACY_POPUP, false)
                switchFragment(2)
            }
        }

        mixer_status_rv.layoutManager = LinearLayoutManager(context)
        mixer_status_rv.adapter = MixerStatusAdapter()
        setMixerStatus()
        multiWallet?.setAccountMixerNotification(this)
    }

    private fun setMixerStatus() = GlobalScope.launch(Dispatchers.Main) {

        if (!isForeground) {
            return@launch
        }

        var activeMixers = 0
        for (wallet in multiWallet!!.openedWalletsList()) {
            if (wallet.isAccountMixerActive) {
                activeMixers++
            }
        }

        if (activeMixers > 0) {
            tv_mixer_running.text = context!!.resources.getQuantityString(R.plurals.mixer_is_running, activeMixers, activeMixers)
            cspp_running_layout.show()
            mixer_status_rv.adapter?.notifyDataSetChanged()
        } else {
            cspp_running_layout.hide()
        }

        mixer_go_to_wallets.setOnClickListener {
            switchFragment(2)
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
            setToolbarTitle(CoinFormat.format(multiWallet!!.totalWalletBalance(), 0.7f), true)
        } else {
            setToolbarTitle(R.string.overview, false)
        }
    }

    private fun loadBalance() = GlobalScope.launch(Dispatchers.Main) {
        balanceTextView.text = CoinFormat.format(multiWallet!!.totalWalletBalance(), 0.5f)
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
        setMixerStatus()
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

        setMixerStatus()
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

    override fun onSyncCompleted() {
        super.onSyncCompleted()
        setMixerStatus()
    }

    override fun onAccountMixerEnded(walletID: Long) {
        setMixerStatus()
        SnackBar.showText(context!!, R.string.mixer_has_stopped_running)
    }

    override fun onAccountMixerStarted(walletID: Long) {
        setMixerStatus()
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