/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.dialog

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dcrandroid.R
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.show
import com.dcrandroid.view.util.InputHelper
import dcrlibwallet.Dcrlibwallet
import kotlinx.android.synthetic.main.create_watch_only_sheet.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class CreateWatchOnlyWallet(val walletCreated: (walletID: Long) -> Unit) : FullScreenBottomSheetDialog() {

    lateinit var walletNameInput: InputHelper
    lateinit var extendedPublicKeyInput: InputHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.create_watch_only_sheet, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        walletNameInput = InputHelper(context!!, wallet_name) {

            walletNameInput.validationMessage = R.string.wallet_name_exists

            if (!it.isBlank()) {
                try {
                    return@InputHelper !multiWallet.walletNameExists(it)
                } catch (e: Exception) {
                    if (e.message == Dcrlibwallet.ErrReservedWalletName) {
                        walletNameInput.validationMessage = R.string.reserved_wallet_name
                    }

                    return@InputHelper false
                }
            }

            return@InputHelper true

        }.apply {
            hintTextView.setText(R.string.wallet_name)
            hideQrScanner()
            hidePasteButton()
        }

        extendedPublicKeyInput = InputHelper(context!!, extended_public_key) {

            extendedPublicKeyInput.validationMessage = R.string.invalid_key

            if (!it.isBlank()) {
                try {
                    multiWallet.validateExtPubKey(it)
                    return@InputHelper true
                } catch (e: Exception) {
                    e.printStackTrace()

                    if (e.message == Dcrlibwallet.ErrUnusableSeed) {
                        extendedPublicKeyInput.validationMessage = R.string.unusable_key
                    }

                }

                return@InputHelper false
            }

            return@InputHelper true

        }.apply {
            hintTextView.setText(R.string.extended_public_key)
            hideQrScanner()
            editText.setRawInputType(InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
        }

        walletNameInput.textChanged = this@CreateWatchOnlyWallet.textChanged
        extendedPublicKeyInput.textChanged = this@CreateWatchOnlyWallet.textChanged


        btn_cancel.setOnClickListener {
            dismiss()
        }

        btn_create.setOnClickListener {
            toggleButtons(false)

            val walletName = walletNameInput.validatedInput!!
            val extendedPublicKey = extendedPublicKeyInput.validatedInput!!

            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val wallet = multiWallet.createWatchOnlyWallet(walletName, extendedPublicKey)
                    dismiss()
                    walletCreated(wallet.id)
                } catch (e: Exception) {
                    e.printStackTrace()

                    toggleButtons(true)
                }
            }

        }
    }

    override fun onResume() {
        super.onResume()
        extendedPublicKeyInput.onResume()
    }

    private fun toggleButtons(enable: Boolean) = GlobalScope.launch(Dispatchers.Main) {
        isCancelable = enable
        btn_cancel.isEnabled = enable
        if (enable) {
            btn_create.show()
            progress_bar.hide()
        } else {
            btn_create.hide()
            progress_bar.show()
        }
    }

    private val textChanged = {
        btn_create.isEnabled = !walletNameInput.validatedInput.isNullOrBlank()
                && !extendedPublicKeyInput.validatedInput.isNullOrBlank()
    }

}