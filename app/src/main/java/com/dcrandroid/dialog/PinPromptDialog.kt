/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import com.dcrandroid.R
import com.dcrandroid.view.PinViewUtil
import kotlinx.android.synthetic.main.pin_prompt_sheet.*

class PinPromptDialog(val walletID: Long, @StringRes val dialogTitle: Int, val isSpendingPass: Boolean, val passEntered:(passphrase: String?) -> Unit): CollapsedBottomSheetDialog() {

    var confirmed = false
    private lateinit var pinViewUtil: PinViewUtil

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.pin_prompt_sheet, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        sheet_title.setText(dialogTitle)

        pinViewUtil = PinViewUtil(pin_view, pin_counter, null)

        pinViewUtil.pinChanged = {
            btn_confirm.isEnabled = it.isNotEmpty()
            Unit
        }

        pinViewUtil.pinView.onEnter = {
            onEnter()
        }

        if(isSpendingPass){
            pinViewUtil.showHint(R.string.enter_spending_pin)
        }

        btn_confirm.setOnClickListener {
            onEnter()
        }

        btn_cancel.setOnClickListener {
            passEntered(null)
            dismiss()
        }
    }

    private fun onEnter(){
        passEntered(pinViewUtil.passCode)
        dismiss()
    }
}