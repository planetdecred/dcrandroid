/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.util

import android.view.View
import androidx.fragment.app.FragmentManager
import com.dcrandroid.data.Account
import com.dcrandroid.data.Constants
import com.dcrandroid.dialog.AccountPickerDialog
import com.dcrandroid.extensions.openedWalletsList
import dcrlibwallet.LibWallet
import kotlinx.android.synthetic.main.account_custom_spinner.view.*

class AccountCustomSpinner(private val fragmentManager: FragmentManager, private val spinnerLayout: View,
                           var sourceAccountChanged: ((Account) -> Unit?)? = null) : View.OnClickListener {

    val context = spinnerLayout.context

    private val multiWallet = WalletData.multiWallet
    var wallet: LibWallet

    var selectedAccount: Account? = null
    set(value) {
        spinnerLayout.spinner_account_name.text = value!!.accountName
        spinnerLayout.spinner_wallet_name.text =  wallet.walletName
        spinnerLayout.spinner_total_balance.text = CoinFormat.format(value.totalBalance)

        field = value
    }

    init {
        // Set default selected account as "default"
        // account from the first opened wallet
        wallet = multiWallet!!.openedWalletsList()[0]
        // TODO: Remove required confimation param
        selectedAccount = Account.from(wallet.getAccount(Constants.DEFAULT_ACCOUNT_NUMBER, Constants.REQUIRED_CONFIRMATIONS))
        sourceAccountChanged?.let { it1 -> it1(selectedAccount!!) }
        spinnerLayout.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        AccountPickerDialog{
            wallet = multiWallet!!.getWallet(it.walletID)
            selectedAccount = it
            sourceAccountChanged?.let { it1 -> it1(it) }
        }.show(fragmentManager, null)
    }

    fun getCurrentAddress(): String{
        return wallet.currentAddress(selectedAccount!!.accountNumber)
    }

    fun getNewAddress(): String{
        return wallet.nextAddress(selectedAccount!!.accountNumber)
    }
}