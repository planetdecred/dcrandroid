package com.dcrandroid.fragments

import android.app.Activity.RESULT_OK
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dcrandroid.R
import com.dcrandroid.activities.ReaderActivity
import com.dcrandroid.util.CoinFormat
import com.dcrandroid.util.DcrConstants
import kotlinx.android.synthetic.main.fragment_send.*
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import android.widget.ArrayAdapter
import com.dcrandroid.data.Constants
import com.dcrandroid.util.PreferenceUtil
import org.json.JSONException
import android.widget.Toast
import org.json.JSONObject
import android.os.AsyncTask
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.widget.AdapterView
import android.widget.EditText
import com.dcrandroid.MainActivity
import com.dcrandroid.data.Account
import com.dcrandroid.dialog.ConfirmTransactionDialog
import com.dcrandroid.dialog.InfoDialog
import com.dcrandroid.util.Utils
import mobilewallet.LibWallet
import mobilewallet.Mobilewallet
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList


class SendFragment : Fragment(), AdapterView.OnItemSelectedListener {

    private val SCANNER_ACTIVITY_RESULT_CODE = 0
    private var constants: DcrConstants = DcrConstants.getInstance()
    private var textChanged : Boolean = false
    private var isSendAll : Boolean = false
    private var exchangeRate : Double = -1.0
    private var exchangeDecimal: BigDecimal? = null
    private val formatter: DecimalFormat = DecimalFormat("#.########")
    private var accounts: ArrayList<String>? = null
    private val accountNumbers: ArrayList<Int> = ArrayList()
    private var dataAdapter: ArrayAdapter<String>? = null
    private var util: PreferenceUtil? = null
    private var pd: ProgressDialog? = null
    private val wallet: LibWallet
        get(){
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
            return Mobilewallet.amountAtom(amount_dcr.text.toString().toDouble())
        }

