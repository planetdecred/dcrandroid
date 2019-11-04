/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import com.dcrandroid.R
import com.dcrandroid.data.Account
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.toggleVisibility
import com.dcrandroid.util.CoinFormat
import com.dcrandroid.util.SnackBar
import com.dcrandroid.util.WalletData
import dcrlibwallet.LibWallet
import dcrlibwallet.MultiWallet
import kotlinx.android.synthetic.main.account_details.*
import java.lang.Exception

class AccountDetailsDialog(val ctx: Context, val walletID: Long, val account: Account, val renameAccount:(newName: String) -> Exception?) : Dialog(ctx, R.style.FullWidthDialog) {

    private var wallet: LibWallet? = null
    private var multiWallet: MultiWallet = WalletData.multiWallet!!

    init {
        this.wallet = multiWallet.getWallet(walletID)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(wallet == null){
            error("WalletID = null")
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.account_details)

        tv_account_name.text = account.accountName
        account_details_total_balance.text = CoinFormat.format(account.totalBalance, 0.625f)
        account_details_spendable.text = CoinFormat.format(account.balance.spendable, 0.7f)

        // properties
        account_details_number.text = account.accountNumber.toString()
        account_details_path.text = account.hdPath
        account_details_keys.text = context.getString(R.string.key_count, account.externalKeyCount, account.internalKeyCount, account.importedKeyCount)

        if(account.accountNumber == Int.MAX_VALUE){ // imported account
            default_account_row.hide()
            account_details_icon.setImageResource(R.drawable.ic_accounts_locked)
            iv_rename_account.hide()
        }

        // click listeners
        tv_toggle_properties.setOnClickListener {
            tv_toggle_properties.text = if (account_details_properties.toggleVisibility() == View.VISIBLE) {
                context.getString(R.string.hide_properties)
            }else context.getString(R.string.show_properties)
        }

        iv_close.setOnClickListener { dismiss() }

        iv_rename_account.setOnClickListener {
            val activity = ctx as AppCompatActivity
            RenameAccountDialog(account.accountName) {

                val e = renameAccount(it)
                if (e != null){
                    return@RenameAccountDialog e
                }else{
                    tv_account_name.text = it
                    SnackBar.showText(account_details_root, R.string.account_renamed)
                    null
                }

            }.show(activity.supportFragmentManager, null)
        }

    }


}