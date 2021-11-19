/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities.security

import android.os.Bundle
import android.view.View
import com.dcrandroid.R
import com.dcrandroid.activities.BaseActivity
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.openedWalletsList
import com.dcrandroid.extensions.show
import com.dcrandroid.view.util.InputHelper
import kotlinx.android.synthetic.main.activity_validate_address.*

class ValidateAddress : BaseActivity(), View.OnClickListener {

    private lateinit var addressInputHelper: InputHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_validate_address)

        addressInputHelper = InputHelper(this, address_container) {
            // no validation for address input
            true
        }
        addressInputHelper.setHint(R.string.address)

        addressInputHelper.textChanged = {
            result_layout.hide()
            tv_validate.isEnabled = !addressInputHelper.validatedInput.isNullOrBlank()
        }

        tv_clear.setOnClickListener(this)
        tv_validate.setOnClickListener(this)
        go_back.setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()
        addressInputHelper.onResume()
    }

    override fun onClick(v: View) {

        when (v.id) {
            R.id.tv_validate -> validateAddress()

            R.id.tv_clear -> addressInputHelper.editText.text = null

            else -> finish() // R.id.go_back
        }
    }

    private fun validateAddress() {

        val wallets = multiWallet!!.openedWalletsList()

        val address = addressInputHelper.validatedInput!!

        var icon = R.drawable.ic_crossmark
        var titleText = R.string.invalid_address
        var subtitleText: String? = null

        val addressIsValid = multiWallet!!.isAddressValid(address)
        if (addressIsValid) {
            tv_title.setTextColor(getColor(R.color.text6))
            tv_subtitle.show()

            icon = R.drawable.ic_checkmark
            titleText = R.string.valid_address

            for (wallet in wallets) {
                if (wallet.haveAddress(address)) {
                    subtitleText = getString(R.string.internal_valid_address, wallet.name)
                    tv_subtitle.setTextColor(getColor(R.color.secondary))
                    break
                } else {
                    subtitleText = getString(R.string.external_valid_address)
                    tv_subtitle.setTextColor(getColor(R.color.text3))
                }
            }
        }

        result_layout.show()
        if (!addressIsValid) {
            tv_title.setTextColor(getColor(R.color.colorError))
            tv_subtitle.hide()
        }

        tv_title.setText(titleText)
        if (subtitleText != null) {
            tv_subtitle.text = subtitleText
        }
        iv_result_icon.setImageResource(icon)
    }
}