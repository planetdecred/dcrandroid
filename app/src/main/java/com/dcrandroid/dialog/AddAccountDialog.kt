/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import com.dcrandroid.R
import com.dcrandroid.util.PassPromptTitle
import com.dcrandroid.util.PassPromptUtil
import com.dcrandroid.util.WalletData
import com.dcrandroid.view.util.InputHelper
import dcrlibwallet.Wallet
import kotlinx.android.synthetic.main.add_account_sheet.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddAccountDialog(private val fragmentActivity: FragmentActivity, private val walletID: Long, private val accountCreated: (accountNumber: Int) -> Unit) : FullScreenBottomSheetDialog() {

    private var wallet: Wallet = WalletData.multiWallet!!.walletWithID(walletID)
    private lateinit var accountNameInput: InputHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.add_account_sheet, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        accountNameInput = InputHelper(fragmentActivity, account_name_input) {
            btn_create.isEnabled = !it.isNullOrBlank()
            true
        }.apply {
            editText.isSingleLine = true
            hidePasteButton()
            hideQrScanner()
            setHint(R.string.account_name)
        }

        btn_cancel.setOnClickListener {
            dismiss()
        }

        btn_create.setOnClickListener {
            setEnabled(false)

            val title = PassPromptTitle(R.string.confirm_to_create_account, R.string.confirm_to_create_account, R.string.confirm_to_create_account)
            PassPromptUtil(fragmentActivity, walletID, title, allowFingerprint = true) { dialog, passphrase ->

                val newName = accountNameInput.validatedInput!!.trim()

                GlobalScope.launch(Dispatchers.IO) {
                    if (passphrase != null) {
                        try {
                            val accountNumber = wallet.createNewAccount(newName, passphrase.toByteArray())

                            withContext(Dispatchers.Main) {
                                dialog?.dismiss()
                                dismiss()
                                accountCreated(accountNumber)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()

                            PassPromptUtil.handleError(fragmentActivity, e, dialog)
                            withContext(Dispatchers.Main) {
                                setEnabled(true)
                            }
                        }
                    }
                }

                false
            }.show()
        }
    }

    private fun setEnabled(enabled: Boolean) {
        btn_create?.isEnabled = enabled
        btn_cancel?.isEnabled = enabled
        accountNameInput.editText.isEnabled = enabled
    }
}