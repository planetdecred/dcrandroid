/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.view.util

import android.view.View
import androidx.fragment.app.FragmentManager
import com.dcrandroid.data.Account
import com.dcrandroid.data.parseAccounts
import com.dcrandroid.dialog.AccountPickerDialog
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.openedWalletsList
import com.dcrandroid.extensions.show
import com.dcrandroid.util.CoinFormat
import com.dcrandroid.util.WalletData
import dcrlibwallet.Wallet
import kotlinx.android.synthetic.main.account_custom_spinner.view.*

class AccountCustomSpinner(private val fragmentManager: FragmentManager, private val spinnerLayout: View,
                           var selectedAccountChanged: ((AccountCustomSpinner) -> Unit?)? = null) : View.OnClickListener {

    var pickerTitle: Int? = null
    private lateinit var filterAccount: (account: Account) -> Boolean

    // Set this value to make the picker use only this wallet
    var singleWalletID: Long? = null

    private val multiWallet = WalletData.multiWallet
    lateinit var wallet: Wallet

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

    // also init function
    fun init(filterAccount: (account: Account) -> Boolean) {
        this.filterAccount = filterAccount

        // Set default selected account as "default"
        // account from the first opened wallet or `walletID`
        wallet = if (singleWalletID != null) multiWallet!!.walletWithID(singleWalletID!!)
        else multiWallet!!.openedWalletsList()[0]

        val accounts = parseAccounts(wallet.accounts).accounts.filter { filterAccount(it) }
        selectedAccount = accounts[0]

        if (multiWallet.openedWalletsCount() == 0 || singleWalletID != null) {
            // hide wallet name since we're dealing with a single wallet here
            spinnerLayout.spinner_wallet_name.hide()
        }

        spinnerLayout.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        val accountPicker = AccountPickerDialog(pickerTitle!!, selectedAccount!!)
        accountPicker.accountSelected = {
            selectedAccount = it
            Unit
        }
        accountPicker.filterAccount = filterAccount
        accountPicker.singleWalletID = singleWalletID

        accountPicker.show(fragmentManager, null)
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