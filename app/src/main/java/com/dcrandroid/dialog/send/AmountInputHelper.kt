/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.dialog.send

import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import com.dcrandroid.R
import com.dcrandroid.data.Account
import com.dcrandroid.data.Constants
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.show
import com.dcrandroid.extensions.toggleVisibility
import com.dcrandroid.util.CoinFormat
import com.dcrandroid.util.GetExchangeRate
import com.dcrandroid.util.Utils
import com.dcrandroid.util.WalletData
import dcrlibwallet.Dcrlibwallet
import kotlinx.android.synthetic.main.fee_layout.view.*
import kotlinx.android.synthetic.main.send_page_amount_card.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

const val AmountRelativeSize = 0.625f
val formatter: DecimalFormat = DecimalFormat("#.####")
val dcrFormat = DecimalFormat("#.########")

class AmountInputHelper(private val layout: LinearLayout, private val scrollToBottom: () -> Unit) : TextWatcher, View.OnClickListener, GetExchangeRate.ExchangeRateCallback {

    val context = layout.context

    var exchangeEnabled = true
    var exchangeDecimal: BigDecimal? = null
    var currencyIsDCR = true

    var selectedAccount: Account? = null
        set(value) {
            layout.spendable_balance.text = context.getString(R.string.spendable_bal_format,
                    Utils.formatDecredWithComma(value!!.balance.spendable))
            field = value
        }

    var usdAmount: BigDecimal? = null
    var dcrAmount: BigDecimal? = null
    val enteredAmount: BigDecimal?
        get() {
            val s = layout.send_amount.text.toString()
            try {
                return BigDecimal(s)
            } catch (e: Exception) {
            }
            return null
        }

    val maxDecimalPlaces: Int
        get() {
            return if (currencyIsDCR)
                8
            else
                2
        }

