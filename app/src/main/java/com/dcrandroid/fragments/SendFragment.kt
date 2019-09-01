/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.fragments

import android.app.Activity.RESULT_OK
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.dcrandroid.BuildConfig
import com.dcrandroid.MainActivity
import com.dcrandroid.R
import com.dcrandroid.activities.EnterPassCode
import com.dcrandroid.activities.ReaderActivity
import com.dcrandroid.activities.TransactionDetailsActivity
import com.dcrandroid.adapter.AccountSpinnerAdapter
import com.dcrandroid.data.Account
import com.dcrandroid.data.Constants
import com.dcrandroid.dialog.ConfirmTransactionDialog
import com.dcrandroid.dialog.InfoDialog
import com.dcrandroid.extensions.visibleWalletAccounts
import com.dcrandroid.util.*
import dcrlibwallet.Dcrlibwallet
import dcrlibwallet.LibWallet
import kotlinx.android.synthetic.main.fragment_send.*
import org.json.JSONException
import org.json.JSONObject
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*
import kotlin.collections.ArrayList

class SendFragment : Fragment(), AdapterView.OnItemSelectedListener, GetExchangeRate.ExchangeRateCallback {

    private var SEND_ACCOUNT = false
    private val SCANNER_ACTIVITY_REQUEST_CODE = 0
    private val PASSCODE_REQUEST_CODE = 1
    private var constants: WalletData = WalletData.getInstance()
    private var textChanged: Boolean = false
    private var isSendAll: Boolean = false
    private var exchangeRate: Double = -1.0
    private var exchangeDecimal: BigDecimal? = null
    private val formatter: DecimalFormat = NumberFormat.getNumberInstance(Locale.ENGLISH) as DecimalFormat
    private var accounts: List<Account> = ArrayList()
    private var dataAdapter: AccountSpinnerAdapter? = null
    private var util: PreferenceUtil? = null
    private var pd: ProgressDialog? = null
    private val wallet: LibWallet
        get() {
            return constants.wallet
        }

    private val requiredConfirmations: Int
        get() {
            return if (util!!.getBoolean(Constants.SPEND_UNCONFIRMED_FUNDS))
                0
            else Constants.REQUIRED_CONFIRMATIONS
        }

    private val amount: Long
        get() {
            return Dcrlibwallet.amountAtom(amount_dcr.text.toString().toDouble())
        }

    private val validDestinationAddress: String?
        get() {
            if (SEND_ACCOUNT) {
                return wallet.currentAddress(selectedDestAccount.accountNumber)
            } else {
                val destAddress = send_dcr_address.text.toString()
                if (wallet.isAddressValid(destAddress)) {
                    return destAddress
                }

                return null
            }
        }

