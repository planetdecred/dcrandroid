/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities.security

import android.os.Bundle
import android.view.ViewTreeObserver
import androidx.core.text.HtmlCompat
import com.dcrandroid.R
import com.dcrandroid.activities.BaseActivity
import com.dcrandroid.dialog.InfoDialog
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.show
import com.dcrandroid.view.util.InputHelper
import dcrlibwallet.Dcrlibwallet
import kotlinx.android.synthetic.main.activity_verify_message.*

class VerifyMessage : BaseActivity(), ViewTreeObserver.OnScrollChangedListener {

    lateinit var addressInputHelper: InputHelper
    lateinit var messageInputHelper: InputHelper
    lateinit var signatureInputHelper: InputHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_message)

        addressInputHelper = InputHelper(this, address_container) {
            multiWallet!!.isAddressValid(it)
        }.apply {
            setHint(R.string.address)

            textChanged = this@VerifyMessage.textChanged
        }

        messageInputHelper = InputHelper(this, message_container) { true }
                .apply {
                    hideQrScanner()
                    setHint(R.string.message)

                    textChanged = this@VerifyMessage.textChanged
                }

        signatureInputHelper = InputHelper(this, signature_container) {
            try {
                Dcrlibwallet.decodeBase64(it)
                return@InputHelper true
            } catch (e: Exception) {
                e.printStackTrace()
                return@InputHelper false
            }
        }.apply {
            hideQrScanner()
            setHint(R.string.signature)
            validationMessage = R.string.invalid_base64_string

            textChanged = this@VerifyMessage.textChanged
        }

        verify_msg_scroll.viewTreeObserver.addOnScrollChangedListener(this)
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        addressInputHelper.onResume()
        messageInputHelper.onResume()
        signatureInputHelper.onResume()
    }

    private val textChanged = {
        tv_verify_message.isEnabled = !addressInputHelper.validatedInput.isNullOrBlank()
                && !signatureInputHelper.validatedInput.isNullOrBlank()
        result_layout.hide()
    }

    override fun onScrollChanged() {
        app_bar.elevation = if (verify_msg_scroll.scrollY > 0) {
            resources.getDimension(R.dimen.app_bar_elevation)
        } else {
            0f
        }
    }

    private fun setupClickListeners() {

        go_back.setOnClickListener {
            finish()
        }

        iv_info.setOnClickListener {

            val message = HtmlCompat.fromHtml(getString(R.string.verify_message_description), 0)
            InfoDialog(this)
                    .setDialogTitle(getString(R.string.verify_message))
                    .setMessage(message)
                    .setPositiveButton(getString(R.string.got_it))
                    .show()
        }

        tv_clear.setOnClickListener {
            addressInputHelper.editText.text = null
            messageInputHelper.editText.text = null
            signatureInputHelper.editText.text = null
        }

        tv_verify_message.setOnClickListener {

            val address = addressInputHelper.validatedInput!!
            val message = messageInputHelper.validatedInput!!
            val base64Signature = signatureInputHelper.validatedInput!!

            var validSignature = false
            try {
                validSignature = multiWallet!!.verifyMessage(address, message, base64Signature)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }

            showVerificationResult(validSignature)
        }
    }


    private fun showVerificationResult(valid: Boolean) {

        val titleColor: Int
        val titleText: Int
        val iconResource: Int

        if (valid) {
            titleColor = R.color.greenTextColor
            titleText = R.string.valid_signature
            iconResource = R.drawable.ic_checkmark

        } else {
            titleColor = R.color.colorError
            titleText = R.string.invalid_signature
            iconResource = R.drawable.ic_crossmark
        }

        tv_title.setTextColor(getColor(titleColor))
        tv_title.setText(titleText)
        iv_result_icon.setImageResource(iconResource)
        result_layout.show()
    }
}