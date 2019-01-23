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
import java.text.DecimalFormat

class ConfirmTransactionDialog(context: Context) : Dialog(context), View.OnClickListener {

    private var btnPositiveClick: DialogInterface.OnClickListener? = null

    private var amount: Long? = null

    private var address: CharSequence? = null

    private var account: CharSequence? = null

    private var fee: Long? = null

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

        tvTitle.text = context.getString(R.string.sending) + " ${format.format(Dcrlibwallet.amountCoin(amount!!))} DCR"

        val tvAddress = findViewById<TextView>(R.id.address)
        tvAddress.text = address

        val tvAccount = findViewById<TextView>(R.id.account)
        tvAccount.visibility = View.GONE
        if (!account.isNullOrBlank()) {
            tvAccount.visibility = View.VISIBLE
            tvAccount.text = "($account)"
        }

        val tvFee = findViewById<TextView>(R.id.fee)

        val estFee = Utils.signedSizeToAtom(fee!!)
        tvFee.text = "${context.getString(R.string.withFeeOff)} ${format.format(Dcrlibwallet.amountCoin(estFee))} DCR ($fee B)"

        val tvTotal = findViewById<TextView>(R.id.total)

        tvTotal.text = "${context.getString(R.string.total)} ${Dcrlibwallet.amountCoin(amount!! + estFee)} DCR"

        val util = PreferenceUtil(context)
        if (util.get(Constants.SPENDING_PASSPHRASE_TYPE) == Constants.PIN) {
            passphrase_input_layout.visibility = View.GONE
            btn_positive.isEnabled = true
            btn_positive.setTextColor(ContextCompat.getColor(context, R.color.blue))
        } else {
            passphrase_input.addTextChangedListener(passphraseTextWatcher)
        }
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