/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.dcrandroid.BuildConfig
import com.dcrandroid.R
import com.dcrandroid.adapter.TransactionDetailsAdapter
import com.dcrandroid.data.Constants
import com.dcrandroid.data.Transaction
import com.dcrandroid.util.*
import dcrlibwallet.LibWallet
import org.json.JSONObject
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class TransactionDetailsActivity: AppCompatActivity() {

    private var value: TextView? = null
    private var date:TextView? = null
    private var status:TextView? = null
    private var txType:TextView? = null
    private var confirmation:TextView? = null
    private var transactionFee:TextView? = null
    private var tvHash:TextView? = null

    private var mListView: ListView? = null

    private var util: PreferenceUtil? = null

    private var transactionType: String? = null

    private var wallet: LibWallet? = null

    private var calendar: Calendar? = null
    private var sdf: SimpleDateFormat? = null

    private var items: ArrayList<TransactionDetailsAdapter.TransactionDebitCredit>? = null
    private var transaction: Transaction? = null

    private fun setListViewHeight() {
        val listAdapter = mListView!!.adapter ?: return

        var totalHeight = 0

        for (i in 0 until listAdapter.count) {
            val listItem = listAdapter.getView(i, null, mListView)
            val px = 450 * mListView!!.resources.displayMetrics.density
            listItem.measure(View.MeasureSpec.makeMeasureSpec(px.toInt(), View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            totalHeight += listItem.measuredHeight
        }

        val params = mListView!!.layoutParams
        params.height = totalHeight
        mListView!!.layoutParams = params
        mListView!!.requestLayout()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (WalletData.getInstance().wallet == null) {
            Utils.restartApp(this)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val decorView = window.decorView
            decorView.systemUiVisibility = WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }

        title = getString(R.string.Transaction_details)
        setContentView(R.layout.transaction_details_view)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        util = PreferenceUtil(this)

        wallet = WalletData.getInstance().wallet

        calendar = GregorianCalendar(TimeZone.getDefault())
        sdf = SimpleDateFormat(" dd yyyy, hh:mma", Locale.getDefault())

        mListView = findViewById(R.id.lv_tx_details)
        value = findViewById(R.id.tx_dts_value)
        date = findViewById(R.id.tx_date)
        status = findViewById(R.id.tx_dts_status)
        txType = findViewById(R.id.txtype)
        confirmation = findViewById(R.id.tx_dts_confirmation)
        transactionFee = findViewById(R.id.tx_fee)
        tvHash = findViewById(R.id.tx_hash)

        val intent = intent
        if (intent.getBooleanExtra(Constants.NO_INFO, false)) {
            getTransaction()
            return
        }else{
            transaction = intent.getSerializableExtra(Constants.TRANSACTION) as Transaction
            if (transaction == null) {
                println("transaction is null")
                return
            }
        }

        transactionType = transaction!!.type
        if (transactionType == Constants.TICKET_PURCHASE) {
            transactionType = getString(R.string.ticket_purchase)
        } else {
            transactionType = transactionType!!.substring(0, 1).toUpperCase() + transactionType!!.substring(1).toLowerCase()
        }

        loadInOut()

        tvHash!!.text = transaction!!.hash

        value!!.text = CoinFormat.format(Utils.formatDecredWithComma(transaction!!.amount) + " " + getString(R.string.dcr))
        transactionFee!!.text = CoinFormat.format(Utils.formatDecredWithComma(transaction!!.fee) + " " + getString(R.string.dcr))

        calendar!!.timeInMillis = transaction!!.timestamp * 1000

        date!!.text = calendar!!.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) + sdf!!.format(calendar!!.time).toLowerCase()

        txType!!.text = transactionType

        val height = transaction!!.height
        if (height == 0) {
            //Not included in block chain, therefore transaction is pending
            status!!.setBackgroundResource(R.drawable.tx_status_pending)
            status!!.setTextColor(applicationContext.resources.getColor(R.color.bluePendingTextColor))
            status!!.setText(R.string.pending)
            confirmation!!.setText(R.string.unconfirmed)
        } else {
            var confirmations = WalletData.getInstance().wallet.bestBlock - height
            confirmations += 1 //+1 confirmation that it exist in a block. best block - height returns 0.
            confirmation!!.text = confirmations.toString()
            if (util!!.getBoolean(Constants.SPEND_UNCONFIRMED_FUNDS) || confirmations > 1) {
                status!!.setBackgroundResource(R.drawable.tx_status_confirmed)
                status!!.setTextColor(applicationContext.resources.getColor(R.color.greenConfirmedTextColor))
                status!!.setText(R.string.confirmed)
            } else {
                status!!.setBackgroundResource(R.drawable.tx_status_pending)
                status!!.setTextColor(applicationContext.resources.getColor(R.color.bluePendingTextColor))
                status!!.setText(R.string.pending)
            }
        }

        tvHash!!.setOnClickListener {
            Utils.copyToClipboard(this@TransactionDetailsActivity, transaction!!.hash, R.string.tx_hash_copy)
        }
    }

    private fun getTransaction(){
        try{
            val txHash = intent.getStringExtra(Constants.HASH) ?: return
            transaction = TransactionsParser.parseTransaction(wallet!!.getTransaction(Utils.getHash(txHash)))
        }catch (e: Exception){
            e.printStackTrace()
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadInOut(){
        val usedInput = transaction!!.inputs
        val usedOutput = transaction!!.outputs

        val walletOutputIndices = ArrayList<Int>()
        val walletInputIndices = ArrayList<Int>()

        items = ArrayList()
        items!!.add(TransactionDetailsAdapter.TransactionDebitCredit(getString(R.string.inputs),
                TransactionDetailsAdapter.TransactionDebitCredit.ItemType.HEADER)) // Inputs Header

        try{
            val rawJson = wallet!!.decodeTransaction(Utils.getHash(transaction!!.hash))
            val parent = JSONObject(rawJson)
            val inputs = parent.getJSONArray(Constants.INPUTS)
            val outputs = parent.getJSONArray(Constants.OUTPUTS)

            for (i in usedInput!!.indices) {
                val input = inputs.getJSONObject(usedInput[i].index)
                walletInputIndices.add(usedInput[i].index)

                var hash = input.getString(Constants.PREVIOUS_TRANSACTION_HASH)

                if (hash == Constants.STAKE_BASE_HASH) {
                    hash = "Stakebase: 0000"
                }

                hash += ":" + input.getInt(Constants.PREVIOUS_TRANSACTION_INDEX)

                val amount = getString(R.string.external_output_account, Utils.formatDecredWithComma(usedInput[i].previousAmount), usedInput[i].accountName)

                val debit = TransactionDetailsAdapter.TransactionDebitCredit(amount, hash,
                        TransactionDetailsAdapter.TransactionDebitCredit.ItemType.ITEM, TransactionDetailsAdapter.TransactionDebitCredit.Direction.DEBIT)
                items!!.add(debit)
            }

            for (i in 0 until inputs.length()) {

                val input = inputs.getJSONObject(i)

                if (walletInputIndices.indexOf(i) != -1) {
                    continue
                }

                val amount = getString(R.string.external_output_amount, Utils.formatDecredWithComma(input.getLong(Constants.AMOUNT_IN)))

                var hash = input.getString(Constants.PREVIOUS_TRANSACTION_HASH)

                if (hash == Constants.STAKE_BASE_HASH) {
                    hash = "Stakebase: 0000"
                }
                hash += ":" + input.getInt(Constants.PREVIOUS_TRANSACTION_INDEX)

                items!!.add(TransactionDetailsAdapter.TransactionDebitCredit(amount, hash,
                        TransactionDetailsAdapter.TransactionDebitCredit.ItemType.ITEM, TransactionDetailsAdapter.TransactionDebitCredit.Direction.DEBIT))
            }

            // Outputs header
            items!!.add(TransactionDetailsAdapter.TransactionDebitCredit(getString(R.string.outputs), TransactionDetailsAdapter.TransactionDebitCredit.ItemType.HEADER))

            for (i in usedOutput!!.indices) {
                walletOutputIndices.add(usedOutput[i].index)
                val amount = getString(R.string.external_output_account, Utils.formatDecredWithComma(usedOutput[i].amount), wallet!!.accountOfAddress(usedOutput[i].address))
                val address = usedOutput[i].address
                items!!.add(TransactionDetailsAdapter.TransactionDebitCredit(amount, address,
                        TransactionDetailsAdapter.TransactionDebitCredit.ItemType.ITEM, TransactionDetailsAdapter.TransactionDebitCredit.Direction.CREDIT))
            }

            for (i in 0 until outputs.length()) {
                val output = outputs.getJSONObject(i)

                if (walletOutputIndices.indexOf(i) != -1) {
                    continue
                }

                val addresses = output.getJSONArray(Constants.ADDRESSES)

                val scriptType = output.getString(Constants.SCRIPT_TYPE)

                var address = if (addresses.length() > 0) addresses.getString(0) else ""

                var amount = getString(R.string.external_output_amount, Utils.formatDecredWithComma(output.getLong(Constants.VALUE)))

                when (scriptType) {
                    "nulldata" -> {
                        amount = "[null data]"
                        address = "[script]"
                    }
                    "stakegen" -> address = "[stakegen]"
                }

                items!!.add(TransactionDetailsAdapter.TransactionDebitCredit(amount, address, TransactionDetailsAdapter.TransactionDebitCredit.ItemType.ITEM, TransactionDetailsAdapter.TransactionDebitCredit.Direction.CREDIT))
            }

            if (transactionType.equals(Constants.VOTE, ignoreCase = true)) {
                findViewById<LinearLayout>(R.id.tx_dts_vote_layout).visibility = View.VISIBLE

                val version = findViewById<TextView>(R.id.tx_dts_version)
                val lastBlockValid = findViewById<TextView>(R.id.tx_dts_block_valid)
                val voteBits = findViewById<TextView>(R.id.tx_dts_vote_bits)

                version.text = String.format(Locale.getDefault(), "%d", parent.getInt(Constants.VOTE_VERSION))

                lastBlockValid.text = parent.getBoolean(Constants.LAST_BLOCK_VALID).toString()

                voteBits.text = parent.getString(Constants.VOTE_BITS)
            }

            val adp = TransactionDetailsAdapter(this, items!!)
            mListView!!.adapter = adp
            mListView!!.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                val item = items!![position]
                if (item.type === TransactionDetailsAdapter.TransactionDebitCredit.ItemType.HEADER || item.info == null || item.info.trim { it <= ' ' } == "") {
                    return@OnItemClickListener
                }

                if (item.direction === TransactionDetailsAdapter.TransactionDebitCredit.Direction.DEBIT) {
                    Utils.copyToClipboard(this@TransactionDetailsActivity, item.info, R.string.tx_hash_copy)
                } else {
                    Utils.copyToClipboard(this@TransactionDetailsActivity, item.info, R.string.address_copy_text)
                }
            }

            setListViewHeight()
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.transaction_details_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.tx_details_tx_hash -> Utils.copyToClipboard(this, transaction!!.hash, R.string.tx_hash_copy)
            R.id.tx_details_raw_tx -> Utils.copyToClipboard(this, transaction!!.raw, R.string.raw_tx_copied)
            R.id.tx_viewOnDcrData -> {
                val url = if(BuildConfig.IS_TESTNET) {
                    "https://testnet.dcrdata.org/tx/" + transaction!!.hash
                } else {
                    "https://mainnet.dcrdata.org/tx/" + transaction!!.hash
                }

                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(browserIntent)
            }
        }
        return super.onOptionsItemSelected(item)
    }
}