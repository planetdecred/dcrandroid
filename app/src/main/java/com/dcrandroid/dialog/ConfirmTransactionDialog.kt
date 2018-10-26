package com.dcrandroid.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.TextView
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.util.PreferenceUtil
import com.dcrandroid.util.Utils
import kotlinx.android.synthetic.main.confirm_tx_dialog.*
import mobilewallet.Mobilewallet
import java.text.DecimalFormat

class ConfirmTransactionDialog(context: Context?) : Dialog(context), View.OnClickListener {

    private var btnPositiveClick: DialogInterface.OnClickListener? = null

    private var amount: Long? = null

    private var address: CharSequence? = null

    private var fee: Long? = null

    private val util = PreferenceUtil(getContext())

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.confirm_tx_dialog)

        findViewById<TextView>(R.id.btn_positive).setOnClickListener(this)
        findViewById<TextView>(R.id.btn_negative).setOnClickListener(this)

        val format = DecimalFormat()
        format.applyPattern("#.########")

        val tvTitle = findViewById<TextView>(R.id.title)

        tvTitle.text = context.getString(R.string.sending) + " ${format.format(Mobilewallet.amountCoin(amount!!))} DCR"

        val tvAddress = findViewById<TextView>(R.id.address)
        tvAddress.text = "${context.getString(R.string.to)} $address"

        val tvFee = findViewById<TextView>(R.id.fee)

        val estFee = Utils.signedSizeToAtom(fee!!)
        tvFee.text = "${context.getString(R.string.withFeeOff)} ${format.format(Mobilewallet.amountCoin(estFee))} DCR ($fee B)"

        val tvTotal = findViewById<TextView>(R.id.total)

        tvTotal.text = "${context.getString(R.string.total)} ${Mobilewallet.amountCoin(amount!! + estFee)} DCR"

        passphrase_input.addTextChangedListener(passphraseTextWatcher)

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

    fun setFee(fee: Long): ConfirmTransactionDialog {
        this.fee = fee
        return this
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.btn_negative -> {
                cancel()
            }
            R.id.btn_positive -> {
                dismiss()
                if (btnPositiveClick != null) {
                    util.set(Constants.PASSPHRASE, passphrase_input.text.toString())
                    btnPositiveClick?.onClick(this, DialogInterface.BUTTON_POSITIVE)
                }
            }
        }
    }

    private val passphraseTextWatcher: TextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            if(passphrase_input.text.isNotEmpty()) {
                btn_positive.isEnabled = true
                btn_positive.setTextColor(ContextCompat.getColor(getContext(), R.color.ButtonColor))
            } else {
                btn_positive.isEnabled = false
                btn_positive.setTextColor(ContextCompat.getColor(getContext(), R.color.greyViewDivider))
            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }
}