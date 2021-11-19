/*
 * Copyright (c) 2018-2021 The Decred developers
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PasswordPromptDialog(
    @StringRes val dialogTitle: Int, val isSpending: Boolean,
    val passEntered: (dialog: FullScreenBottomSheetDialog, passphrase: String?) -> Boolean
) : FullScreenBottomSheetDialog() {

    var confirmed = false
    var passwordTrials = 0
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.password_prompt_sheet, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        dialog_title.setText(dialogTitle)

        if (!isSpending) {
            password_input.setHint(R.string.startup_password)
        }

        password_input.validateInput = {
            btn_confirm.isEnabled = true
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

    override fun setProcessing(processing: Boolean) {
        GlobalScope.launch(Dispatchers.Main) {
            btn_cancel.isEnabled = !processing
            password_input.isEnabled = !processing

            if (!processing) {
                btn_confirm.isEnabled = password_input.textString.isNotBlank()
            } else {
                password_input.setError(null)
            }
        }
    }

    override fun showError() {
        GlobalScope.launch(Dispatchers.Main) {
            passwordTrials++
            password_input.setError(getString(R.string.invalid_password))
            password_input.isEnabled = false
            btn_confirm.isEnabled = false

            var delayTime = 2000L
            if (passwordTrials % 2 == 0) {
                delayTime = 5000L
            }

            delay(delayTime)

            password_input?.isEnabled = true
            btn_confirm?.isEnabled = true
        }
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