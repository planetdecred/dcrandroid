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
import androidx.core.content.ContextCompat
import com.dcrandroid.R
import kotlinx.android.synthetic.main.password_dialog.*

class PasswordDialog(context: Context) : Dialog(context), View.OnClickListener {

    private var btnPositiveClick: DialogInterface.OnClickListener? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.password_dialog)

        btn_positive.setOnClickListener(this)
        btn_negative.setOnClickListener(this)

        password_input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s!!.isNotEmpty()) {
                    btn_positive.isEnabled = true
                    btn_positive.setTextColor(ContextCompat.getColor(context, R.color.blue))
                } else {
                    btn_positive.isEnabled = false
                    btn_positive.setTextColor(ContextCompat.getColor(context, R.color.lightGreyBackgroundColor))
                }
            }

        })
    }

    fun setPositiveButton(listener: DialogInterface.OnClickListener?): PasswordDialog {
        btnPositiveClick = listener
        return this
    }

    fun getPassword(): String {
        return password_input.text.toString()
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
}