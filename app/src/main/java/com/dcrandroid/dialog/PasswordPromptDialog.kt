/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.dialog

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import com.dcrandroid.R
import kotlinx.android.synthetic.main.password_prompt_sheet.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PasswordPromptDialog(@StringRes val dialogTitle: Int, val isSpending: Boolean,
                           val passEntered: (dialog: FullScreenBottomSheetDialog, passphrase: String?) -> Boolean) : FullScreenBottomSheetDialog() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.password_prompt_sheet, container, false)
    }

    var confirmed = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        dialog_title.setText(dialogTitle)

        if (!isSpending) {
            password_input.setHint(R.string.startup_password)
        }

        password_input.validateInput = {
            btn_confirm.isEnabled = it.isNotBlank()
            true
        }

        btn_cancel.setOnClickListener { dismiss() }
        btn_confirm.setOnClickListener {
            confirmed = true

            btn_cancel.isEnabled = false
            btn_confirm.isEnabled = false
            val dismissDialog = passEntered(this, password_input.textString)
            if (dismissDialog) {
                dismiss()
            } else {
                setProcessing(true)
            }
        }
    }

    fun setProcessing(processing: Boolean) = GlobalScope.launch(Dispatchers.Main) {
        btn_cancel.isEnabled = !processing
        password_input.isEnabled = !processing

        if (!processing) {
            btn_confirm.isEnabled = password_input.textString.isNotBlank()
        }
    }

    fun showError() = GlobalScope.launch(Dispatchers.Main) {
        password_input.setError(getString(R.string.invalid_password))
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        passEntered(this, null)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (!confirmed) {
            passEntered(this, null)
        }
    }
}