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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.openedWalletsList
import com.dcrandroid.extensions.show
import com.dcrandroid.extensions.totalWalletBalance
import com.dcrandroid.util.CoinFormat
import com.dcrandroid.util.Utils
import com.dcrandroid.util.WalletData
import dcrlibwallet.LibWallet
import kotlinx.android.synthetic.main.wallet_row.view.*

class WalletsAdapter(val context: Context): RecyclerView.Adapter<WalletsAdapter.WalletsViewHolder>() {

    private var wallets: ArrayList<LibWallet>
    private var expanded = -1

    init {
        val multiWallet = WalletData.getInstance().multiWallet
        wallets = multiWallet.openedWalletsList()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalletsViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        val view = inflater.inflate(R.layout.wallet_row, parent, false)
        return WalletsViewHolder(view)
    }

    override fun getItemCount(): Int {
        return wallets.size
    }

    override fun onBindViewHolder(holder: WalletsViewHolder, position: Int) {
        val wallet = wallets[position]

        holder.walletName.text = wallet.walletName
        holder.totalBalance.text = context.getString(R.string.dcr_amount,
                Utils.formatDecred(wallet.totalWalletBalance(context)))

        when{
            position == 0 -> holder.container.setBackgroundResource(R.drawable.curved_top_ripple)
            (position == itemCount - 1) && expanded != position -> holder.container.setBackgroundResource(R.drawable.curved_bottom_ripple) // last item and not expanded
            else -> {
                val outValue = TypedValue()
                context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                holder.container.setBackgroundResource(outValue.resourceId)
            }
        }

        if(position == itemCount - 1){
            holder.divider.hide()
        }else{
            holder.divider.show()
        }

        if(expanded == position){
            val adapter = AccountsAdapter(context, wallet.walletID) {position == itemCount-1}

            holder.accountsList.layoutManager = LinearLayoutManager(context)
            holder.accountsList.isNestedScrollingEnabled = false
            holder.accountsList.adapter = adapter

            holder.accountsLayout.show()
        }else{
            holder.accountsList.adapter = null
            holder.accountsLayout.hide()
        }

        holder.container.setOnClickListener {
            var currentlyExpanded: Int? = null
            expanded = when (expanded) {
                position -> -1
                -1 -> position
                else -> { // an item is currently expanded
                    currentlyExpanded = expanded
                    position
                }
            }

            if(currentlyExpanded != null){
                notifyItemChanged(currentlyExpanded)
            }
            notifyItemChanged(position)
        }
    }

    inner class WalletsViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val walletName =  itemView.wallet_name
        val totalBalance = itemView.wallet_total_balance

        val container = itemView.container
        val accountsLayout = itemView.accounts
        val divider = itemView.rv_divider

        val accountsList = itemView.account_list_rv

    }
}