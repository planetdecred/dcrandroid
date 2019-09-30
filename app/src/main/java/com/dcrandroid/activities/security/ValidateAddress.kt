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
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.show
import com.dcrandroid.view.util.InputHelper
import dcrlibwallet.LibWallet
import kotlinx.android.synthetic.main.activity_validate_address.*

class ValidateAddress: BaseActivity(), View.OnClickListener {

    lateinit var addressInputHelper: InputHelper

    lateinit var wallet: LibWallet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_validate_address)

        val walletID = intent.getLongExtra(Constants.WALLET_ID, -1)
        wallet = multiWallet.getWallet(walletID)

        addressInputHelper = InputHelper(this, address_container){
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

    override fun onClick(v: View) {

        when (v.id){
            R.id.tv_validate -> {
                result_layout.show()

                val address = addressInputHelper.validatedInput!!

                val icon: Int
                val titleText: Int
                var subtitleText: Int? = null

                if(wallet.isAddressValid(address)){
                    tv_title.setTextColor(getColor(R.color.greenTextColor))
                    tv_subtitle.show()
                    icon = R.drawable.ic_checkmark
                    titleText = R.string.valid_address

                    if(wallet.haveAddress(address)){
                        subtitleText = R.string.internal_valid_address
                        tv_subtitle.setTextColor(getColor(R.color.greenLightTextColor))
                    }else{
                        subtitleText = R.string.external_valid_address
                        tv_subtitle.setTextColor(getColor(R.color.lightGrayTextColor))
                    }

                }else{
                    tv_title.setTextColor(getColor(R.color.colorError))
                    icon = R.drawable.ic_crossmark
                    titleText = R.string.invalid_address

                    tv_subtitle.hide()
                }

                tv_title.setText(titleText)
                if (subtitleText != null) {
                    tv_subtitle.setText(subtitleText)
                }
                iv_result_icon.setImageResource(icon)
            }

            R.id.tv_clear -> addressInputHelper.editText.text = null

            else -> finish() // R.id.go_back
        }
    }
}