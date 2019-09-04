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
import com.dcrandroid.util.*
import dcrlibwallet.LibWallet
import kotlinx.android.synthetic.main.account_row.view.*
import java.lang.Exception

class AccountsAdapter(private val context: Context, private val walletID: Long): RecyclerView.Adapter<AccountsAdapter.AccountsViewHolder>() {

    private val accounts: ArrayList<Account>
    private val wallet: LibWallet
    private val requiredConfirmations: Int

    init {
        val util = PreferenceUtil(context)
        requiredConfirmations = if (util.getBoolean(Constants.SPEND_UNCONFIRMED_FUNDS)) 0
        else Constants.REQUIRED_CONFIRMATIONS

        wallet = WalletData.getInstance().multiWallet.getWallet(walletID)
        accounts = wallet.walletAccounts(requiredConfirmations)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        val view = if (viewType == 0) {
            inflater.inflate(R.layout.account_row, parent, false)
        }else{
            inflater.inflate(R.layout.add_row, parent, false)
        }

        return AccountsViewHolder(view)
    }

    override fun getItemCount(): Int {
        return accounts.size + 1 // add account row
    }

    override fun getItemViewType(position: Int): Int {
        return if (position != itemCount-1) 0 else 1
    }

    override fun onBindViewHolder(holder: AccountsViewHolder, position: Int) {
        if(position != accounts.size) {
            val account = accounts[position]

            if (account.accountNumber == Int.MAX_VALUE){
                holder.icon.setImageResource(R.drawable.ic_accounts_locked)
            }else{
                holder.icon.setImageResource(R.drawable.ic_accounts)
            }

            holder.accountName.text = account.accountName
            holder.accountName.isSelected = true
            holder.totalBalance.text = CoinFormat.format(account.balance.total)
            holder.spendableBalance.text = context.getString(R.string.dcr_amount, Utils.formatDecred(account.balance.spendable))

            holder.itemView.setOnClickListener {
                AccountDetailsDialog(context, walletID, account) { newName ->
                    try{
                        wallet.renameAccount(account.accountNumber, newName)
                        account.accountName = newName
                        notifyItemChanged(position)
                    }catch (e: Exception){
                        return@AccountDetailsDialog e
                    }

                    null
                }.show()
            }
        }else{
            holder.itemView.setOnClickListener {
                val activity = context as AppCompatActivity
                AddAccountDialog(walletID) {newAccountNumber ->
                    val account = wallet.getAccount(newAccountNumber, requiredConfirmations)

                    val index = accounts.size - 1 // there's always at least 2 accounts(default & imported)
                    accounts.add(index, Account.from(account)) // inserted before imported account
                    notifyItemInserted(index)

                    SnackBar.showText(context, R.string.account_created)
                }.show(activity.supportFragmentManager, null)
            }
        }
    }

    inner class AccountsViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val icon = itemView.account_row_icon
        val accountName = itemView.account_name
        val totalBalance =  itemView.account_row_total_balance
        val spendableBalance = itemView.account_row_spendable_balance
    }
}