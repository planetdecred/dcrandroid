/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.dcrandroid.R
import dcrlibwallet.Dcrlibwallet
import kotlinx.android.synthetic.main.create_password_sheet.*

class CreatePasswordPromptFragment(var isSpending: Boolean, @StringRes var positiveButtonTitle: Int, private var clickListener: DialogButtonListener) : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.create_password_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(!isSpending){
            pass_layout.hint = getString(R.string.startup_password)
            til_confirm_pass.hint = getString(R.string.confirm_startup_password)
        }

        ed_pass.addTextChangedListener(pinWatcher)
        ed_pass.addTextChangedListener(passwordStrengthWatcher)
        ed_confirm_pass.addTextChangedListener(pinWatcher)

        btn_cancel.setOnClickListener { clickListener.onClickCancel() }

        btn_create.setText(positiveButtonTitle)
        btn_create.setOnClickListener {
            it.visibility = View.GONE
            ed_pass.isFocusable = false
            ed_confirm_pass.isFocusable = false

            btn_cancel.isEnabled = false
            btn_cancel.setTextColor(resources.getColor(R.color.colorDisabled))
            progress_bar.visibility = View.VISIBLE

            clickListener.onClickOk(ed_pass.text.toString())
        }
    }

    private val passwordStrengthWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            val progress = (Dcrlibwallet.shannonEntropy(s.toString()) / 4) * 100
            if (progress > 70) {
                pass_strength.progressDrawable = resources.getDrawable(R.drawable.password_strength_bar_strong)
            } else {
                pass_strength.progressDrawable = resources.getDrawable(R.drawable.password_strength_bar_weak)
            }

            pass_strength.progress = progress.toInt()
        }
    }

    private val pinWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(editable: Editable) {

            btn_create.isEnabled = ed_pass.text!!.isNotBlank() && ed_pass.text.toString() == ed_confirm_pass.text.toString()

            if (ed_confirm_pass.text.toString() == "") {
                til_confirm_pass.error = null
            } else {
                if (ed_pass.text.toString() != ed_confirm_pass.text.toString()) {
                    til_confirm_pass.error = getString(R.string.mismatch_password)
                } else {
                    til_confirm_pass.error = null
                }
            }
        }
    }
}
