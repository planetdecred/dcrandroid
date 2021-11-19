/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.dialog

import android.os.Bundle
import android.text.InputFilter.LengthFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dcrandroid.R
import com.dcrandroid.view.util.InputHelper
import dcrlibwallet.Dcrlibwallet
import kotlinx.android.synthetic.main.rename_account_sheet.*

const val MAX_NAME_LENGTH = 32

class RequestNameDialog(
    private val dialogTitle: Int, private val currentName: String,
    private val isWallet: Boolean = false, private val rename: (newName: String) -> Exception?
) : FullScreenBottomSheetDialog() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.rename_account_sheet, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        sheet_title.setText(dialogTitle)

        val accountNameInput = InputHelper(requireContext(), account_name_input) {
            btn_confirm.isEnabled = !it.isNullOrBlank() && it != currentName
            true
        }.apply {
            hidePasteButton()
            hideQrScanner()
            if (isWallet) {
                setHint(R.string.wallet_name)
            } else {
                setHint(R.string.account_name)
            }

            val filterArray = Array(1) { LengthFilter(MAX_NAME_LENGTH) }
            editText.filters = filterArray

            editText.isSingleLine = true
            editText.setText(currentName)
            editText.requestFocus()
            editText.setSelection(0, currentName.length)

        }

        btn_cancel.setOnClickListener {
            dismiss()
        }

        btn_confirm.setOnClickListener {
            it.isEnabled = false
            btn_cancel.isEnabled = false
            accountNameInput.editText.isEnabled = false

            val exception = rename(accountNameInput.validatedInput!!.trim())
            if (exception != null) {
                it.isEnabled = true
                btn_cancel.isEnabled = true
                accountNameInput.editText.isEnabled = true

                val errString = when (exception.message) {
                    Dcrlibwallet.ErrExist -> {
                        getString(R.string.wallet_name_exists)
                    }
                    Dcrlibwallet.ErrReservedWalletName -> {
                        getString(R.string.reserved_wallet_name)
                    }
                    else -> {
                        exception.message
                    }
                }
                accountNameInput.setError(errString)
            } else {
                dismiss()
            }
        }
    }
}