    init {
        layout.send_amount.addTextChangedListener(this)
        layout.send_amount.setOnFocusChangeListener { _, _ ->
            setBackground()
        }

        layout.iv_send_clear.setOnClickListener(this)
        layout.iv_expand_fees.setOnClickListener(this)
        layout.swap_currency.setOnClickListener(this)
        layout.exchange_error_retry.setOnClickListener(this)

        layout.send_amount_layout.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                // focus amount input on touch
                layout.send_amount.requestFocus()
                return@setOnTouchListener true
            }
            return@setOnTouchListener false
        }

        fetchExchangeRate()
    }

    private fun fetchExchangeRate() {
        val multiWallet = WalletData.multiWallet!!
        val currencyConversion = multiWallet.readInt32ConfigValueForKey(Dcrlibwallet.CurrencyConversionConfigKey, Constants.DEF_CURRENCY_CONVERSION)

        exchangeEnabled = currencyConversion > 0

        if (!exchangeEnabled) {
            return
        }

        println("Getting exchange rate")
        val userAgent = multiWallet.readStringConfigValueForKey(Dcrlibwallet.UserAgentConfigKey)
        GetExchangeRate(userAgent, this).execute()
    }

    private fun setBackground() = GlobalScope.launch(Dispatchers.Main) {
        var backgroundResource: Int

        backgroundResource = if (layout.send_amount.hasFocus()) {
            R.drawable.input_background_active
        } else {
            R.drawable.input_background
        }

        if (layout.amount_error_text.text.isNotEmpty()) {
            backgroundResource = R.drawable.input_background_error
        }

        layout.amount_input_container.setBackgroundResource(backgroundResource)
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.iv_expand_fees -> {

                layout.fee_verbose.toggleVisibility()
                scrollToBottom()
                val img = if (layout.fee_verbose.visibility == View.VISIBLE) {
                    R.drawable.ic_collapse
                } else {
                    R.drawable.ic_expand
                }

                layout.iv_expand_fees.setImageResource(img)
            }

            R.id.swap_currency -> {
                if (exchangeDecimal == null) {
                    return
                }

                if (currencyIsDCR) {
                    layout.currency_label.setText(R.string.usd)
                } else {
                    layout.currency_label.setText(R.string.dcr)
                }

                currencyIsDCR = !currencyIsDCR

                layout.send_amount.removeTextChangedListener(this)
                if (enteredAmount != null) {
                    if (currencyIsDCR) {
                        val dcr = dcrFormat.format(dcrAmount!!.setScale(8, BigDecimal.ROUND_HALF_EVEN).toDouble())
                        layout.send_amount.setText(CoinFormat.format(dcr, AmountRelativeSize))
                    } else {
                        val usd = usdAmount!!.setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString()
                        layout.send_amount.setText(usd)
                    }

                    layout.send_amount.setSelection(layout.send_amount.text.length) //move cursor to end
                } else {
                    layout.send_amount.text = null
                }
                layout.send_amount.addTextChangedListener(this)

                displayEquivalentValue()
                amountChanged?.invoke(false)
            }
            R.id.exchange_error_retry -> {
                layout.exchange_layout.hide()
                fetchExchangeRate()
            }
            R.id.iv_send_clear -> {
                layout.send_amount.text = null
            }
        }
    }

    fun setAmountDCR(coin: Double) {
        if (coin > 0) {
            layout.send_amount.removeTextChangedListener(this)

            dcrAmount = BigDecimal(coin)
            usdAmount = dcrToUSD(exchangeDecimal, dcrAmount!!.toDouble())

            if (currencyIsDCR) {
                val dcr = Dcrlibwallet.amountAtom(coin)
                val amountString = Utils.formatDecredWithoutComma(dcr)
                layout.send_amount.setText(CoinFormat.format(amountString, AmountRelativeSize))
            } else {
                val usd = usdAmount!!.setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString()
                layout.send_amount.setText(usd)
            }

            layout.send_amount.setSelection(layout.send_amount.text.length) //move cursor to end

            layout.send_amount.addTextChangedListener(this)
            layout.currency_label.setTextColor(context.resources.getColor(R.color.darkBlueTextColor))
        } else {
            layout.send_amount.text = null
        }

        displayEquivalentValue()
        hideOrShowClearButton()
    }

    fun setAmountDCR(dcr: Long) = setAmountDCR(Dcrlibwallet.amountCoin(dcr))

    fun setError(error: String?) = GlobalScope.launch(Dispatchers.Main) {
        if (error == null) {
            layout.amount_error_text.text = null
            layout.amount_error_text.hide()
            setBackground()
            return@launch
        }

        layout.amount_error_text.apply {
            text = error
            show()
        }
        setBackground()
    }

    var amountChanged: ((byUser: Boolean) -> Unit?)? = null
    override fun afterTextChanged(s: Editable?) {
        if (s.isNullOrEmpty()) {
            layout.currency_label.setTextColor(context.resources.getColor(R.color.lightGrayTextColor))
            hideOrShowClearButton()
        } else {
            layout.currency_label.setTextColor(context.resources.getColor(R.color.darkBlueTextColor))
            hideOrShowClearButton()
            if (currencyIsDCR) {
                CoinFormat.formatSpannable(s, AmountRelativeSize)
            }
        }

        setError(null)

        if (enteredAmount != null) {
            if (currencyIsDCR) {
                dcrAmount = enteredAmount!!
                usdAmount = dcrToUSD(exchangeDecimal, dcrAmount!!.toDouble())
            } else {
                usdAmount = enteredAmount!!
                dcrAmount = usdToDCR(exchangeDecimal, usdAmount!!.toDouble())
            }
        } else {
            dcrAmount = null
            usdAmount = null
        }

        displayEquivalentValue()

        amountChanged?.invoke(true)
    }

    private fun displayEquivalentValue() {

        if (currencyIsDCR) {
            val usd = if (usdAmount == null) {
                0.0
            } else {
                usdAmount!!.toDouble()
            }

            val usdStr = formatter.format(usd)
            layout.send_equivalent_value.text = context.getString(R.string.x_usd, usdStr)
        } else {
            val dcr = if (dcrAmount == null) {
                0.0
            } else {
                dcrAmount!!.toDouble()
            }

            val dcrStr = dcrFormat.format(dcr)
            layout.send_equivalent_value.text = context.getString(R.string.x_dcr, dcrStr)
        }
    }

    private fun hideOrShowClearButton(){
        if(layout.send_amount.text.isEmpty()){
            layout.iv_send_clear.hide()
        }else{
            layout.iv_send_clear.show()
        }
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun onExchangeRateSuccess(rate: GetExchangeRate.BittrexRateParser) {
        exchangeDecimal = rate.usdRate

        GlobalScope.launch(Dispatchers.Main) {
            layout.exchange_error_layout.hide()
            layout.send_equivalent_value.show()
            layout.exchange_layout.show()
        }

        if (dcrAmount != null) {
            usdAmount = dcrToUSD(exchangeDecimal, dcrAmount!!.toDouble())
        }

        displayEquivalentValue()
        amountChanged?.invoke(false)
    }

    override fun onExchangeRateError(e: Exception) {
        e.printStackTrace()

        GlobalScope.launch(Dispatchers.Main) {
            layout.exchange_error_layout.show()

            layout.send_equivalent_value.hide()
            layout.exchange_layout.show()
        }
    }
}

fun dcrToFormattedUSD(exchangeDecimal: BigDecimal?, dcr: Double, scale: Int = 4): String {
    return formatter.format(
            dcrToUSD(exchangeDecimal, dcr)!!.setScale(scale, BigDecimal.ROUND_HALF_EVEN).toDouble())
}

fun dcrToUSD(exchangeDecimal: BigDecimal?, dcr: Double): BigDecimal? {
    val dcrDecimal = BigDecimal(dcr)
    return exchangeDecimal?.multiply(dcrDecimal)
}

fun usdToDCR(exchangeDecimal: BigDecimal?, usd: Double): BigDecimal? {
    if (exchangeDecimal == null) {
        return null
    }

    val usdDecimal = BigDecimal(usd)
    // using 8 to be safe
    return usdDecimal.divide(exchangeDecimal, 8, RoundingMode.HALF_EVEN)
}