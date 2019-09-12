/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.fragments

import android.os.Build
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
import com.dcrandroid.util.Deserializer
import com.google.gson.GsonBuilder
import dcrlibwallet.Dcrlibwallet
import dcrlibwallet.LibWallet
import kotlinx.android.synthetic.main.transactions_page.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class TransactionsFragment: BaseFragment(), AdapterView.OnItemSelectedListener, ViewTreeObserver.OnScrollChangedListener {

    private var loadedAll = false
    private val loading = AtomicBoolean()

    private var wallet: LibWallet? = null

    private var layoutManager: LinearLayoutManager? = null
    private val transactions: ArrayList<Transaction> = ArrayList()
    private var adapter: TransactionPageAdapter? = null
    private val gson = GsonBuilder().registerTypeHierarchyAdapter(ArrayList::class.java, Deserializer.TransactionDeserializer())
            .create()

    private var txFilter = Dcrlibwallet.TxFilterAll
    private val availableTxTypes = java.util.ArrayList<String>()

    private var txTypeSortAdapter: ArrayAdapter<String>? = null

    fun setWalletID(walletID: Long): TransactionsFragment{
        wallet = multiWallet.getWallet(walletID)
        return this
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.transactions_page, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setToolbarTitle(R.string.transactions, false)
        adapter = TransactionPageAdapter(context!!, transactions)

        txTypeSortAdapter = ArrayAdapter(context!!, android.R.layout.simple_spinner_item, availableTxTypes)
        txTypeSortAdapter!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        tx_type_spinner.adapter = txTypeSortAdapter
        tx_type_spinner.onItemSelectedListener = this


        layoutManager = LinearLayoutManager(context)
        recycler_view.layoutManager = layoutManager
        recycler_view.adapter = adapter

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            recycler_view.viewTreeObserver.addOnScrollChangedListener(this)

        initAdapter()

    }

    override fun onScrollChanged() {
        if(context == null) return

        val firstVisibleItem = layoutManager!!.findFirstCompletelyVisibleItemPosition()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            transactions_page_header.elevation = if(firstVisibleItem != 0) resources.getDimension(R.dimen.app_bar_elevation)
            else 0f
        }

        val lastVisibleItem = layoutManager!!.findLastCompletelyVisibleItemPosition()
        if(lastVisibleItem >= transactions.size - 5){
            if(!loadedAll){
                recycler_view.stopScroll()
                loadTransactions(loadMore = true)
            }
        }
    }

    private fun initAdapter() {
        refreshAvailableTxType()

        loadTransactions()
    }

    private fun refreshAvailableTxType() =  GlobalScope.launch(Dispatchers.Default) {
        availableTxTypes.clear()

        val txCount = wallet!!.countTransactions(Dcrlibwallet.TxFilterAll)
        val sentTxCount = wallet!!.countTransactions(Dcrlibwallet.TxFilterSent)
        val receivedTxCount =  wallet!!.countTransactions(Dcrlibwallet.TxFilterReceived)
        val transferredTxCount =  wallet!!.countTransactions(Dcrlibwallet.TxFilterTransferred)
        val stakingTxCount = wallet!!.countTransactions(Dcrlibwallet.TxFilterStaking)
        val coinbaseTxCount =  wallet!!.countTransactions(Dcrlibwallet.TxFilterCoinBase)

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

        withContext(Dispatchers.Main){
            txTypeSortAdapter?.notifyDataSetChanged()
        }
    }

    private fun loadTransactions(loadMore: Boolean = false) = GlobalScope.launch(Dispatchers.Default){

        if(loading.get()){
            return@launch
        }

        loading.set(true)

        val limit = 40
        val offset = when {
            loadMore -> transactions.size
            else -> 0
        }

        val jsonResult = wallet!!.getTransactions(offset, limit, txFilter)
        val tempTxs = gson.fromJson(jsonResult, Array<Transaction>::class.java)

        if (tempTxs == null) {
            loadedAll = true
            loading.set(false)
            return@launch
        }

        if(tempTxs.size < limit){
            loadedAll = true
        }

        if(loadMore){
            val positionStart = transactions.size
            transactions.addAll(tempTxs)
            withContext(Dispatchers.Main){
                adapter?.notifyItemRangeInserted(positionStart, tempTxs.size)

                // notify previous last item to remove bottom margin
                adapter?.notifyItemChanged(positionStart - 1)
            }

        }else{
            transactions.let {
                it.clear()
                it.addAll(tempTxs)
            }
            withContext(Dispatchers.Main){
                adapter?.notifyDataSetChanged()
            }
        }

        loading.set(false)
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        txFilter = when(position){
            0 -> Dcrlibwallet.TxFilterAll
            1 -> Dcrlibwallet.TxFilterSent
            2 -> Dcrlibwallet.TxFilterReceived
            3 -> Dcrlibwallet.TxFilterTransferred
            else -> Dcrlibwallet.TxFilterStaking
        }

        loadTransactions()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

}