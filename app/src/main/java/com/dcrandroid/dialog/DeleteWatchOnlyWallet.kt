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
import com.dcrandroid.R
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.show
import com.dcrandroid.util.Utils
import com.dcrandroid.view.util.InputHelper
import dcrlibwallet.Dcrlibwallet
import dcrlibwallet.Wallet
import kotlinx.android.synthetic.main.delete_watch_only_wallet_sheet.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeleteWatchOnlyWallet(val wallet: Wallet, val walletDeleted: () -> Unit) : FullScreenBottomSheetDialog() {

    private var walletNameInput: InputHelper? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.delete_watch_only_wallet_sheet, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        walletNameInput = InputHelper(context!!, wallet_name) {
            true
        }.apply {
            hintTextView.setText(R.string.wallet_name)
            hideQrScanner()
            hidePasteButton()
        }

        walletNameInput?.textChanged = {
            btn_delete.isEnabled = walletNameInput?.validatedInput!! == wallet.name
        }

        btn_cancel.setOnClickListener {
            dismiss()
        }

        btn_delete.setOnClickListener {
            toggleButtons(false)
            GlobalScope.launch(Dispatchers.Default) {
                try {
                    multiWallet.deleteWallet(wallet.id, null)
                    walletDeleted()
                } catch (e: Exception) {
                    toggleButtons(true)
                    withContext(Dispatchers.Main) {
                        val op = this@DeleteWatchOnlyWallet.javaClass.name + ": createWatchOnlyWallet"
                        Utils.showErrorDialog(this@DeleteWatchOnlyWallet.context!!, op + ": " + e.message)
                        Dcrlibwallet.logT(op, e.message)
                    }
                }
            }
        }
    }

    private fun toggleButtons(enable: Boolean) = GlobalScope.launch(Dispatchers.Main) {
        isCancelable = enable
        btn_cancel.isEnabled = enable
        walletNameInput!!.setEnabled(enable)
        if (enable) {
            btn_delete.show()
            progress_bar.hide()
        } else {
            btn_delete.hide()
            progress_bar.show()
        }
    }
}
