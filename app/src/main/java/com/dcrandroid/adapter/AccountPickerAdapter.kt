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
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import com.dcrandroid.data.Account
import com.dcrandroid.util.CoinFormat
import dcrlibwallet.Wallet
import kotlinx.android.synthetic.main.account_picker_header.view.*
import kotlinx.android.synthetic.main.account_picker_row.view.*
import java.util.*

enum class DisabledAccounts(value: Int) {
    MixerChangeAccount(0),
    MixerMixedAccount(1),
    WatchOnlyWalletAccount(2),
}

class AccountPickerAdapter(val context: Context, val items: Array<Any>, val currentAccount: Account, val disabledAccounts: EnumSet<DisabledAccounts>,
                           val accountSelected: (account: Account) -> Unit?) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(context)

        val layout = when (viewType) {
            0 -> R.layout.account_picker_header
            else -> R.layout.account_picker_row
        }

        val view = inflater.inflate(layout, parent, false)
        return ViewHolder(view)
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is Wallet -> 0
            else -> 1 // is account
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        val item = items[position]
        if (item is Wallet) {
            holder.itemView.wallet_name.text = item.name
        } else if (item is Account) {

            if (item.accountNumber == Int.MAX_VALUE) {
                holder.itemView.account_row_icon.setImageResource(R.drawable.ic_accounts_locked)
            } else {
                holder.itemView.account_row_icon.setImageResource(R.drawable.ic_accounts)
            }

            if (item.accountNumber == currentAccount.accountNumber && item.walletID == currentAccount.walletID) {
                holder.itemView.iv_selected_account.setImageResource(R.drawable.ic_checkmark03)
            } else {
                holder.itemView.iv_selected_account.setImageBitmap(null)
            }

            holder.itemView.account_name.apply {
                text = item.accountName
                isSelected = true
            }

            holder.itemView.account_row_total_balance.text = CoinFormat.format(item.balance.total)

            holder.itemView.account_row_spendable_balance.text = context.getString(R.string.dcr_amount,
                    CoinFormat.formatDecred(item.balance.spendable))

            holder.itemView.setOnClickListener {
                accountSelected(item)
            }

            var disableAccount = false
            disableAccount = item.isMixerMixedAccount && disabledAccounts.contains(DisabledAccounts.MixerMixedAccount)
            disableAccount = disableAccount || (item.isMixerChangeAccount && disabledAccounts.contains(DisabledAccounts.MixerChangeAccount))
            holder.itemView.isEnabled = !disableAccount
        }
    }

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v)
}