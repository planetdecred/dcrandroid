/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.fragments

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.core.text.HtmlCompat
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
import kotlinx.android.synthetic.main.toolbar_layout.*
import kotlinx.android.synthetic.main.transactions_overview_layout.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal


const val MAX_TRANSACTIONS = 3

class OverviewFragment : BaseFragment(), ViewTreeObserver.OnScrollChangedListener,
        AccountMixerNotificationListener, GetExchangeRate.ExchangeRateCallback {

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
    private val gson = GsonBuilder().registerTypeHierarchyAdapter(
            ArrayList::class.java,
            Deserializer.TransactionDeserializer()
    )
            .create()

    private lateinit var scrollView: NestedScrollView
    private lateinit var recyclerView: RecyclerView

    private lateinit var balanceTextView: TextView
    private lateinit var usdBalanceTextView: TextView
    internal lateinit var noTransactionsTextView: TextView
    private lateinit var toolbarTitle: TextView
    private lateinit var toolbarSubtitle: TextView
    internal lateinit var transactionsLayout: LinearLayout
    private lateinit var ivConcealReveal: ImageView
    private lateinit var toolbarConcealReveal: ImageView

    var exchangeDecimal: BigDecimal? = null

    private lateinit var syncLayout: LinearLayout
    private var syncLayoutUtil: SyncLayoutUtil? = null

    private var isBalanceHidden = false

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_overview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        scrollView = view.findViewById(R.id.scroll_view_overview)
        recyclerView = view.findViewById(R.id.rv_transactions)

        balanceTextView = view.findViewById(R.id.tv_visible_wallet_balance)
        usdBalanceTextView = view.findViewById(R.id.tv_visible_usd_wallet_balance)
        noTransactionsTextView = view.findViewById(R.id.tv_no_transactions)
        transactionsLayout = view.findViewById(R.id.transactions_view)
        ivConcealReveal = view.findViewById(R.id.iv_conceal_reveal)

        val toolbar: Toolbar = view.findViewById(R.id.toolbar)
        toolbarTitle = toolbar.findViewById(R.id.toolbar_title)
        toolbarSubtitle = view.findViewById(R.id.toolbar_subtitle)
        toolbarConcealReveal = view.findViewById(R.id.toolbar_right_icon)

        syncLayout = view.findViewById(R.id.sync_layout)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        adapter = TransactionListAdapter(requireContext(), transactions)

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.isNestedScrollingEnabled = false
        recyclerView.setDivider(R.drawable.recycler_view_divider_pad_56)
        recyclerView.adapter = adapter

        setToolbarTitle(R.string.overview, false)
        scrollView.viewTreeObserver.addOnScrollChangedListener(this)

        isBalanceHidden = multiWallet!!.readBoolConfigValueForKey(Constants.SHOW_HIDE_BALANCE, false)

        loadBalance()
        loadTransactions()

        btn_view_all_transactions.setOnClickListener {
            switchFragment(1) // Transactions Fragment
        }

        setupLogoAnim()

        if (multiWallet!!.numWalletsNeedingSeedBackup() > 0 && !closedBackupWarning) {
            backup_warning_layout?.show()

            backup_warning_title?.text = getString(R.string.wallets_need_backup)

            backup_warning_layout.go_to_wallets_btn.setOnClickListener {
                switchFragment(2) // Wallets fragment
            }

            iv_close_backup_warning?.setOnClickListener {
                InfoDialog(requireContext())
                        .setMessage(getString(R.string.close_backup_warning_dialog_message))
                        .setPositiveButton(
                                getString(R.string.got_it)
                        ) { _, _ ->
                            closedBackupWarning = true
                            backup_warning_layout?.hide()
                        }
                        .show()
            }
        }

        if (!multiWallet!!.readBoolConfigValueForKey(Constants.HAS_SETUP_PRIVACY, false)
                && multiWallet!!.fullCoinWalletsList().size > 0 && !closedPrivacyReminder
        ) {
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

        ivConcealReveal.setOnClickListener {
            isBalanceHidden = !isBalanceHidden
            multiWallet?.setBoolConfigValueForKey(Constants.SHOW_HIDE_BALANCE, isBalanceHidden)
            loadBalance()
        }

        toolbar_right_icon.setOnClickListener {
            isBalanceHidden = !isBalanceHidden
            multiWallet?.setBoolConfigValueForKey(Constants.SHOW_HIDE_BALANCE, isBalanceHidden)
            loadBalance()
        }

        if (!isBalanceHidden) {
            fetchExchangeRate()
        }
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
            tv_mixer_running.text = requireContext().resources.getQuantityString(
                    R.plurals.mixer_is_running,
                    activeMixers,
                    activeMixers
            )
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
        multiWallet!!.removeAccountMixerNotificationListener(this.javaClass.name)
        multiWallet!!.addAccountMixerNotificationListener(this, this.javaClass.name)
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
        multiWallet!!.removeAccountMixerNotificationListener(this.javaClass.name)
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
        val totalBalanceAtom = multiWallet!!.totalWalletBalance()
        val totalBalanceCoin = Dcrlibwallet.amountCoin(totalBalanceAtom)

        if (mainBalanceIsVisible()) {
            toolbar_right_icon.visibility = View.VISIBLE
            if (isBalanceHidden) {
                setToolbarTitle(Constants.HIDDEN_BALANCE_TEXT, true)
                toolbarConcealReveal.setImageResource(R.drawable.ic_conceal)
            } else {
                setToolbarTitle(CoinFormat.format(multiWallet!!.totalWalletBalance(), 0.7f), true)
                toolbarConcealReveal.setImageResource(R.drawable.ic_reveal)
                if (exchangeDecimal != null) {
                    val formattedUSD = HtmlCompat.fromHtml(
                            getString(
                                    R.string.usd_symbol_format,
                                    CurrencyUtil.dcrToFormattedUSD(exchangeDecimal, totalBalanceCoin, 2)
                            ), 0
                    )
                    setToolbarSubTitle(formattedUSD)
                }
            }

        } else {
            toolbarConcealReveal.visibility = View.GONE
            setToolbarTitle(R.string.overview, false)
            setToolbarSubTitle("")
        }
    }

    private fun loadBalance() = GlobalScope.launch(Dispatchers.Main) {
        if (isBalanceHidden) {
            usdBalanceTextView.hide()
            balanceTextView.text = Constants.HIDDEN_BALANCE_TEXT
            ivConcealReveal.setImageResource(R.drawable.ic_conceal)
            toolbarConcealReveal.setImageResource(R.drawable.ic_conceal)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ivConcealReveal.tooltipText = context?.getString(R.string.show_balance)
                toolbarConcealReveal.tooltipText = context?.getString(R.string.show_balance)
            }
        } else {
            fetchExchangeRate()
            balanceTextView.text = CoinFormat.format(multiWallet!!.totalWalletBalance(), 0.5f)
            ivConcealReveal.setImageResource(R.drawable.ic_reveal)
            toolbarConcealReveal.setImageResource(R.drawable.ic_reveal)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ivConcealReveal.tooltipText = context?.getString(R.string.hide_balance)
                toolbarConcealReveal.tooltipText = context?.getString(R.string.hide_balance)
            }
        }
        val totalBalanceAtom = multiWallet!!.totalWalletBalance()
        val totalBalanceCoin = Dcrlibwallet.amountCoin(totalBalanceAtom)

        if (mainBalanceIsVisible()) {
            if (isBalanceHidden) {
                setToolbarTitle(Constants.HIDDEN_BALANCE_TEXT, true)
                setToolbarSubTitle("")
            } else {
                setToolbarTitle(CoinFormat.format(multiWallet!!.totalWalletBalance(), 0.7f), true)
                if (exchangeDecimal != null) {
                    val formattedUSD = HtmlCompat.fromHtml(
                            getString(
                                    R.string.usd_symbol_format,
                                    CurrencyUtil.dcrToFormattedUSD(exchangeDecimal, totalBalanceCoin, 2)
                            ), 0
                    )
                    setToolbarSubTitle(formattedUSD)
                }
            }
        }
    }

    private fun loadTransactions() = GlobalScope.launch(Dispatchers.Default) {
        val jsonResult =
                multiWallet!!.getTransactions(0, MAX_TRANSACTIONS, Dcrlibwallet.TxFilterAll, true)
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

    fun setToolbarTitle(title: CharSequence, showShadow: Boolean) {
        toolbarTitle.text = title
        app_bar.elevation = if (showShadow) {
            resources.getDimension(R.dimen.app_bar_elevation)
        } else {
            0f
        }
    }

    fun setToolbarTitle(@StringRes title: Int, showShadow: Boolean) {
        if (context != null) {
            setToolbarTitle(requireContext().getString(title), showShadow)
        }
    }

    fun setToolbarSubTitle(subtitle: CharSequence) {
        if (subtitle == "") {
            toolbarSubtitle.visibility = View.GONE
        } else {
            toolbarSubtitle.visibility = View.VISIBLE
            toolbarSubtitle.text = subtitle
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
        SnackBar.showText(requireContext(), R.string.mixer_has_stopped_running)
    }

    override fun onAccountMixerStarted(walletID: Long) {
        setMixerStatus()
    }

    private fun isExchangeEnabled(): Boolean {
        val currencyConversion = multiWallet!!.readInt32ConfigValueForKey(
                Dcrlibwallet.CurrencyConversionConfigKey,
                Constants.DEF_CURRENCY_CONVERSION
        )

        return currencyConversion > 0
    }

    private fun fetchExchangeRate() {
        if (!isExchangeEnabled()) {
            return
        }

        println("Getting exchange rate")
        val userAgent = multiWallet!!.readStringConfigValueForKey(Dcrlibwallet.UserAgentConfigKey)
        GetExchangeRate(userAgent, this).execute()
    }

    override fun onExchangeRateSuccess(rate: GetExchangeRate.BittrexRateParser) {
        exchangeDecimal = rate.usdRate

        val totalBalanceAtom = multiWallet!!.totalWalletBalance()
        val totalBalanceCoin = Dcrlibwallet.amountCoin(totalBalanceAtom)
        if (isAdded) {
            val formattedUSD = HtmlCompat.fromHtml(
                    getString(
                            R.string.usd_symbol_format,
                            CurrencyUtil.dcrToFormattedUSD(exchangeDecimal, totalBalanceCoin, 2)
                    ), 0
            )

            GlobalScope.launch(Dispatchers.Main) {
                if (!isBalanceHidden) {
                    usdBalanceTextView.text = formattedUSD
                    usdBalanceTextView.show()
                }
            }
        }
    }

    override fun onExchangeRateError(e: Exception) {
        e.printStackTrace()

        GlobalScope.launch(Dispatchers.Main) {
            usdBalanceTextView.hide()
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

@SuppressLint("ClickableViewAccessibility")
private fun OverviewFragment.setupLogoAnim() {
    val runnable = Runnable {
        val anim = AnimationUtils.loadAnimation(context, R.anim.logo_anim)
        home_logo.startAnimation(anim)
    }

    val handler = Handler()
    toolbar_title.setOnTouchListener { _, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> handler.postDelayed(runnable, 10000)
            MotionEvent.ACTION_UP -> handler.removeCallbacks(runnable)
        }
        true
    }
}