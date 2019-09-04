/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.dialog

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dcrandroid.R
import kotlinx.android.synthetic.main.rename_account_sheet.*
import java.lang.Exception

class RenameAccountDialog(private val currentName: String, private val isWallet:Boolean = false, private val renameAccount:(newName: String) -> Exception?): CollapsedBottomSheetDialog() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.rename_account_sheet, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if(isWallet){
            sheet_title.setText(R.string.rename_wallet_sheet_title)
            account_name_input.hint = getString(R.string.wallet_name)
        }

        new_account_name.setText(currentName)

        new_account_name.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(s: Editable?) {
                new_account_name.error = null
                btn_confirm.isEnabled = !s.isNullOrBlank() && s.toString() != currentName
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        btn_cancel.setOnClickListener {
            dismiss()
        }

        btn_confirm.setOnClickListener {
            it.isEnabled = false
            btn_cancel.isEnabled = false
            new_account_name.isEnabled = false

            val exception = renameAccount(new_account_name.text.toString())
            if (exception != null) {
                it.isEnabled = true
                btn_cancel.isEnabled = true
                new_account_name.isEnabled = true
                new_account_name.error = exception.message
            }else{
                dismiss()
            }
        }
    }
}