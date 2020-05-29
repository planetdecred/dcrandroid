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
import androidx.core.text.HtmlCompat
import com.dcrandroid.R
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.show
import com.dcrandroid.util.Utils
import com.dcrandroid.view.util.InputHelper
import dcrlibwallet.Dcrlibwallet
import dcrlibwallet.Wallet
import kotlinx.android.synthetic.main.create_watch_only_sheet.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateWatchOnlyWallet(val walletCreated: (wallet: Wallet) -> Unit) : FullScreenBottomSheetDialog() {

    private var walletNameInput: InputHelper? = null
    private var extendedPublicKeyInput: InputHelper? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.create_watch_only_sheet, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (multiWallet.loadedWalletsCount() > 0) {
            walletNameInput = InputHelper(context!!, wallet_name) {
                walletNameInput?.validationMessage = R.string.wallet_name_exists

                if (!it.isBlank()) {
                    try {
                        return@InputHelper !multiWallet.walletNameExists(it)
                    } catch (e: Exception) {
                        if (e.message == Dcrlibwallet.ErrReservedWalletName) {
                            walletNameInput?.validationMessage = R.string.reserved_wallet_name
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

            walletNameInput?.textChanged = this@CreateWatchOnlyWallet.textChanged
        } else {
            wallet_name.hide()
        }

        extendedPublicKeyInput = InputHelper(context!!, extended_public_key) {

            extendedPublicKeyInput?.validationMessage = R.string.invalid_key

            if (!it.isBlank()) {
                try {
                    multiWallet.validateExtPubKey(it)
                    return@InputHelper true
                } catch (e: Exception) {
                    e.printStackTrace()

                    if (e.message == Dcrlibwallet.ErrUnusableSeed) {
                        extendedPublicKeyInput?.validationMessage = R.string.unusable_key
                    }

                }

                return@InputHelper false
            }

            return@InputHelper true

        }.apply {
            hintTextView.setText(R.string.extended_public_key2)
            hideQrScanner()
            editText.setRawInputType(InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
        }

        extendedPublicKeyInput?.textChanged = this@CreateWatchOnlyWallet.textChanged


        btn_cancel.setOnClickListener {
            dismiss()
        }

        btn_import.setOnClickListener {
            toggleUI(false)

            val walletName = getWalletName()
            val extendedPublicKey = extendedPublicKeyInput?.validatedInput!!

            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val wallet = multiWallet.createWatchOnlyWallet(walletName, extendedPublicKey)
                    dismiss()
                    walletCreated(wallet)
                } catch (e: Exception) {
                    e.printStackTrace()
                    toggleUI(true)

                    withContext(Dispatchers.Main) {
                        val op = this@CreateWatchOnlyWallet.javaClass.name + ": createWatchOnlyWallet"
                        Utils.showErrorDialog(this@CreateWatchOnlyWallet.context!!, op + ": " + e.message)
                        Dcrlibwallet.logT(op, e.message)
                    }
                }
            }

        }

        iv_info.setOnClickListener {
            InfoDialog(context!!)
                    .setDialogTitle(getString(R.string.extended_public_key2))
                    .setMessage(HtmlCompat.fromHtml(getString(R.string.ext_pub_key_info), 0))
                    .setPositiveButton(getString(R.string.got_it), null)
                    .show()
        }
    }

    override fun onResume() {
        super.onResume()
        extendedPublicKeyInput?.onResume()
    }

    private fun getWalletName(): String {
        if (multiWallet.loadedWalletsCount() == 0) {
            return getString(R.string.mywallet)
        }

        return walletNameInput?.validatedInput!!
    }

    private fun toggleUI(enable: Boolean) = GlobalScope.launch(Dispatchers.Main) {
        isCancelable = enable

        walletNameInput?.setEnabled(enable)
        extendedPublicKeyInput?.setEnabled(enable)
        btn_cancel.isEnabled = enable
        if (enable) {
            btn_import.show()
            progress_bar.hide()
        } else {
            btn_import.hide()
            progress_bar.show()
        }
    }

    private val textChanged = {
        btn_import.isEnabled = (!walletNameInput?.validatedInput.isNullOrBlank() || multiWallet.loadedWalletsCount() == 0)
                && !extendedPublicKeyInput?.validatedInput.isNullOrBlank()
    }

}