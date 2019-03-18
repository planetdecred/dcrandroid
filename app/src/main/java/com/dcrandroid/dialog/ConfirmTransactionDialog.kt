/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.Window
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.util.PreferenceUtil
import com.dcrandroid.util.Utils
import dcrlibwallet.Dcrlibwallet
import kotlinx.android.synthetic.main.confirm_tx_dialog.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.math.MathContext


class ConfirmTransactionDialog(context: Context) : Dialog(context), View.OnClickListener {

    private var btnPositiveClick: DialogInterface.OnClickListener? = null

    private var amount: Long? = null
    private var fee: Long? = null

    private var address: CharSequence? = null
    private var account: CharSequence? = null

    private var exchangeDecimal: BigDecimal? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.confirm_tx_dialog)

        findViewById<TextView>(R.id.btn_positive).setOnClickListener(this)
        findViewById<TextView>(R.id.btn_negative).setOnClickListener(this)

        val format = DecimalFormat("#.########")

        val util = PreferenceUtil(context)

        val tvTitle = findViewById<TextView>(R.id.title)

        val amountCoin = Dcrlibwallet.amountCoin(amount!!)
        tvTitle.text = context.getString(R.string.sending) + " ${format.format(amountCoin)} DCR"

        val tvAddress = findViewById<TextView>(R.id.address)
        tvAddress.text = "${context.getString(R.string.to)} $address"

        val tvAccount = findViewById<TextView>(R.id.account)
        tvAccount.visibility = View.GONE
        if (!account.isNullOrBlank()) {
            tvAccount.visibility = View.VISIBLE
            tvAccount.text = context.getString(R.string.to_account, account)
        }

        val tvFee = findViewById<TextView>(R.id.fee)

        val estFee = Utils.signedSizeToAtom(fee!!)
        val feeCoin = Dcrlibwallet.amountCoin(estFee)
        tvFee.text = "${context.getString(R.string.withFeeOff)} ${format.format(feeCoin)} DCR"

        if (util.get(Constants.SPENDING_PASSPHRASE_TYPE) == Constants.PIN) {
            passphrase_input_layout.visibility = View.GONE
            btn_positive.isEnabled = true
            btn_positive.setTextColor(ContextCompat.getColor(context, R.color.blue))
        } else {
            passphrase_input.addTextChangedListener(passphraseTextWatcher)
        }

        // display conversion if enabled and exchange rate has been fetched
        if(exchangeDecimal != null && Integer.parseInt(util.get(Constants.CURRENCY_CONVERSION, "0")) != 0){
            val amountUSD = dcrToUSD(amountCoin)
            tvTitle.text = "${tvTitle.text} ($${format.format(amountUSD)})"

            val feeUSD = dcrToUSD(feeCoin)
            tvFee.text = "${tvFee.text} ($${format.format(feeUSD)})"
        }
    }

    private fun dcrToUSD(dcr: Double): Double{
        var currentAmount = BigDecimal(dcr)
        currentAmount = currentAmount.setScale(9, RoundingMode.HALF_UP)

        var convertedAmount = currentAmount.multiply(exchangeDecimal)

        // USD is displayed in 2 decimal places by default.
        // If the converted amount is less than two significant figures,
        // it would be rounded to the nearest significant figure.
        if(convertedAmount.toDouble() < 0.01){

            convertedAmount = convertedAmount.round(MathContext(1))

            return convertedAmount.toDouble()
        }else{
            //round to 2 decimal places
            return Math.round(convertedAmount.toDouble() * 100.0) / 100.0
        }
    }

    fun setExchangeDecimal(exchangeDecimal: BigDecimal?): ConfirmTransactionDialog{
        this.exchangeDecimal = exchangeDecimal
        return this
    }

    fun setPositiveButton(listener: DialogInterface.OnClickListener?): ConfirmTransactionDialog {
        btnPositiveClick = listener
        return this
    }

    fun setAmount(amount: Long): ConfirmTransactionDialog {
        this.amount = amount
        return this
    }

    fun setAddress(address: String): ConfirmTransactionDialog {
        this.address = address
        return this
    }

    fun setAccount(account: String): ConfirmTransactionDialog {
        this.account = account
        return this
    }

    fun setFee(fee: Long): ConfirmTransactionDialog {
        this.fee = fee
        return this
    }

    fun getPassphrase(): String {
        return passphrase_input.text.toString()
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.btn_negative -> {
                cancel()
            }
            R.id.btn_positive -> {
                dismiss()
                if (btnPositiveClick != null) {
                    btnPositiveClick?.onClick(this, DialogInterface.BUTTON_POSITIVE)
                }
            }
        }
    }

    private val passphraseTextWatcher: TextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            if (passphrase_input.text.isNullOrEmpty()) {
                btn_positive.isEnabled = false
                btn_positive.setTextColor(ContextCompat.getColor(getContext(), R.color.lightGreyBackgroundColor))
            } else {
                btn_positive.isEnabled = true
                btn_positive.setTextColor(ContextCompat.getColor(getContext(), R.color.blue))
            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }
}