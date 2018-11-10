package com.dcrandroid.dialog


import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.Window
import android.widget.TextView
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.util.PreferenceUtil
import kotlinx.android.synthetic.main.confirm_tx_dialog.*

class DeleteWalletDialog(context: Context) : Dialog(context), View.OnClickListener {
    private var btnPositiveClick: DialogInterface.OnClickListener? = null

    private var title: String? = null

    private var message: String? = null


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.delete_wallet_dialog)

        findViewById<TextView>(R.id.btn_positive).setOnClickListener(this)
        findViewById<TextView>(R.id.btn_negative).setOnClickListener(this)

        val tvTitle = findViewById<TextView>(R.id.title)

        tvTitle.text = title

        val tvMessage = findViewById<TextView>(R.id.message)
        tvMessage.text = message

        val util = PreferenceUtil(context)
        if (util.get(Constants.SPENDING_PASSPHRASE_TYPE) == Constants.PIN) {
            passphrase_input.visibility = View.GONE
            btn_positive.isEnabled = true
            btn_positive.setTextColor(ContextCompat.getColor(getContext(), R.color.blue))
        } else {
            passphrase_input.addTextChangedListener(passphraseTextWatcher)
        }
    }

    fun setPositiveButton(listener: DialogInterface.OnClickListener?): DeleteWalletDialog {
        btnPositiveClick = listener
        return this
    }

    fun setTitle(title: String): DeleteWalletDialog {
        this.title = title
        return this
    }

    fun setMessage(message: String): DeleteWalletDialog {
        this.message = message
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
