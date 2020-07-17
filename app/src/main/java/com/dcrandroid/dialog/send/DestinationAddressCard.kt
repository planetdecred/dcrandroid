/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.dialog.send

import android.content.Context
import android.text.InputType
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.dcrandroid.R
import com.dcrandroid.adapter.DisabledAccounts
import com.dcrandroid.data.Account
import com.dcrandroid.view.util.AccountCustomSpinner
import com.dcrandroid.view.util.InputHelper
import kotlinx.android.synthetic.main.send_page_sheet.view.*
import java.util.*

class DestinationAddressCard(context: Context, val layout: LinearLayout, validateAddress: (String) -> Boolean) {

    lateinit var addressChanged: () -> Unit
    internal val destinationAccountSpinner: AccountCustomSpinner
    internal val addressInputHelper: InputHelper

    init {
        val activity = context as AppCompatActivity

        val disabledAccounts = EnumSet.of(DisabledAccounts.MixerMixedAccount)
        destinationAccountSpinner = AccountCustomSpinner(activity.supportFragmentManager,
                layout.destination_account_spinner, R.string.dest_account_picker_title, disabledAccounts)

        addressInputHelper = InputHelper(context, layout.destination_address_container, validateAddress)
        addressInputHelper.editText.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

        layout.send_dest_toggle.setOnClickListener {
            if (destinationAccountSpinner.isVisible()) {
                layout.send_dest_toggle.setText(R.string.send_to_account)
                destinationAccountSpinner.hide()
                addressInputHelper.show()
            } else {
                layout.send_dest_toggle.setText(R.string.send_to_address)
                addressInputHelper.hide()
                destinationAccountSpinner.show()
            }

            addressChanged()
        }
    }

    val isSendToAccount: Boolean
        get() = destinationAccountSpinner.isVisible()

    val destinationAddress: String?
        get() {
            if (destinationAccountSpinner.isVisible()) {
                return destinationAccountSpinner.getCurrentAddress()
            }

            return addressInputHelper.validatedInput
        }

    val estimationAddress: String?
        get() {
            if (addressInputHelper.isVisible() && addressInputHelper.isInvalid()) {  // entered address is invalid
                return null
            } else if (destinationAddress != null) {
                return destinationAddress
            }

            // address input is empty or invalid so we get from a spinner
            return destinationAccountSpinner.getCurrentAddress()
        }

    val destinationAccount: Account? // returns selected account if sending to self
        get() {
            if (destinationAccountSpinner.isVisible()) {
                return destinationAccountSpinner.selectedAccount
            }

            return null
        }

    fun clear() {
        // this would trigger addressChanged whether address is visible or not
        // but that won't be an issue since the account selector would always have
        // a valid address
        addressInputHelper.editText.text = null
    }
}