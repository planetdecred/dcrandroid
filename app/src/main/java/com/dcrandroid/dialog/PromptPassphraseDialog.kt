/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.dialog

import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import com.dcrandroid.R
import com.dcrandroid.util.WalletData
import dcrlibwallet.Dcrlibwallet
import kotlinx.android.synthetic.main.spending_passphrase_prompt_sheet.*

class PromptPassphraseDialog(val walletID: Long, @StringRes val dialogTitle: Int, val passEntered:(passphrase: String?) -> Unit): CollapsedBottomSheetDialog() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.spending_passphrase_prompt_sheet, container, false)
    }

    var confirmed = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        dialog_title.setText(dialogTitle)

        val wallet = WalletData.multiWallet!!.getWallet(walletID)
        if(wallet.spendingPassphraseType == Dcrlibwallet.SpendingPassphraseTypePass){
            spending_pin.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            spending_pass_layout.hint = getString(R.string.spending_password)
        }

        spending_pin.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                btn_confirm.isEnabled = !s.isNullOrBlank()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        btn_cancel.setOnClickListener{dismiss()}
        btn_confirm.setOnClickListener {
            confirmed = true
            passEntered(spending_pin.text.toString())
            dismiss()
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        passEntered(null)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if(!confirmed){
            passEntered(null)
        }
    }
}