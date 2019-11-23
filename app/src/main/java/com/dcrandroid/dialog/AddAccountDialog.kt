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
import com.dcrandroid.util.PassPromptTitle
import com.dcrandroid.util.PassPromptUtil
import com.dcrandroid.util.Utils
import com.dcrandroid.util.WalletData
import dcrlibwallet.Wallet
import kotlinx.android.synthetic.main.add_account_sheet.*
import kotlinx.android.synthetic.main.add_account_sheet.btn_cancel
import kotlinx.android.synthetic.main.add_account_sheet.new_account_name
import java.lang.Exception

class AddAccountDialog(private val walletID: Long, private val accountCreated:(accountNumber: Int) -> Unit): CollapsedBottomSheetDialog() {

    private var wallet: Wallet = WalletData.multiWallet!!.walletWithID(walletID)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.add_account_sheet, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        new_account_name.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                new_account_name.error = null
                btn_create.isEnabled = !s.isNullOrBlank()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        btn_cancel.setOnClickListener {
            dismiss()
        }

        btn_create.setOnClickListener{
            setEnabled(false)

            val title = PassPromptTitle(R.string.confirm_to_create_account, R.string.confirm_to_create_account, R.string.confirm_to_create_account)
            PassPromptUtil(context!!, walletID, title) { _, passphrase ->

                if(passphrase != null){
                    val newName = new_account_name.text.toString()
                    try {
                        val accountNumber = wallet.nextAccount(newName, passphrase.toByteArray())
                        dismiss()
                        accountCreated(accountNumber)
                    }catch (e: Exception){
                        new_account_name.error = Utils.translateError(context!!, e)
                    }
                }

                setEnabled(true)
                true
            }.show()
        }
    }

    private fun setEnabled(enabled: Boolean){
        btn_create.isEnabled = enabled
        btn_cancel.isEnabled = enabled
        new_account_name.isEnabled = enabled
    }
}