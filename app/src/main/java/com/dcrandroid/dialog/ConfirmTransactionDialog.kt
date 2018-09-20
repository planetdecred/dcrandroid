package com.dcrandroid.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.TextView
import com.dcrandroid.R
import com.dcrandroid.util.Utils
import mobilewallet.Mobilewallet
import java.text.DecimalFormat

class ConfirmTransactionDialog(context: Context?) : Dialog(context), View.OnClickListener{

    private var btnPositiveClick : DialogInterface.OnClickListener? = null

    private var amount : Long? = null

    private var address : CharSequence? = null

    private var fee : Long? = null

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

        tvTitle.text =  context.getString(R.string.sending) + " ${format.format(Mobilewallet.amountCoin(amount!!))} DCR"

        val tvAddress = findViewById<TextView>(R.id.address)
        tvAddress.text = address

        val tvFee = findViewById<TextView>(R.id.fee)

        val estFee = Utils.signedSizeToAtom(fee!!)
        tvFee.text = "${format.format(Mobilewallet.amountCoin(estFee!!))} DCR ${context.getString(R.string.fee_cap)} (${format.format(fee!! / 1024F)} kB)"

        val tvTotal = findViewById<TextView>(R.id.total)

        tvTotal.text = "Total ${Mobilewallet.amountCoin(amount!! + estFee)} DCR"
    }

    fun setPositiveButton(text: String, listener: DialogInterface.OnClickListener?) : ConfirmTransactionDialog{
        btnPositiveClick = listener
        return this
    }

    fun setAmount(amount : Long) : ConfirmTransactionDialog{
        this.amount = amount
        return this
    }

    fun setAddress(address: String) : ConfirmTransactionDialog{
        this.address = address
        return this
    }

    fun setFee(fee: Long) : ConfirmTransactionDialog{
        this.fee = fee
        return this
    }

    override fun onClick(v: View?) {
        when(v!!.id){
            R.id.btn_negative -> {
                cancel()
            }
            R.id.btn_positive -> {
                dismiss()
                if(btnPositiveClick != null) {
                    btnPositiveClick?.onClick(this, DialogInterface.BUTTON_POSITIVE)
                }
            }
        }
    }
}