/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import com.dcrandroid.data.Account
import com.dcrandroid.data.Constants
import com.dcrandroid.dialog.AccountDetailsDialog
import com.dcrandroid.dialog.AddAccountDialog
import com.dcrandroid.extensions.walletAccounts
import com.dcrandroid.util.CoinFormat
import com.dcrandroid.util.SnackBar
import com.dcrandroid.util.Utils
import com.dcrandroid.util.WalletData
import dcrlibwallet.Dcrlibwallet
import dcrlibwallet.Wallet
import kotlinx.android.synthetic.main.account_row.view.*

class AccountsAdapter(private val context: Context, private val walletID: Long) : RecyclerView.Adapter<AccountsAdapter.AccountsViewHolder>() {

    private val accounts: ArrayList<Account>
    private val wallet: Wallet
    private val requiredConfirmations: Int

    init {
        requiredConfirmations = when {
            WalletData.multiWallet!!.readBoolConfigValueForKey(Dcrlibwallet.SpendUnconfirmedConfigKey, Constants.DEF_SPEND_UNCONFIRMED) -> 0
            else -> Constants.REQUIRED_CONFIRMATIONS
        }

        wallet = WalletData.multiWallet!!.walletWithID(walletID)
        accounts = wallet.walletAccounts()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        val view = if (viewType == 0) {
            inflater.inflate(R.layout.account_row, parent, false)
        } else {
            inflater.inflate(R.layout.add_account_row, parent, false)
        }

        return AccountsViewHolder(view)
    }

    override fun getItemCount(): Int {
        return accounts.size + 1 // add account row
    }

    override fun getItemViewType(position: Int): Int {
        return if (position != itemCount - 1) 0 else 1
    }

    override fun onBindViewHolder(holder: AccountsViewHolder, position: Int) {
        if (position != accounts.size) {
            val account = accounts[position]

            if (account.accountNumber == Int.MAX_VALUE) {
                holder.icon.setImageResource(R.drawable.ic_accounts_locked)
            } else {
                holder.icon.setImageResource(R.drawable.ic_accounts)
            }

            holder.accountName.text = account.accountName
            holder.accountName.isSelected = true
            holder.totalBalance.text = CoinFormat.format(account.totalBalance)
            holder.spendableBalance.text = context.getString(R.string.dcr_amount, Utils.formatDecredWithComma(account.balance.spendable))

            holder.itemView.setOnClickListener {
                AccountDetailsDialog(context, walletID, account) { newName ->
                    try {
                        wallet.renameAccount(account.accountNumber, newName)
                        account.accountName = newName
                        notifyItemChanged(position)
                    } catch (e: Exception) {
                        return@AccountDetailsDialog e
                    }

                    null
                }.show(context)
            }
        } else {

            val background = when {
                wallet.seed.isNullOrBlank() -> R.drawable.curved_bottom_ripple
                else -> R.drawable.ripple
            }
            holder.itemView.setBackgroundResource(background)

            holder.itemView.setOnClickListener {
                val activity = context as AppCompatActivity
                AddAccountDialog(activity, walletID) { newAccountNumber ->
                    val account = wallet.getAccount(newAccountNumber, requiredConfirmations)

                    val index = accounts.size - 1 // there's always at least 2 accounts(default & imported)
                    accounts.add(index, Account.from(account)) // inserted before imported account
                    notifyItemInserted(index)

                    SnackBar.showText(context, R.string.account_created)
                }.show(activity.supportFragmentManager, null)
            }
        }
    }

    inner class AccountsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon = itemView.account_row_icon
        val accountName = itemView.account_name
        val totalBalance = itemView.account_row_total_balance
        val spendableBalance = itemView.account_row_spendable_balance
    }
}