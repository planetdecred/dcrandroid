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
import com.dcrandroid.extensions.totalWalletBalance
import com.dcrandroid.util.CoinFormat
import dcrlibwallet.LibWallet
import kotlinx.android.synthetic.main.wallet_row.view.*

class WalletsAdapter(val wallets: ArrayList<LibWallet>): RecyclerView.Adapter<WalletsAdapter.WalletsViewHolder>() {

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
        holder.totalBalance.text = CoinFormat.format(wallet.totalWalletBalance(2, holder.itemView.context)) // TODO

        if(position == 0){
            holder.itemView.setBackgroundResource(R.drawable.curved_top_ripple)
        }else{
            val outValue = TypedValue()
            holder.itemView.context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            holder.itemView.setBackgroundResource(outValue.resourceId)
        }
    }

    inner class WalletsViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val walletName =  itemView.wallet_name
        val totalBalance = itemView.wallet_total_balance
    }
}