/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.dialog.send

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.dcrandroid.data.Account
import com.dcrandroid.util.AccountCustomSpinner
import kotlinx.android.synthetic.main.send_page_sheet.view.*

class DestinationAddressHelper(context: Context, val layout: LinearLayout) {

    private val destinationAccountSpinner: AccountCustomSpinner
    init {
        val activity = context as AppCompatActivity
        destinationAccountSpinner = AccountCustomSpinner(activity.supportFragmentManager, layout.destination_account_spinner)
    }

    lateinit var validateAddress:(String) -> Boolean

    val destinationAddress: String?
    get() {
        if(layout.destination_account_spinner.visibility == View.VISIBLE){
            return destinationAccountSpinner.getCurrentAddress()
        }

        //validate address or return null
        return null
    }

    val estimationAddress: String?
    get() {
        if(destinationAddress != null){
            destinationAddress!! // either valid address entered or address for spinner
        }

        // address input is probably empty or invalid so we get from a spinner
        return destinationAccountSpinner.getCurrentAddress()
    }

    val destinationAccount: Account? // returns selected account if sending to self
    get() {
        if(layout.destination_account_spinner.visibility == View.VISIBLE){
            return destinationAccountSpinner.selectedAccount
        }

        return null
    }
}