/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dcrandroid.R
import com.dcrandroid.view.util.InputHelper
import kotlinx.android.synthetic.main.rename_account_sheet.*
import java.lang.Exception

// can also rename a wallet
class RenameAccountDialog(private val currentName: String, private val isWallet:Boolean = false, private val rename:(newName: String) -> Exception?): CollapsedBottomSheetDialog() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.rename_account_sheet, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if(isWallet){
            sheet_title.setText(R.string.rename_wallet_sheet_title)
        }

        val accountNameInput = InputHelper(context!!, account_name_input){
            btn_confirm.isEnabled = !it.isNullOrBlank() && it != currentName
            true
        }.apply {
            hidePasteButton()
            hideQrScanner()
            if(isWallet){
                setHint(R.string.wallet_name)
            }else{
                setHint(R.string.account_name)
            }

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

            val exception = rename(accountNameInput.validatedInput!!)
            if (exception != null) {
                it.isEnabled = true
                btn_cancel.isEnabled = true
                accountNameInput.editText.isEnabled = true
                accountNameInput.setError(exception.message)
            }else{
                dismiss()
            }
        }
    }
}