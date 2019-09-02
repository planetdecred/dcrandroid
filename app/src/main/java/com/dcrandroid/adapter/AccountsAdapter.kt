/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.adapter

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import com.dcrandroid.data.Account
import com.dcrandroid.data.Constants
import com.dcrandroid.extensions.walletAccounts
import com.dcrandroid.util.CoinFormat
import com.dcrandroid.util.PreferenceUtil
import com.dcrandroid.util.Utils
import com.dcrandroid.util.WalletData
import kotlinx.android.synthetic.main.account_row.view.*

class AccountsAdapter(private val context: Context, private val walletID: Long,  var isLastItem:() -> Boolean): RecyclerView.Adapter<AccountsAdapter.AccountsViewHolder>() {

    private val accounts: Array<Account>

    init {
        val util = PreferenceUtil(context)
        val requiredConfirmations = if (util.getBoolean(Constants.SPEND_UNCONFIRMED_FUNDS)) 0
        else Constants.REQUIRED_CONFIRMATIONS

        val wallet = WalletData.getInstance().multiWallet.getWallet(walletID)
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
            holder.totalBalance.text = CoinFormat.format(account.balance.total)
            holder.spendableBalance.text = context.getString(R.string.dcr_amount, Utils.formatDecred(account.balance.spendable))
        }
    }

    inner class AccountsViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val icon = itemView.account_row_icon
        val accountName = itemView.account_name
        val totalBalance =  itemView.account_row_total_balance
        val spendableBalance = itemView.account_row_spendable_balance
    }
}