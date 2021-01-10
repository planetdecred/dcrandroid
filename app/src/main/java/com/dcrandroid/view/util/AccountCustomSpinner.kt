/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.view.util

import android.view.View
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentManager
import com.dcrandroid.adapter.DisabledAccounts
import com.dcrandroid.data.Account
import com.dcrandroid.data.Constants
import com.dcrandroid.dialog.AccountPickerDialog
import com.dcrandroid.extensions.*
import com.dcrandroid.util.CoinFormat
import com.dcrandroid.util.WalletData
import dcrlibwallet.Wallet
import kotlinx.android.synthetic.main.account_custom_spinner.view.*
import java.util.*

class AccountCustomSpinner(private val fragmentManager: FragmentManager, private val spinnerLayout: View,
                           @StringRes val pickerTitle: Int, val disabledAccounts: EnumSet<DisabledAccounts>, var selectedAccountChanged: ((AccountCustomSpinner) -> Unit?)? = null) : View.OnClickListener {

    val context = spinnerLayout.context

    private val multiWallet = WalletData.multiWallet
    var wallet: Wallet

    var selectedAccount: Account? = null
        set(value) {
            if (value != null) {
                if (value.walletID != wallet.id) {
                    wallet = multiWallet!!.walletWithID(value.walletID)
                }

                spinnerLayout.spinner_account_name.text = value.accountName
                spinnerLayout.spinner_wallet_name.text = wallet.name
                spinnerLayout.spinner_total_balance.text = CoinFormat.format(value.totalBalance)
            }

            field = value

            selectedAccountChanged?.let { it1 -> it1(this) }
        }

    init {
        // Set default selected account as "default"
        // account from the first opened wallet
        wallet = if (!disabledAccounts.contains(DisabledAccounts.WatchOnlyWalletAccount)) {
            multiWallet!!.openedWalletsList()[0]
        } else {
            multiWallet!!.fullCoinWalletsList()[0]
        }


        selectedAccount = Account.from(wallet.getAccount(Constants.DEF_ACCOUNT_NUMBER))
        spinnerLayout.setOnClickListener(this)

        val visibleAccounts = wallet.walletAccounts()
                .dropLastWhile { it.accountNumber == Int.MAX_VALUE }.size

        if (multiWallet.openedWalletsCount() == 1 && visibleAccounts == 1) {
            spinnerLayout.setOnClickListener(null)
            spinnerLayout.spinner_dropdown.visibility = View.INVISIBLE
        }
    }

    override fun onClick(v: View?) {
        AccountPickerDialog(pickerTitle, selectedAccount!!, disabledAccounts) {
            selectedAccount = it
            return@AccountPickerDialog Unit
        }.show(fragmentManager, null)
    }

    fun getCurrentAddress(): String {
        return wallet.currentAddress(selectedAccount!!.accountNumber)
    }

    fun getNewAddress(): String {
        return wallet.nextAddress(selectedAccount!!.accountNumber)
    }

    fun refreshBalance() {
        val account = wallet.getAccount(selectedAccount!!.accountNumber)
        selectedAccount = Account.from(account)
    }

    fun isVisible(): Boolean {
        return spinnerLayout.visibility == View.VISIBLE
    }

    fun show() {
        spinnerLayout.show()
    }

    fun hide() {
        spinnerLayout.hide()
    }

}