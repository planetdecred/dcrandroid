/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.data.Transaction
import com.dcrandroid.util.WalletData
import dcrlibwallet.Dcrlibwallet
import dcrlibwallet.Wallet
import kotlinx.android.synthetic.main.transaction_row.view.*

class TransactionPageAdapter(
    val context: Context,
    walletID: Long,
    val transactions: ArrayList<Transaction>
) : RecyclerView.Adapter<TransactionListViewHolder>() {

    private val layoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val requiredConfirmations: Int
    private val wallet: Wallet

    init {
        val multiWallet = WalletData.multiWallet!!
        wallet = multiWallet.walletWithID(walletID)
        requiredConfirmations = when {
            multiWallet.readBoolConfigValueForKey(
                Dcrlibwallet.SpendUnconfirmedConfigKey,
                Constants.DEF_SPEND_UNCONFIRMED
            ) -> 0
            else -> Constants.REQUIRED_CONFIRMATIONS
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionListViewHolder {
        val view = layoutInflater.inflate(R.layout.transaction_page_row, parent, false)
        return TransactionListViewHolder(view)
    }

    override fun getItemCount(): Int {
        return transactions.size
    }

    override fun onBindViewHolder(holder: TransactionListViewHolder, position: Int) {

        // background ripple
        val backgroundResource: Int = when {
            itemCount == 1 -> R.drawable.ripple_bg_surface_corners_14dp // only item on the list
            position == 0 -> R.drawable.ripple_bg_surface_top_corner_14dp
            position == (itemCount - 1) -> R.drawable.curved_bottom_ripple_14dp
            else -> R.drawable.transactions_row_bg
        }

        holder.itemView.transaction_ripple_layout.setBackgroundResource(backgroundResource)

        // setting top & bottom margin for top and bottom rows.
        val layoutParams = holder.itemView.layoutParams as RecyclerView.LayoutParams

        layoutParams.bottomMargin = when (position) {
            itemCount - 1 -> context.resources.getDimensionPixelSize(R.dimen.margin_padding_size_80)
            else -> 0
        }

        layoutParams.topMargin = when (position) {
            0 -> context.resources.getDimensionPixelSize(R.dimen.margin_padding_size_4)
            else -> 0
        }

        holder.itemView.layoutParams = layoutParams

        val transaction = transactions[position]
        populateTxRow(transaction, holder.itemView, layoutInflater)
    }

}

