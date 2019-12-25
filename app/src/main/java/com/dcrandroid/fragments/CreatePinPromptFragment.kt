/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.dcrandroid.R
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.show
import com.dcrandroid.view.PinViewUtil
import kotlinx.android.synthetic.main.create_pin_sheet.*
import kotlinx.coroutines.*

class CreatePinPromptFragment(var isSpending: Boolean, @StringRes var positiveButtonTitle: Int,
                              private val createWallet:(passphrase: String?) -> Unit?) : Fragment() {

    private var currentPassCode: String? = null
    private lateinit var pinViewUtil: PinViewUtil

    private var hint = R.string.enter_spending_pin
    private var confirmHint = R.string.enter_spending_pin_again

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.create_pin_sheet, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (!isSpending) {
            hint = R.string.enter_new_startup_pin
            confirmHint = R.string.enter_new_startup_pin_again
        }

        pinViewUtil = PinViewUtil(pin_view, pin_counter, pass_strength)

        pinViewUtil.pinChanged = {
            if (currentPassCode == null) {
                btn_create.isEnabled = it.isNotEmpty()
            } else {
                btn_create.isEnabled = it == currentPassCode!!
            }
        }

        pinViewUtil.pinView.onEnter = {
            onEnter()
        }

        btn_cancel.setOnClickListener { createWallet(null) }
        btn_back.setOnClickListener {
            currentPassCode = null
            pinViewUtil.reset()
            pinViewUtil.showHint(hint)
            togglePasswordStrength(true)
            btn_create.setText(R.string.next)
            btn_create.isEnabled = false

            btn_back.hide()
        }

        btn_create.setOnClickListener {
            onEnter()
        }

        pinViewUtil.showHint(hint)
    }

    private fun onEnter() {
        when (currentPassCode) {
            null -> {
                togglePasswordStrength(false)
                currentPassCode = pinViewUtil.passCode
                pinViewUtil.reset()
                pinViewUtil.showHint(confirmHint)
                btn_create.setText(positiveButtonTitle)
                btn_create.isEnabled = false

                btn_back.show()
            }
            pinViewUtil.passCode -> {
                btn_create.hide()
                progress_bar.show()
                pinViewUtil.pinView.rejectInput = true


                btn_back.hide()
                btn_cancel.isEnabled = false
                btn_cancel.setTextColor(resources.getColor(R.color.colorDisabled))

                createWallet(currentPassCode!!)

            }
            else -> {
                currentPassCode = null
                pinViewUtil.pinView.rejectInput = true
                pinViewUtil.showError(R.string.mismatch_passcode)

                btn_create.setText(R.string.next)
                btn_create.isEnabled = false
                btn_back.hide()

                GlobalScope.launch(Dispatchers.Default) {
                    delay(2000)
                    withContext(Dispatchers.Main) {
                        pinViewUtil.reset()
                        pinViewUtil.showHint(hint)
                        togglePasswordStrength(true)
                        pinViewUtil.pinView.rejectInput = false
                    }
                }
            }
        }
    }

    private fun togglePasswordStrength(show: Boolean) {
        val pinBottomPadding: Int

        if (show) {
            pass_strength?.visibility = View.VISIBLE
            pinBottomPadding = resources.getDimensionPixelOffset(R.dimen.margin_padding_size_128)
        } else {
            pass_strength?.visibility = View.GONE
            pinBottomPadding = resources.getDimensionPixelOffset(R.dimen.margin_padding_size_96)
        }

        val bottomBarTopMargin = -pinBottomPadding
        val bottomBarParams = bottom_bar.layoutParams as LinearLayout.LayoutParams
        bottomBarParams.topMargin = bottomBarTopMargin
        bottom_bar.layoutParams = bottomBarParams

        pin_view?.setPadding(pin_view.paddingLeft, pin_view.paddingTop, pin_view.paddingRight, pinBottomPadding)
        pin_counter?.setPadding(pin_counter.paddingLeft, pin_counter.paddingTop, pin_counter.paddingRight, pinBottomPadding)


    }
}