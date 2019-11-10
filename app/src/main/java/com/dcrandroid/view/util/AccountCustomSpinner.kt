/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.view.util

import android.view.View
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentManager
import com.dcrandroid.data.Account
import com.dcrandroid.data.Constants
import com.dcrandroid.dialog.AccountPickerDialog
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.openedWalletsList
import com.dcrandroid.extensions.show
import com.dcrandroid.extensions.visibleWalletAccounts
import com.dcrandroid.util.CoinFormat
import com.dcrandroid.util.WalletData
import dcrlibwallet.Dcrlibwallet
import dcrlibwallet.LibWallet
import kotlinx.android.synthetic.main.account_custom_spinner.view.*

class AccountCustomSpinner(private val fragmentManager: FragmentManager, private val spinnerLayout: View,
                           @StringRes val pickerTitle: Int, var selectedAccountChanged: ((AccountCustomSpinner) -> Unit?)? = null) : View.OnClickListener {

    val context = spinnerLayout.context

    private val multiWallet = WalletData.multiWallet
    var wallet: LibWallet

    var selectedAccount: Account? = null
    set(value) {
        spinnerLayout.spinner_account_name.text = value!!.accountName
        spinnerLayout.spinner_wallet_name.text =  wallet.name
        spinnerLayout.spinner_total_balance.text = CoinFormat.format(value.totalBalance)

        field = value
    }

    init {

        val requiredConfirmations = if(multiWallet!!.readBoolConfigValueForKey(Dcrlibwallet.SpendUnconfirmedConfigKey, Constants.DEF_SPEND_UNCONFIRMED)){
            0
        }else{
            Constants.REQUIRED_CONFIRMATIONS
        }

        // Set default selected account as "default"
        // account from the first opened wallet
        wallet = multiWallet.openedWalletsList()[0]

        selectedAccount = Account.from(wallet.getAccount(Constants.DEF_ACCOUNT_NUMBER, requiredConfirmations))
        selectedAccountChanged?.let { it1 -> it1(this) }
        spinnerLayout.setOnClickListener(this)

        val visibleAccounts = wallet.visibleWalletAccounts(context)
                .dropLastWhile { it.accountNumber == Int.MAX_VALUE }.size

        if(multiWallet.openedWalletsCount() == 1 && visibleAccounts == 1){
            spinnerLayout.setOnClickListener(null)
            spinnerLayout.spinner_dropdown.visibility = View.INVISIBLE
        }
    }

    override fun onClick(v: View?) {
        AccountPickerDialog(pickerTitle, selectedAccount!!){
            wallet = multiWallet!!.getWallet(it.walletID)
            selectedAccount = it
            selectedAccountChanged?.let { it1 -> it1(this) }
        }.show(fragmentManager, null)
    }

    fun getCurrentAddress(): String{
        return wallet.currentAddress(selectedAccount!!.accountNumber)
    }

    fun getNewAddress(): String{
        return wallet.nextAddress(selectedAccount!!.accountNumber)
    }

    fun isVisible(): Boolean{
        return spinnerLayout.visibility == View.VISIBLE
    }

    fun show(){
        spinnerLayout.show()
    }

    fun hide(){
        spinnerLayout.hide()
    }

}