/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.dialog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.dcrandroid.R
import com.dcrandroid.data.Account
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.toggleVisibility
import com.dcrandroid.util.CoinFormat
import com.dcrandroid.util.SnackBar
import dcrlibwallet.LibWallet
import kotlinx.android.synthetic.main.account_details.*
import java.lang.Exception

class AccountDetailsDialog(val ctx: Context, val walletID: Long, val account: Account,
                           val renameAccount:(newName: String) -> Exception?) : CollapsedBottomSheetDialog() {

    private var wallet: LibWallet? = null

    init {
        this.wallet = multiWallet!!.getWallet(walletID)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.account_details, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val balance = account.balance

        tv_account_name.text = account.accountName
        account_details_total_balance.text = CoinFormat.format(account.totalBalance, 0.625f)
        account_details_spendable.text = CoinFormat.format(balance.spendable)

        val stakeSum = balance.immatureReward + balance.lockedByTickets + balance.votingAuthority + balance.immatureStakeGeneration
        if(stakeSum > 0){
            account_details_imm_rewards.text = CoinFormat.format(balance.immatureReward)
            account_details_locked_by_tickets.text = CoinFormat.format(balance.lockedByTickets)
            account_details_voting_authority.text = CoinFormat.format(balance.votingAuthority)
            account_details_imm_stake_gen.text = CoinFormat.format(balance.immatureStakeGeneration)
        }else{
            staking_balance.hide()
        }

        // properties
        account_details_number.text = account.accountNumber.toString()
        account_details_path.text = account.hdPath
        account_details_keys.text = context!!.getString(R.string.key_count, account.externalKeyCount, account.internalKeyCount, account.importedKeyCount)

        if(account.accountNumber == Int.MAX_VALUE){ // imported account
            default_account_row.hide()
            account_details_icon.setImageResource(R.drawable.ic_accounts_locked)
            iv_rename_account.hide()
        }

        // click listeners
        tv_toggle_properties.setOnClickListener {
            tv_toggle_properties.text = if (account_details_properties.toggleVisibility() == View.VISIBLE) {
                context!!.getString(R.string.hide_properties)
            }else context!!.getString(R.string.show_properties)
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


        account_details_sv.viewTreeObserver.addOnScrollChangedListener {
            top_bar.elevation = if (account_details_sv.scrollY == 0) {
                0f
            } else {
                resources.getDimension(R.dimen.app_bar_elevation)
            }
        }

    }

}