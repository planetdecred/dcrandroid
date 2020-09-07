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
import com.dcrandroid.extensions.show
import com.dcrandroid.extensions.toggleVisibility
import com.dcrandroid.util.CoinFormat
import com.dcrandroid.util.SnackBar
import dcrlibwallet.Wallet
import kotlinx.android.synthetic.main.account_details.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AccountDetailsDialog(private val ctx: Context, val walletID: Long, val account: Account,
                           val renameAccount: (newName: String) -> Exception?) : FullScreenBottomSheetDialog() {

    private val wallet: Wallet = multiWallet.walletWithID(walletID)


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.account_details, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        tv_account_name.text = if (wallet.isWatchingOnlyWallet) {
            wallet.name
        } else account.accountName

        populateBalances()

        // properties
        account_details_number.text = account.accountNumber.toString()
        account_details_keys.text = context!!.getString(R.string.key_count, account.externalKeyCount, account.internalKeyCount, account.importedKeyCount)
        if (wallet.isWatchingOnlyWallet) {
            iv_rename_account.hide()
            account_number_row.hide()
            hd_path_row.hide()

            account_details_icon.setImageResource(R.drawable.ic_watch_only_wallet)
        } else {
            account_details_path.text = account.hdPath
        }

        if (account.accountNumber == Int.MAX_VALUE) {
            iv_rename_account.hide()
            account_details_icon.setImageResource(R.drawable.ic_accounts_locked)
        }

        // click listeners
        tv_toggle_properties.setOnClickListener {
            tv_toggle_properties.text = if (account_details_properties.toggleVisibility() == View.VISIBLE) {
                context!!.getString(R.string.hide_properties)
            } else context!!.getString(R.string.show_properties)
        }

        iv_close.setOnClickListener { dismiss() }

        iv_rename_account.setOnClickListener {
            val activity = ctx as AppCompatActivity
            RequestNameDialog(R.string.rename_account, account.accountName) {

                val e = renameAccount(it)
                if (e != null) {
                    return@RequestNameDialog e
                } else {
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

    private fun populateBalances() {
        val balance = wallet.getAccountBalance(account.accountNumber)

        // Total & spendable balance
        account_details_total_balance.text = CoinFormat.format(balance.total, 0.625f)
        account_details_spendable.text = CoinFormat.format(balance.spendable)

        // Staking balances
        val stakeSum = balance.immatureReward + balance.lockedByTickets + balance.votingAuthority + balance.immatureStakeGeneration
        if (stakeSum > 0) {
            if (balance.immatureReward > 0) {
                account_details_imm_rewards.text = CoinFormat.format(balance.immatureReward)
                account_details_imm_rewards_row.show()
            }

            if(balance.lockedByTickets > 0){
                account_details_locked_by_tickets.text = CoinFormat.format(balance.lockedByTickets)
                account_details_locked_by_tickets_row.show()
            }

            if(balance.votingAuthority > 0){
                account_details_voting_authority.text = CoinFormat.format(balance.votingAuthority)
                account_details_voting_authority_row.show()
            }

            if(balance.immatureStakeGeneration > 0){
                account_details_imm_stake_gen.text = CoinFormat.format(balance.immatureStakeGeneration)
                account_details_imm_stake_gen_row.show()
            }

        } else {
            staking_balance.hide()
        }
    }

    override fun onTxOrBalanceUpdateRequired(walletID: Long?) {
        super.onTxOrBalanceUpdateRequired(walletID)
        if (walletID != null && walletID != account.walletID) {
            return
        }

        GlobalScope.launch(Dispatchers.Main) {
            populateBalances()
        }
    }

}