/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities.security

import android.os.Bundle
import android.view.View
import com.dcrandroid.R
import com.dcrandroid.activities.BaseActivity
import com.dcrandroid.data.Constants
import com.dcrandroid.dialog.InfoDialog
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.show
import com.dcrandroid.util.PassPromptTitle
import com.dcrandroid.util.PassPromptUtil
import com.dcrandroid.util.SnackBar
import com.dcrandroid.util.Utils
import com.dcrandroid.view.util.InputHelper
import dcrlibwallet.Dcrlibwallet
import dcrlibwallet.Wallet
import kotlinx.android.synthetic.main.activity_sign_message.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignMessage : BaseActivity(), View.OnClickListener {

    private lateinit var addressInputHelper: InputHelper
    private lateinit var messageInputHelper: InputHelper
    private lateinit var signatureHelper: InputHelper

    private lateinit var wallet: Wallet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_message)

        val walletID = intent.getLongExtra(Constants.WALLET_ID, -1)
        wallet = multiWallet!!.walletWithID(walletID)


        addressInputHelper = InputHelper(this, address_container) {
            multiWallet!!.isAddressValid(it)
        }
        addressInputHelper.setHint(R.string.address)
        addressInputHelper.textChanged = textChanged


        messageInputHelper = InputHelper(this, message_container) { true }
        messageInputHelper.setHint(R.string.message)
        messageInputHelper.hideQrScanner()
        messageInputHelper.textChanged = textChanged


        signatureHelper = InputHelper(this, signature_container) { true }.apply {
            setHint(R.string.signature)
            hideClearButton()
            hidePasteButton()
            hideQrScanner()
            textChanged = {}

            editText.isEnabled = false
        }


        tv_clear.setOnClickListener {
            addressInputHelper.editText.text = null
            messageInputHelper.editText.text = null
        }

        go_back.setOnClickListener {
            finish()
        }

        iv_info.setOnClickListener(this)
        tv_copy.setOnClickListener(this)
        tv_sign.setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()
        addressInputHelper.onResume()
        messageInputHelper.onResume()
        signatureHelper.onResume()
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.tv_sign -> {
                val title = PassPromptTitle(R.string.confirm_to_sign, R.string.confirm_to_sign, R.string.confirm_to_sign)
                PassPromptUtil(this, wallet.id, title, allowFingerprint = true) { _, pass ->
                    if (pass != null) {
                        beginSignMessage(pass)
                    }
                    true
                }.show()
            }
            R.id.tv_copy -> {
                Utils.copyToClipboard(this, signatureHelper.editText.text.toString(), R.string.signature_copied)
            }
            R.id.iv_info -> {
                InfoDialog(this)
                        .setDialogTitle(getString(R.string.sign_message))
                        .setMessage(getString(R.string.sign_message_description))
                        .setPositiveButton(getString(R.string.got_it))
                        .show()
            }
        }
    }

    private val textChanged = {
        result_layout.hide()
        val address = addressInputHelper.validatedInput

        if (address.isNullOrBlank()) {
            tv_sign.isEnabled = false
        } else {
            if (wallet.haveAddress(address)) {
                tv_sign.isEnabled = true
            } else {
                tv_sign.isEnabled = false
                addressInputHelper.setError(getString(R.string.sign_using_external_address_error))
            }
        }
    }

    private fun toggleViews(isEnable: Boolean) = GlobalScope.launch(Dispatchers.Main) {
        tv_sign.isEnabled = isEnable
        tv_clear.isEnabled = isEnable
        go_back.isEnabled = isEnable

        if (isEnable) {
            tv_sign.show()
            progress_bar.hide()
        } else {
            tv_sign.hide()
            progress_bar.show()
        }
    }

    private fun beginSignMessage(passphrase: String) = GlobalScope.launch(Dispatchers.Default) {
        toggleViews(false)

        val address = addressInputHelper.validatedInput!!
        val message = messageInputHelper.editText.text.toString()

        try {
            val signature = wallet.signMessage(passphrase.toByteArray(), address, message)
            val signatureStr = Dcrlibwallet.encodeBase64(signature)

            withContext(Dispatchers.Main) {
                result_layout.show()
                signatureHelper.editText.setText(signatureStr)
                SnackBar.showText(this@SignMessage, R.string.message_signed)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (e.message == Dcrlibwallet.ErrInvalidPassphrase) {

                val err = if (wallet.privatePassphraseType == Dcrlibwallet.PassphraseTypePin) {
                    R.string.invalid_pin
                } else {
                    R.string.invalid_password
                }

                SnackBar.showError(this@SignMessage, err)
            }
        }

        toggleViews(true)
    }

}