    private val destinationAddress: String
        get() {
            var destAddress = send_dcr_address.text.toString()
            if (destAddress == Constants.EMPTY_STRING) {
                destAddress = util!!.get(Constants.RECENT_ADDRESS)
                if (destAddress == Constants.EMPTY_STRING) {
                    try {
                        destAddress = constants.wallet.addressForAccount(0)
                        util!!.set(Constants.RECENT_ADDRESS, destAddress)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            return destAddress
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_send, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        requireActivity().setTitle(R.string.send)

        util = PreferenceUtil(requireContext())

        accounts = ArrayList()

        dataAdapter = ArrayAdapter(requireActivity().applicationContext, R.layout.spinner_list_item_1, accounts)
        dataAdapter!!.setDropDownViewResource(R.layout.dropdown_item_1)
        send_account_spinner.adapter = dataAdapter

        if(Integer.parseInt(util!!.get(Constants.CURRENCY_CONVERSION, "0")) != 0) {
            GetExchangeRate(this).execute()
        }

        send_dcr_scan.setOnClickListener {
            val intent = Intent(activity, ReaderActivity::class.java)
            startActivityForResult(intent, SCANNER_ACTIVITY_RESULT_CODE)
        }

        send_max.setOnClickListener {
            isSendAll = true
            try {
                val spendableBalance = getSpendableForSelectedAccount()
                amount_dcr.removeTextChangedListener(amountWatcher)
                amount_dcr.setText(Utils.formatDecredWithoutComma(spendableBalance))
                amount_dcr.addTextChangedListener(amountWatcher)

                if (exchangeDecimal != null) {
                    var currentAmount = BigDecimal(Mobilewallet.amountCoin(spendableBalance))
                    currentAmount = currentAmount.setScale(9, RoundingMode.HALF_UP)

                    val convertedAmount = currentAmount.multiply(exchangeDecimal, MathContext.DECIMAL128)

                    amount_usd.removeTextChangedListener(exchangeWatcher)
                    amount_usd.setText(formatter.format(convertedAmount.toDouble()))
                    amount_usd.addTextChangedListener(exchangeWatcher)
                }
                constructTransaction()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        send_btn.setOnClickListener {showInputPassPhraseDialog()}

        prepareAccounts()

        send_dcr_address.addTextChangedListener(addressWatcher)
        amount_usd.addTextChangedListener(exchangeWatcher)
        amount_dcr.addTextChangedListener(amountWatcher)
        send_account_spinner.onItemSelectedListener = this
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if(isSendAll){
            send_max.performClick()
            return
        }
        constructTransaction()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        TODO("not implemented")
    }

    private fun prepareAccounts(){
        val parsedAccounts = Account.parse(wallet.getAccounts(requiredConfirmations))
        accountNumbers.clear()
        accounts!!.clear()

        for (account in parsedAccounts) {
            if(account.accountName.trim() == (Constants.IMPORTED)){
                continue
            }

            accounts!!.add(account.accountName + " [" + Utils.formatDecred(account.balance.spendable)+"]")
            accountNumbers.add(account.accountNumber)
        }

        requireActivity().runOnUiThread {
            dataAdapter!!.notifyDataSetChanged()
        }
    }

    @Throws(Exception::class)
    private fun getSpendableForSelectedAccount(): Long {
        return wallet.spendableForAccount(accountNumbers[send_account_spinner.selectedItemPosition], requiredConfirmations)
    }

    private fun validateAmount(sending: Boolean): Boolean {
        val s = amount_dcr.text.toString()

        if (s.isEmpty()) {
            if (sending) {
                send_error_label.text = getString(R.string.amount_is_empty)
            }
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
            if (sending) {
                send_error_label.text = getString(R.string.invalid_amount)
            }
            return false
        }
        send_error_label.text = ""
        return true
    }

    private fun setInvalid() {
        send_dcr_estimate_size.setText(R.string._0_bytes)
        send_dcr_estimate_fee.setText(R.string._0_00_dcr)
        send_dcr_balance_after.setText(R.string._0_00_dcr)
        send_btn.isEnabled = false
        send_btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.blackTextColor38pc))
    }

    private fun constructTransaction(){

        if(!validateAmount(false)){
            setInvalid()
            return
        }

        try {

            if(send_dcr_address.text.toString().isNotEmpty() && !wallet.isAddressValid(send_dcr_address.text.toString())){
                tvDestinationError.setText(R.string.invalid_destination_address)
                return
            }

            val transaction = wallet.constructTransaction(destinationAddress, amount, accountNumbers[send_account_spinner.selectedItemPosition], requiredConfirmations, isSendAll)

            val estFee = Mobilewallet.amountCoin(Utils.signedSizeToAtom(transaction.estimatedSignedSize))

            send_dcr_estimate_fee.text = CoinFormat.format(estFee)

            send_dcr_estimate_size.text = String.format(Locale.getDefault(),"%d %s",transaction.estimatedSignedSize, getString(R.string.bytes))

            if(wallet.isAddressValid(send_dcr_address.text.toString())){
                send_btn.isEnabled = true
                send_btn.setTextColor(Color.WHITE)
            }

            if(isSendAll){
                send_dcr_balance_after.text = CoinFormat.format(getSpendableForSelectedAccount() - transaction.totalPreviousOutputAmount)

                amount_dcr.removeTextChangedListener(amountWatcher)
                amount_dcr.setText(Utils.formatDecredWithoutComma(amount - Utils.signedSizeToAtom(transaction.estimatedSignedSize)))
                amount_dcr.addTextChangedListener(amountWatcher)

                if(exchangeDecimal != null) {
                    var currentAmount = BigDecimal(Mobilewallet.amountCoin(transaction.totalPreviousOutputAmount - Utils.signedSizeToAtom(transaction.estimatedSignedSize)))
                    currentAmount = currentAmount.setScale(9, RoundingMode.HALF_UP)

                    val convertedAmount = currentAmount.multiply(exchangeDecimal)
                    amount_usd.removeTextChangedListener(exchangeWatcher)
                    amount_usd.setText(formatter.format(convertedAmount.toDouble()))
                    amount_usd.addTextChangedListener(exchangeWatcher)
                }
            }else{
                send_dcr_balance_after.text = CoinFormat.format(getSpendableForSelectedAccount() - amount)
            }

        }catch (e: Exception){
            setInvalid()
            send_error_label.text = Utils.translateError(requireActivity().applicationContext, e)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCANNER_ACTIVITY_RESULT_CODE) {
            if(resultCode == RESULT_OK) {
                try {
                    var returnString = data!!.getStringExtra(Constants.ADDRESS)
                    if(returnString.startsWith(getString(R.string.decred_colon)))
                        returnString = returnString.replace(getString(R.string.decred_colon),"")
                    if(returnString.length < 25){
                        Toast.makeText(requireContext(), R.string.wallet_add_too_short, Toast.LENGTH_SHORT).show()
                        return
                    }else if(returnString.length > 36){
                        Toast.makeText(requireContext(), R.string.wallet_addr_too_long, Toast.LENGTH_SHORT).show()
                        return
                    }

                    //TODO: Make available for mainnet
                    if(returnString.startsWith("T")){
                        send_dcr_address.setText(returnString)
                    }else{
                        Toast.makeText(requireContext(), R.string.invalid_address_prefix, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), R.string.error_not_decred_address, Toast.LENGTH_LONG).show()
                    send_dcr_address.setText("")
                }
            }
        }
    }

    private fun showInputPassPhraseDialog() {
        if(context == null || activity == null){
            return
        }

        send_error_label.text = null
        val dialogTitle = CoinFormat.format(
                String.format(Locale.getDefault(), "%s %s DCR", getString(R.string.send), Utils.removeTrailingZeros(Mobilewallet.amountCoin(amount)))
        )

        val dialogBuilder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val dialogView = inflater.inflate(R.layout.input_passphrase_box, null)
        dialogBuilder.setCancelable(false)
        dialogBuilder.setView(dialogView)

        val passphrase = dialogView.findViewById<EditText>(R.id.passphrase_input)

        dialogBuilder.setView(dialogView)

        dialogBuilder.setTitle(dialogTitle)
        dialogBuilder.setMessage(String.format(Locale.getDefault(),"%s %s DCR", getString(R.string.transaction_confirmation), Utils.formatDecred(amount)))

        dialogBuilder.setPositiveButton(R.string.done) { _, _ ->
            val pass = passphrase.text.toString()
            if (pass.isNotEmpty()) {
                val srcAccount = accountNumbers[send_account_spinner.selectedItemPosition]
                try {
                    val unsignedTransaction = wallet.constructTransaction(send_dcr_address.text.toString(), amount, srcAccount, requiredConfirmations, isSendAll)
                    val transactionDialog = ConfirmTransactionDialog(context)
                            .setAddress(send_dcr_address.text.toString())
                            .setAmount(amount)
                            .setFee(unsignedTransaction.estimatedSignedSize)
                            .setPositiveButton(getString(R.string.send), DialogInterface.OnClickListener { _, _ ->
                                startTransaction(pass)
                            })
                    transactionDialog.show()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        dialogBuilder.setNegativeButton(R.string.cancel, null)

        val b = dialogBuilder.create()
        b.show()
        b.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLUE)
    }

    private fun startTransaction(passphrase: String){
        if(context == null){
            return
        }
        pd = Utils.getProgressDialog(context,false,false,"Processing...")
        pd!!.show()
        Thread{
            try {
                val srcAccount = accountNumbers[send_account_spinner.selectedItemPosition]
                val txHash = wallet.sendTransaction(passphrase.toByteArray(), send_dcr_address.text.toString(), amount, srcAccount, requiredConfirmations, isSendAll)
                txHash.reverse()
                val sb = StringBuilder()
                for(byte in txHash){
                    sb.append(String.format(Locale.getDefault(),"%02x", byte))
                }
                println("Hash: $sb")
                if(activity != null){
                    requireActivity().runOnUiThread {
                        if(pd!!.isShowing){ pd!!.dismiss() }
                        prepareAccounts()
                        showTxConfirmDialog(sb.toString())
                    }
                }
            }catch (e: Exception){
                e.printStackTrace()
                if(activity != null){
                    requireActivity().runOnUiThread {
                        if(pd!!.isShowing){ pd!!.dismiss() }
                        send_error_label.text = Utils.translateError(context, e)
                    }
                }
            }
        }.start()
    }

    private fun showTxConfirmDialog(txHash: String) {
        if(activity == null || context == null){
            return
        }

         InfoDialog(context)
                 .setDialogTitle(getString(R.string.transaction_was_successful))
                 .setMessage("${getString(R.string.hash_colon)}\n$txHash")
                 .setIcon(R.drawable.np_amount_withdrawal)
                 .setTitleTextColor(ContextCompat.getColor(requireContext(), R.color.greenLightTextColor))
                 .setMessageTextColor(ContextCompat.getColor(requireContext(), R.color.blue))
                 .setPositiveButton(getString(R.string.close_cap), null)
                 .setNegativeButton(getString(R.string.view_cap), DialogInterface.OnClickListener { _, _ -> run {
                     if (activity != null && activity is MainActivity) {
                         val mainActivity = activity as MainActivity
                         mainActivity.displayOverview()
                     }
                 }})
                 .setMessageClickListener(View.OnClickListener {
                    Utils.copyToClipboard(context, txHash, getString(R.string.tx_hash_copy))
                 }).show()

        amount_dcr.text = null

        send_dcr_address.removeTextChangedListener(addressWatcher)
        send_dcr_address.text = null
        send_dcr_address.addTextChangedListener(addressWatcher)
    }

    private val addressWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable) {
            if (s.toString() == "") {
                tvDestinationError.setText(R.string.empty_destination_address)
                send_btn.isEnabled = false
                send_btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.blackTextColor38pc))
            } else if (!constants.wallet.isAddressValid(s.toString())) {
                tvDestinationError.setText(R.string.invalid_destination_address)
                send_btn.isEnabled = false
                send_btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.blackTextColor38pc))
            } else {
                tvDestinationError.text = null
                tvDestinationError.visibility = View.VISIBLE
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

            if(exchangeDecimal != null){
                if(s.isNotEmpty()){
                    var currentAmount = BigDecimal(s.toString())
                    currentAmount = currentAmount.setScale(9, RoundingMode.HALF_UP)
                    val convertedAmount = currentAmount.multiply(exchangeDecimal)

                    amount_usd.run {
                        removeTextChangedListener(exchangeWatcher)
                        setText(formatter.format(convertedAmount.toDouble()))
                        addTextChangedListener(exchangeWatcher)
                    }
                }else{
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

    private val exchangeWatcher: TextWatcher = object :TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable) {
            if (exchangeDecimal == null){
                return
            }

            isSendAll = false
            if (s.isEmpty()){
                amount_dcr.run {
                    removeTextChangedListener(amountWatcher)
                    text = null
                    addTextChangedListener(amountWatcher)
                }
            }else {
                var currentAmount = BigDecimal(s.toString())
                currentAmount = currentAmount.setScale(9, RoundingMode.HALF_UP)

                val convertedAmount = currentAmount.divide(exchangeDecimal, MathContext.DECIMAL128)
                amount_dcr.run {
                    removeTextChangedListener(amountWatcher)
                    setText(formatter.format(convertedAmount.toDouble()))
                    addTextChangedListener(amountWatcher)
                }
            }

            constructTransaction()
        }
    }

    private class GetExchangeRate(private val sendFragment: SendFragment) : AsyncTask<Void, String, String>() {

        override fun doInBackground(vararg voids: Void): String? {
            try {
                if(sendFragment.activity == null){
                    return null
                }
                val url = URL(sendFragment.requireActivity().getString(R.string.dcr_to_usd_exchange_url))
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.readTimeout = 7000
                connection.connectTimeout = 7000
                connection.setRequestProperty(Constants.USER_AGENT, sendFragment.util!!.get(Constants.USER_AGENT, ""))
                val br = BufferedReader(InputStreamReader(connection.inputStream))
                val result = StringBuilder()
                br.lineSequence().forEach {
                    result.append(it)
                }
                br.close()
                connection.disconnect()
                return result.toString()
            } catch (e: MalformedURLException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return null
        }

        override fun onPostExecute(s: String?) {
            super.onPostExecute(s)
            if(sendFragment.activity == null){
                return
            }

            val context = sendFragment.requireActivity().applicationContext
            val index = Integer.parseInt(sendFragment.util!!.get(Constants.CURRENCY_CONVERSION, "0"))
            val currency = context.resources.getStringArray(R.array.currency_conversion_abbrv)[index]
            val source = context.resources.getStringArray(R.array.currency_conversion_source)[index]

            if (s == null) {
                sendFragment.rate_unavailable.visibility = View.VISIBLE
                sendFragment.rate_unavailable.text = String.format("%s %s", source, context.getString(R.string.exchange_rate_unavailable))
                return
            }
            try {
                sendFragment.rate_unavailable.visibility = View.GONE
                val apiResult = JSONObject(s)
                if (apiResult.getBoolean("success")) {
                    val result = apiResult.getJSONObject("result")
                    sendFragment.exchangeRate = result.getDouble("Last")
                    sendFragment.exchangeDecimal = BigDecimal(sendFragment.exchangeRate)
                            .setScale(9, RoundingMode.HALF_UP)
                    sendFragment.send_dcr_exchange_rate.text = String.format(Locale.getDefault(), "%.2f %s/DCR (%s)", result.getDouble("Last"), currency, source)
                    sendFragment.exchange_details.visibility = View.VISIBLE
                    if (sendFragment.amount_dcr.text.isNotEmpty()) {
                        var currentAmount = BigDecimal(sendFragment.amount_dcr.text.toString())
                        currentAmount = currentAmount.setScale(9, RoundingMode.HALF_UP)

                        val convertedAmount = currentAmount.multiply(sendFragment.exchangeDecimal)
                        sendFragment.amount_usd.removeTextChangedListener(sendFragment.exchangeWatcher)
                        sendFragment.amount_usd.setText(sendFragment.formatter.format(convertedAmount.toDouble()))
                        sendFragment.amount_usd.addTextChangedListener(sendFragment.exchangeWatcher)
                    }

                    sendFragment.exchange_layout.visibility = View.VISIBLE
                } else {
                    Toast.makeText(context, "${context.getString(R.string.exchange_rate_error)}: ${apiResult.getString("message")}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }

        }
    }
}