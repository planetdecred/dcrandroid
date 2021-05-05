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
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.show
import com.dcrandroid.view.PinViewUtil
import kotlinx.android.synthetic.main.pin_prompt_sheet.*
import kotlinx.coroutines.*

class PinPromptDialog(@StringRes val dialogTitle: Int, val isSpendingPass: Boolean,
                      val passEntered: (dialog: FullScreenBottomSheetDialog, passphrase: String?) -> Boolean) : FullScreenBottomSheetDialog() {

    var hint = R.string.enter_spending_pin
    private lateinit var pinViewUtil: PinViewUtil

    private var pinTrials = 0

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

        hint = if (isSpendingPass) {
            R.string.enter_spending_pin
        } else {
            R.string.enter_startup_pin
        }

        pinViewUtil.showHint(hint)

        btn_confirm.setOnClickListener {
            onEnter()
        }

        btn_cancel.setOnClickListener {
            passEntered(this, null)
            dismiss()
        }
    }

    override fun showError() {
        GlobalScope.launch(Dispatchers.Main) {
            pinTrials++
            var delayTime = 2000L
            if (pinTrials % 2 == 0) {
                delayTime = 5000
            }
            pinViewUtil.pinView.rejectInput = true
            pinViewUtil.showError(R.string.invalid_pin)
            btn_cancel.isEnabled = false
            btn_confirm.isEnabled = false
            btn_confirm.show()
            progress_bar.hide()

            delay(delayTime)
            withContext(Dispatchers.Main) {
                pinViewUtil.reset()
                pinViewUtil.showHint(hint)
                pinViewUtil.pinView.rejectInput = false

                btn_cancel.isEnabled = true
            }
        }
    }

    override fun setProcessing(processing: Boolean) {
        GlobalScope.launch(Dispatchers.Main) {
            pinViewUtil.pinView.rejectInput = processing
            btn_cancel.isEnabled = !processing

            if (processing) {
                btn_confirm.hide()
                progress_bar.show()
            } else {
                btn_confirm.show()
                progress_bar.hide()

                btn_confirm.isEnabled = pinViewUtil.passCode.isNotEmpty()
            }
        }
    }

    private fun onEnter() {
        val dismissDialog = passEntered(this, pinViewUtil.passCode)
        if (dismissDialog) {
            dismiss()
        } else {
            setProcessing(true)
        }
    }
}