/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.dcrandroid.R
import com.dcrandroid.adapter.TransactionPageAdapter
import com.dcrandroid.data.Transaction
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.show
import com.dcrandroid.util.Deserializer
import com.google.gson.GsonBuilder
import dcrlibwallet.Dcrlibwallet
import dcrlibwallet.Wallet
import kotlinx.android.synthetic.main.single_wallet_transactions_page.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class TransactionsFragment : BaseFragment(), AdapterView.OnItemSelectedListener, ViewTreeObserver.OnScrollChangedListener {

    private var loadedAll = false
    private val loading = AtomicBoolean(false)
    private val initialLoadingDone = AtomicBoolean(false)

    private var wallet: Wallet? = null

    private var layoutManager: LinearLayoutManager? = null
    private val transactions: ArrayList<Transaction> = ArrayList()
    private var adapter: TransactionPageAdapter? = null
    private val gson = GsonBuilder().registerTypeHierarchyAdapter(ArrayList::class.java, Deserializer.TransactionDeserializer())
            .create()

    private var newestTransactionsFirst = true
    private var txFilter = Dcrlibwallet.TxFilterAll
    private val availableTxTypes = ArrayList<String>()

    private var txTypeSortAdapter: ArrayAdapter<String>? = null

    companion object {
        // Tx hash received during this session is saved here.
        // Not all new tx hashes will be present here,
        // only the ones that came in while Transactions page is visible.
        private val newTxHashes = ArrayList<String>()
    }

    fun setWalletID(walletID: Long): TransactionsFragment {
        wallet = multiWallet!!.walletWithID(walletID)
        TAG = wallet!!.name
        return this
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.single_wallet_transactions_page, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (multiWallet!!.openedWalletsCount() == 1) {
            setToolbarTitle(R.string.transactions, false)
        }

        adapter = TransactionPageAdapter(context!!, wallet!!.id, transactions)

        txTypeSortAdapter = ArrayAdapter(context!!, android.R.layout.simple_spinner_item, availableTxTypes)
        txTypeSortAdapter!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        tx_type_spinner.adapter = txTypeSortAdapter
        tx_type_spinner.onItemSelectedListener = this

        layoutManager = LinearLayoutManager(context)
        recycler_view.layoutManager = layoutManager
        recycler_view.adapter = adapter
        recycler_view.viewTreeObserver.addOnScrollChangedListener(this)

        initAdapter()

    }

    override fun onScrollChanged() {
        if (context == null || transactions.size < 5 || !initialLoadingDone.get()) return

        val firstVisibleItem = layoutManager!!.findFirstCompletelyVisibleItemPosition()
        transactions_page_header.elevation = if (firstVisibleItem != 0) resources.getDimension(R.dimen.app_bar_elevation)
        else 0f

        val lastVisibleItem = layoutManager!!.findLastCompletelyVisibleItemPosition()
        if (lastVisibleItem >= transactions.size - 5) {
            if (!loadedAll) {
                recycler_view.stopScroll()
                loadTransactions(loadMore = true)
            }
        }
    }

    private fun initAdapter() {

        val timestampSortItems = context!!.resources.getStringArray(R.array.timestamp_sort)
        val timestampSortAdapter = ArrayAdapter(context!!, android.R.layout.simple_spinner_item, timestampSortItems)
        timestampSortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        timestamp_sort_spinner.onItemSelectedListener = this
        timestamp_sort_spinner.adapter = timestampSortAdapter

        refreshAvailableTxType()

        loadTransactions()
    }

    private fun refreshAvailableTxType() = GlobalScope.launch(Dispatchers.Default) {
        availableTxTypes.clear()

        val txCount = wallet!!.countTransactions(Dcrlibwallet.TxFilterAll)
        val sentTxCount = wallet!!.countTransactions(Dcrlibwallet.TxFilterSent)
        val receivedTxCount = wallet!!.countTransactions(Dcrlibwallet.TxFilterReceived)
        val transferredTxCount = wallet!!.countTransactions(Dcrlibwallet.TxFilterTransferred)
        val stakingTxCount = wallet!!.countTransactions(Dcrlibwallet.TxFilterStaking)
        val coinbaseTxCount = wallet!!.countTransactions(Dcrlibwallet.TxFilterCoinBase)

        withContext(Dispatchers.Main) {
            if (context == null) {
                return@withContext
            }

            availableTxTypes.add(context!!.getString(R.string.tx_sort_all, txCount))
            availableTxTypes.add(context!!.getString(R.string.tx_sort_sent, sentTxCount))
            availableTxTypes.add(context!!.getString(R.string.tx_sort_received, receivedTxCount))
            availableTxTypes.add(context!!.getString(R.string.tx_sort_transferred, transferredTxCount))

            if (stakingTxCount > 0) {
                availableTxTypes.add(context!!.getString(R.string.tx_sort_staking, stakingTxCount))
            }

            if (coinbaseTxCount > 0) {
                availableTxTypes.add(context!!.getString(R.string.tx_sort_coinbase, coinbaseTxCount))
            }

            txTypeSortAdapter?.notifyDataSetChanged()
        }
    }

    override fun onTxOrBalanceUpdateRequired(walletID: Long?) {
        loadTransactions()
    }

    private fun loadTransactions(loadMore: Boolean = false) = GlobalScope.launch(Dispatchers.Default) {

        if (loading.get()) {
            return@launch
        }

        loading.set(true)

        val limit = 40
        val offset = when {
            loadMore -> transactions.size
            else -> 0
        }

        val jsonResult = wallet!!.getTransactions(offset, limit, txFilter, newestTransactionsFirst)
        val tempTxs = gson.fromJson(jsonResult, Array<Transaction>::class.java)

        initialLoadingDone.set(true)

        if (tempTxs == null) {
            loadedAll = true
            loading.set(false)
            showHideList()

            if (!loadMore) {
                transactions.clear()
            }
            return@launch
        }

        if (tempTxs.size < limit) {
            loadedAll = true
        }

        if (loadMore) {
            val positionStart = transactions.size
            transactions.addAll(tempTxs)
            withContext(Dispatchers.Main) {
                adapter?.notifyItemRangeInserted(positionStart, tempTxs.size)

                // notify previous last item to remove bottom margin
                adapter?.notifyItemChanged(positionStart - 1)
            }

        } else {
            transactions.let {
                it.clear()
                it.addAll(tempTxs)
            }
            withContext(Dispatchers.Main) {
                adapter?.notifyDataSetChanged()
            }
        }

        loading.set(false)
        showHideList()
    }

    override fun onTransaction(transactionJson: String?) {
        if (!isForeground) {
            requiresDataUpdate = true
            return
        }

        val transaction = gson.fromJson(transactionJson, Transaction::class.java)
        if (newTxHashes.indexOf(transaction.hash) == -1) {
            newTxHashes.add(transaction.hash)

            if (transaction.walletID == wallet!!.id) {
                transaction.animate = true

                GlobalScope.launch(Dispatchers.Main) {
                    transactions.add(0, transaction)
                    adapter?.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onTransactionConfirmed(walletID: Long, hash: String, blockHeight: Int) {
        if (walletID == wallet!!.id) {
            if (!isForeground) {
                requiresDataUpdate = true
                return
            }
            GlobalScope.launch(Dispatchers.Main) {
                for (i in 0 until transactions.size) {
                    if (transactions[i].hash == hash) {
                        transactions[i].height = blockHeight
                        adapter?.notifyItemChanged(i)
                    }
                }
            }
        }
    }

    override fun onBlockAttached(walletID: Long, blockHeight: Int) {
        if (walletID == wallet!!.id) {
            if (!isForeground) {
                requiresDataUpdate = true
                return
            }
            GlobalScope.launch(Dispatchers.Main) {
                val unconfirmedTransactions = transactions.filter { it.confirmations <= 2 }.count()
                if (unconfirmedTransactions > 0) {
                    adapter?.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onSyncCompleted() {
        if (!isForeground) {
            requiresDataUpdate = true
            return
        }
        loadTransactions()
    }

    private fun showHideList() = GlobalScope.launch(Dispatchers.Main) {
        if (transactions.size > 0) {
            recycler_view?.show()
        } else {
            recycler_view?.hide()
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (!initialLoadingDone.get()) {
            return
        }

        if (parent!!.id == R.id.timestamp_sort_spinner) {
            val newestFirst = position == 0 // "Newest" is the first item
            if (newestFirst != newestTransactionsFirst) {
                newestTransactionsFirst = newestFirst
                loadTransactions()
            }
        } else {
            txFilter = when (position) {
                0 -> Dcrlibwallet.TxFilterAll
                1 -> Dcrlibwallet.TxFilterSent
                2 -> Dcrlibwallet.TxFilterReceived
                3 -> Dcrlibwallet.TxFilterTransferred
                else -> Dcrlibwallet.TxFilterStaking
            }

            loadTransactions()
        }

    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

}