    private val destinationAddress: String
        get() {
            if (SEND_ACCOUNT) {
                return wallet.currentAddress(selectedDestAccount.accountNumber)
            } else {
                var destAddress = send_dcr_address.text.toString()
                if (destAddress == Constants.EMPTY_STRING) {
                    try {
                        destAddress = wallet.currentAddress(0)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                return destAddress
            }
        }

    private val selectedAccount: Account
        get() = accounts[send_account_spinner.selectedItemPosition]

    private val selectedDestAccount: Account
        get() = accounts[destination_account_spinner.selectedItemPosition]


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        formatter.applyPattern("#.########")
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_send, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        requireActivity().setTitle(R.string.send)

        util = PreferenceUtil(requireContext())

        if (Integer.parseInt(util!!.get(Constants.CURRENCY_CONVERSION, "0")) != 0) {
            GetExchangeRate(getString(R.string.dcr_to_usd_exchange_url), util!!.get(Constants.USER_AGENT, ""), this).execute()
        }

        send_dcr_scan.setOnClickListener {
            val intent = Intent(activity, ReaderActivity::class.java)
            startActivityForResult(intent, SCANNER_ACTIVITY_REQUEST_CODE)
        }

        send_max.setOnClickListener { sendMax() }

        send_btn.setOnClickListener {
            var errors = 0
            val address = validDestinationAddress

            if (address == null) {
                val enteredAddress = send_dcr_address.text.toString()
                if (enteredAddress.isEmpty()) {
                    errors++
                    tvDestinationError.setText(R.string.empty_destination_address)
                } else if (!wallet.isAddressValid(enteredAddress)) {
                    errors++
                    tvDestinationError.setText(R.string.invalid_destination_address)
                }
            }

            if (amount_dcr.text.toString().isEmpty() || amount == 0L) {
                errors++
                send_error_label.setText(R.string.amount_can_not_be_zero)
            }

            if (errors > 0) {
                return@setOnClickListener
            }

            if (!constants.synced) {
                send_main_error.setText(R.string.network_synchronization)
                return@setOnClickListener
            }

            if (constants.peers == 0) {
                send_main_error.setText(R.string.not_connected_error)
                return@setOnClickListener
            }

            send_main_error.text = null

            showConfirmTransactionDialog()
        }

        prepareAccounts()

        dataAdapter = AccountSpinnerAdapter(accounts, layoutInflater)
        send_account_spinner.adapter = dataAdapter
        destination_account_spinner.adapter = dataAdapter

        send_dcr_address.addTextChangedListener(addressWatcher)
        send_dcr_address.setupClearAction()
        amount_usd.addTextChangedListener(exchangeWatcher)
        amount_dcr.addTextChangedListener(amountWatcher)
        send_account_spinner.onItemSelectedListener = this
        destination_account_spinner.onItemSelectedListener = this

        if (SEND_ACCOUNT) {
            destination_address_container.visibility = View.GONE
            destination_account_container.visibility = View.VISIBLE

            var position = destination_account_spinner.selectedItemPosition
            if (position < 0) {
                position = 0
            }

            val receiveAddress = constants.wallet.currentAddress(position)
            send_dcr_address.setText(receiveAddress)

            tvDestinationError.text = ""
        }

        paste_dcr_address.setOnClickListener {
            send_dcr_address.setText(Utils.readFromClipboard(activity!!.applicationContext))
            paste_dcr_address.visibility = View.GONE
        }

        rate_unavailable.setOnClickListener {
            it.visibility = View.INVISIBLE
            GetExchangeRate(getString(R.string.dcr_to_usd_exchange_url), util!!.get(Constants.USER_AGENT, ""), this).execute()
        }
    }

    override fun onResume() {
        super.onResume()

        // enable "Tap to paste" if there's a valid address in clipboard
        if (constants.wallet.isAddressValid(Utils.readFromClipboard(activity!!.applicationContext)) && validDestinationAddress == null) {
            paste_dcr_address.visibility = View.VISIBLE
        } else {
            paste_dcr_address.visibility = View.GONE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.send_page_menu, menu)
        if (SEND_ACCOUNT) {
            menu.findItem(R.id.send_to_account).setTitle(R.string.send_to_address)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.send_to_account -> {
                if (destination_address_container.visibility == View.VISIBLE) {

                    destination_address_container.visibility = View.GONE
                    destination_account_container.visibility = View.VISIBLE

                    tvDestinationError.text = ""
                    item.setTitle(R.string.send_to_address)
                    SEND_ACCOUNT = true
                } else {
                    destination_address_container.visibility = View.VISIBLE
                    destination_account_container.visibility = View.GONE

                    send_dcr_address.removeTextChangedListener(addressWatcher)
                    send_dcr_address.text.clear()
                    send_dcr_address.addTextChangedListener(addressWatcher)

                    if (constants.wallet.isAddressValid(Utils.readFromClipboard(activity!!.applicationContext))) paste_dcr_address.visibility = View.VISIBLE
                    send_dcr_scan.visibility = View.VISIBLE
                    toggleSendButton(false)

                    item.setTitle(R.string.send_to_account)
                    SEND_ACCOUNT = false
                }
                return true
            }
            R.id.clear_fields -> {
                if (destination_address_container.visibility == View.VISIBLE) {
                    send_dcr_address.text.clear()
                }

                tvDestinationError.text = ""

                amount_dcr.text.clear()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (isSendAll) {
            send_max.performClick()
            return
        }

        constructTransaction()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    private fun prepareAccounts() {
        accounts = wallet.visibleWalletAccounts(context!!)
        accounts = accounts.filter { it.accountNumber != Int.MAX_VALUE }.toList() // Filter out imported account

        if (dataAdapter != null) {
            requireActivity().runOnUiThread {
                dataAdapter!!.notifyDataSetChanged()
            }
        }
    }

    private fun sendMax() {
        isSendAll = true
        try {

            val spendableBalance = getSpendableForSelectedAccount()

            if (spendableBalance == 0L) {
                amount_dcr.setText("0")
                return
            }

            val maxSpendable = wallet.estimateMaxSendAmount(selectedAccount.accountNumber, destinationAddress, requiredConfirmations)

            val amount = Utils.formatDecredWithoutComma(maxSpendable.atomValue)
            amount_dcr.removeTextChangedListener(amountWatcher)
            amount_dcr.setText(amount)
            amount_dcr.addTextChangedListener(amountWatcher)

            if (exchangeDecimal != null) {
                dcrToUSD()
            }
            constructTransaction()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Throws(Exception::class)
    private fun getSpendableForSelectedAccount(): Long {
        return wallet.spendableForAccount(selectedAccount.accountNumber, requiredConfirmations)
    }

    private fun validateAmount(): Boolean {
        val s = amount_dcr.text.toString()

        if (s.isEmpty()) {
            return false
        }
        if (s.indexOf('.') != -1) {
            val atoms = s.substring(s.indexOf('.'))
            if (atoms.length > 9) {
                send_error_label.text = getString(R.string._8_decimal_amount_err)
                return false
            }
        }
        if (java.lang.Double.parseDouble(s) == 0.0) {
            return false
        }
        send_error_label.text = ""
        return true
    }

    private fun clearEstimates() {
        send_dcr_estimate_size.setText(R.string._0_bytes)
        send_dcr_estimate_fee.setText(R.string._0_00_dcr)
        send_dcr_balance_after.setText(R.string._0_00_dcr)

        toggleSendButton(false)
    }

    private fun toggleSendButton(enable: Boolean) {
        if (enable) {
            send_btn.setTextColor(Color.WHITE)
            send_btn.setBackgroundResource(R.drawable.btn_blue)
        } else {
            send_btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.blackTextColor38pc))
            send_btn.setBackgroundColor(Color.parseColor("#e6eaed"))
        }
    }

    private fun constructTransaction() {

        if (!validateAmount()) {
            clearEstimates()
            return
        }

        try {

            if (send_dcr_address.text.toString().isNotEmpty() && !wallet.isAddressValid(send_dcr_address.text.toString()) && !SEND_ACCOUNT) {
                tvDestinationError.setText(R.string.invalid_destination_address)
                return
            }

            var address = validDestinationAddress
            if (address == null) {
                address = destinationAddress
            }

            val txFeeAndSize = wallet.calculateNewTxFeeAndSize(amount, selectedAccount.accountNumber, address, requiredConfirmations, isSendAll)

            val estFee = txFeeAndSize.fee.dcrValue
            val estSize = txFeeAndSize.estimatedSignedSize

            // Convert to USD if currency conversion is enabled
            if (exchangeDecimal != null) {
                var fee = BigDecimal(estFee)
                fee = fee.setScale(9, RoundingMode.HALF_UP)
                val convertedFee = fee.multiply(exchangeDecimal)
                val format = DecimalFormat("#.####")
                val formattedFee = format.format(convertedFee.toDouble())
                send_dcr_estimate_fee.text = "${CoinFormat.format(estFee)}\n($formattedFee USD)"
            } else {
                send_dcr_estimate_fee.text = CoinFormat.format(estFee)
            }

            send_dcr_estimate_size.text = String.format(Locale.getDefault(), "%d %s", estSize, getString(R.string.bytes))

            val spendableBalance = getSpendableForSelectedAccount()
            val balanceAfter = spendableBalance - amount - txFeeAndSize.fee.atomValue
            send_dcr_balance_after.text = CoinFormat.format(balanceAfter)

            // Enable send button if the address is valid
            if (validDestinationAddress != null) {
                toggleSendButton(true)
            }

        } catch (e: Exception) {
            clearEstimates()
            send_error_label.text = Utils.translateError(requireActivity().applicationContext, e)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        System.out.println("Request Code $requestCode Result $resultCode")
        if (requestCode == SCANNER_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                try {
                    var returnString = data!!.getStringExtra(Constants.ADDRESS)
                    if (returnString.startsWith(getString(R.string.decred_colon)))
                        returnString = returnString.replace(getString(R.string.decred_colon), "")
                    if (returnString.length < 25) {
                        Toast.makeText(context, R.string.wallet_add_too_short, Toast.LENGTH_LONG).show()
                        return
                    } else if (returnString.length > 36) {
                        Toast.makeText(context, R.string.wallet_addr_too_long, Toast.LENGTH_LONG).show()
                        return
                    }

                    if (returnString.startsWith("T") && BuildConfig.IS_TESTNET) {
                        send_dcr_address.setText(returnString)
                    } else if (returnString.startsWith("D") && !BuildConfig.IS_TESTNET) {
                        send_dcr_address.setText(returnString)
                    } else {
                        if (BuildConfig.IS_TESTNET) {
                            Toast.makeText(context, R.string.invalid_testnet_address, Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, R.string.invalid_mainnett_address, Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), R.string.error_not_decred_address, Toast.LENGTH_LONG).show()
                    send_dcr_address.setText("")
                }
            }
        } else if (requestCode == PASSCODE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                startTransaction(data!!.getStringExtra(Constants.PASSPHRASE))
            }
        }
    }

    private fun showConfirmTransactionDialog() {
        if (context == null || activity == null) {
            return
        }
        send_error_label.text = null

        try {
            val txFeeAndSize = wallet.calculateNewTxFeeAndSize(amount, selectedAccount.accountNumber, validDestinationAddress, requiredConfirmations, isSendAll)
            val transactionDialog = ConfirmTransactionDialog(context!!)
                    .setAddress(validDestinationAddress!!)
                    .setAmount(amount)
                    .setFee(txFeeAndSize.estimatedSignedSize)
                    .setExchangeDecimal(exchangeDecimal)

            transactionDialog.setPositiveButton(DialogInterface.OnClickListener { _, _ ->
                if (util!!.get(Constants.SPENDING_PASSPHRASE_TYPE) == Constants.PIN) {
                    val intent = Intent(context, EnterPassCode::class.java)
                    startActivityForResult(intent, PASSCODE_REQUEST_CODE)
                } else {
                    startTransaction(transactionDialog.getPassphrase())
                }
            })

            if (SEND_ACCOUNT) transactionDialog.setAccount(selectedDestAccount.accountName)
            transactionDialog.setCancelable(true)
            transactionDialog.setCanceledOnTouchOutside(false)
            transactionDialog.show()
        } catch (e: Exception) {
            if (activity != null && context != null) {
                Toast.makeText(context, Utils.translateError(context, e), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startTransaction(passphrase: String) {
        if (context == null) {
            return
        }
        pd = Utils.getProgressDialog(context, false, false, getString(R.string.sending_transaction))
        pd!!.show()
        Thread {
            try {
                val txHash = wallet.sendTransaction(amount, selectedAccount.accountNumber, validDestinationAddress, requiredConfirmations, isSendAll, passphrase.toByteArray())
                txHash.reverse()
                val sb = StringBuilder()
                for (byte in txHash) {
                    sb.append(String.format(Locale.getDefault(), "%02x", byte))
                }

                if (activity != null) {
                    activity!!.runOnUiThread {
                        if (pd!!.isShowing) {
                            pd!!.dismiss()
                        }
                        prepareAccounts()
                        showTxConfirmDialog(sb.toString())
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (activity != null) {
                    activity!!.runOnUiThread {
                        if (pd!!.isShowing) {
                            pd!!.dismiss()
                        }
                        if (e.message == Dcrlibwallet.ErrInvalidPassphrase) {
                            val message = if (util!!.get(Constants.SPENDING_PASSPHRASE_TYPE)
                                    == Constants.PASSWORD) getString(R.string.invalid_password)
                            else getString(R.string.invalid_pin)
                            val retryDialog = InfoDialog(context)
                                    .setDialogTitle(getString(R.string.failed_to_send_transaction))
                                    .setMessage(message)
                                    .setIcon(R.drawable.np_amount_withdrawal)
                                    .setPositiveButton(getString(R.string.retry_caps), DialogInterface.OnClickListener { _, _ ->
                                        showConfirmTransactionDialog()
                                    })
                                    .setNegativeButton(getString(R.string.cancel).toUpperCase(), DialogInterface.OnClickListener { dialog, _ ->
                                        dialog.dismiss()
                                    })

                            retryDialog.setCancelable(true)
                            retryDialog.setCanceledOnTouchOutside(true)
                            retryDialog.show()
                        } else {
                            send_error_label.text = Utils.translateError(context, e)
                        }
                    }
                }
            }
        }.start()
    }

    private fun showTxConfirmDialog(txHash: String) {
        if (activity == null || context == null) {
            return
        }

        val dialog = InfoDialog(context)
                .setDialogTitle(getString(R.string.transaction_was_successful))
                .setMessage("${getString(R.string.hash_colon)}\n$txHash")
                .setIcon(R.drawable.np_amount_withdrawal)
                .setTitleTextColor(ContextCompat.getColor(requireContext(), R.color.greenLightTextColor))
                .setMessageTextColor(ContextCompat.getColor(requireContext(), R.color.blue))
                .setPositiveButton(getString(R.string.ok), DialogInterface.OnClickListener { _, _ ->
                    run {
                        if (activity != null && activity is MainActivity) {
                            val mainActivity = activity as MainActivity
                            mainActivity.displayOverview()
                        }
                    }
                })
                .setNegativeButton(getString(R.string.view_cap), DialogInterface.OnClickListener { d, _ ->
                    d.dismiss()
                    val intent = Intent(context, TransactionDetailsActivity::class.java)
                    intent.putExtra(Constants.HASH, txHash)
                    intent.putExtra(Constants.NO_INFO, true)
                    startActivity(intent)
                    run {
                        if (activity != null && activity is MainActivity) {
                            val mainActivity = activity as MainActivity
                            mainActivity.displayOverview()
                        }
                    }
                })
                .setMessageClickListener(View.OnClickListener {
                    Utils.copyToClipboard(context, txHash, R.string.tx_hash_copy)
                })

        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(false)

        dialog.show()

        amount_dcr.text = null

        send_dcr_address.removeTextChangedListener(addressWatcher)
        send_dcr_address.text = null
        send_dcr_address.addTextChangedListener(addressWatcher)
    }

    private val addressWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable) {
            if (s.toString().isEmpty()) {
                tvDestinationError.text = null
                if (constants.wallet.isAddressValid(Utils.readFromClipboard(activity!!.applicationContext))) paste_dcr_address.visibility = View.VISIBLE
                send_dcr_scan.visibility = View.VISIBLE
                toggleSendButton(false)
            } else if (!constants.wallet.isAddressValid(s.toString())) {
                tvDestinationError.setText(R.string.invalid_destination_address)
                paste_dcr_address.visibility = View.GONE
                send_dcr_scan.visibility = View.GONE
                toggleSendButton(false)
            } else if (constants.wallet.isAddressValid(s.toString())) {
                tvDestinationError.text = null
                tvDestinationError.visibility = View.VISIBLE
                paste_dcr_address.visibility = View.GONE
                send_dcr_scan.visibility = View.GONE
                constructTransaction()
            }
        }
    }

    private val amountWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable) {
            textChanged = true
            isSendAll = false

            if (s.toString() == ".") {
                return
            }

            if (exchangeDecimal != null) {
                if (s.isNotEmpty()) {
                    dcrToUSD()

                    amount_usd.setSelection(amount_usd.text.length)
                } else {
                    amount_usd.run {
                        removeTextChangedListener(exchangeWatcher)
                        text = null
                        addTextChangedListener(exchangeWatcher)
                    }
                }
            }

            constructTransaction()
        }
    }

    private val exchangeWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable) {
            if (exchangeDecimal == null) {
                return
            }

            isSendAll = false
            if (s.isEmpty()) {
                amount_dcr.run {
                    removeTextChangedListener(amountWatcher)
                    text = null
                    addTextChangedListener(amountWatcher)
                }
            } else {
                usdToDCR()

                amount_dcr.setSelection(amount_dcr.text.length)
            }

            constructTransaction()
        }
    }

    private fun EditText.setupClearAction() {
        addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                val clearIcon = if (editable?.isNotEmpty() == true) R.drawable.ic_clear else 0
                setCompoundDrawablesWithIntrinsicBounds(0, 0, clearIcon, 0)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })

        setOnTouchListener(View.OnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (this.right - this.compoundPaddingRight)) {
                    this.setText("")
                    return@OnTouchListener true
                }
            }
            return@OnTouchListener false
        })
    }

    override fun onExchangeRateSuccess(s: String?) {
        if (activity == null) {
            return
        }

        val context = requireActivity().applicationContext
        val index = Integer.parseInt(util!!.get(Constants.CURRENCY_CONVERSION, "0"))
        val currency = context.resources.getStringArray(R.array.currency_conversion_abbrv)[index]
        val source = context.resources.getStringArray(R.array.currency_conversion_source)[index]

        if (s == null) {
            rate_unavailable.visibility = View.VISIBLE
            rate_unavailable.text = String.format("%s %s", source, context.getString(R.string.exchange_rate_unavailable))
            return
        }

        try {
            rate_unavailable.visibility = View.GONE
            val apiResult = JSONObject(s)
            if (apiResult.getBoolean("success")) {
                val result = apiResult.getJSONObject("result")
                exchangeRate = result.getDouble("Last")
                exchangeDecimal = BigDecimal(exchangeRate).setScale(9, RoundingMode.HALF_UP)
                send_dcr_exchange_rate.text = String.format(Locale.getDefault(), "%.2f %s/DCR (%s)", result.getDouble("Last"), currency, source)
                exchange_details.visibility = View.VISIBLE
                if (amount_dcr.text.isNotEmpty()) {
                    dcrToUSD()
                }

                exchange_layout.visibility = View.VISIBLE
            } else {
                Toast.makeText(context, "${context.getString(R.string.exchange_rate_error)}: ${apiResult.getString("message")}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    override fun onExchangeRateError(e: java.lang.Exception) {

    }

    private fun dcrToUSD() {
        var currentAmount = BigDecimal(amount_dcr.text.toString())
        currentAmount = currentAmount.setScale(9, RoundingMode.HALF_UP)

        val convertedAmount = currentAmount.multiply(exchangeDecimal)
        amount_usd.run {
            removeTextChangedListener(exchangeWatcher)
            setText(formatter.format(convertedAmount.toDouble()))
            addTextChangedListener(exchangeWatcher)
        }
    }

    private fun usdToDCR() {
        var currentAmount = BigDecimal(amount_usd.text.toString())
        currentAmount = currentAmount.setScale(9, RoundingMode.HALF_UP)

        val convertedAmount = currentAmount.divide(exchangeDecimal, MathContext.DECIMAL128)
        amount_dcr.run {
            removeTextChangedListener(amountWatcher)
            setText(formatter.format(convertedAmount.toDouble()))
            addTextChangedListener(amountWatcher)
        }
    }
}