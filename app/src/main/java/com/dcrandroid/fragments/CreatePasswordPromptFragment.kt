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

        if (!isSpending) {
            ed_pass.setHint(R.string.new_startup_password)
            ed_confirm_pass.setHint(R.string.confirm_new_startup_password)
        }

        ed_pass.validateInput = {

            pinWatcher.afterTextChanged(null)

            val progress = (Dcrlibwallet.shannonEntropy(it) / 4) * 100
            if (progress > 70) {
                pass_strength.progressDrawable = resources.getDrawable(R.drawable.password_strength_bar_strong)
            } else {
                pass_strength.progressDrawable = resources.getDrawable(R.drawable.password_strength_bar_weak)
            }

            pass_strength.progress = progress.toInt()

            true
        }

        ed_confirm_pass.validateInput = {
            pinWatcher.afterTextChanged(null)
            true
        }

        btn_cancel.setOnClickListener { clickListener.onClickCancel() }

        btn_create.setText(positiveButtonTitle)
        btn_create.setOnClickListener {
            it.visibility = View.GONE
            ed_pass.isFocusable = false
            ed_confirm_pass.isFocusable = false

            btn_cancel.isEnabled = false
            btn_cancel.setTextColor(resources.getColor(R.color.colorDisabled))
            progress_bar.visibility = View.VISIBLE

            clickListener.onClickOk(ed_pass.textString)
        }
    }

    private val pinWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(editable: Editable?) {

            btn_create.isEnabled = ed_pass.textString.isNotBlank() && ed_pass.textString == ed_confirm_pass.textString

            if (ed_confirm_pass.textString == "") {
                ed_confirm_pass.setError(null)
            } else {
                if (ed_pass.textString != ed_confirm_pass.textString) {
                    ed_confirm_pass.setError(getString(R.string.mismatch_password))
                } else {
                    ed_confirm_pass.setError(null)
                }
            }
        }
    }
}
