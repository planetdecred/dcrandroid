/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.adapter

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import com.dcrandroid.data.Account
import com.dcrandroid.util.CoinFormat
import kotlinx.android.synthetic.main.account_row.view.*

class AccountsAdapter(val accounts: ArrayList<Account>): RecyclerView.Adapter<AccountsAdapter.AccountsViewHolder>() {

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
            holder.totalBalance.text = CoinFormat.format(account.balance.total)
            holder.spendableBalance.text = CoinFormat.format(account.balance.spendable)

            if(position == 0){
                holder.itemView.setBackgroundResource(R.drawable.curved_top_ripple)
            }else{
                val outValue = TypedValue()
                holder.itemView.context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                holder.itemView.setBackgroundResource(outValue.resourceId)
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