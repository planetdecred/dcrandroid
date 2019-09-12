/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.adapter

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.BuildConfig
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.data.Transaction
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.show
import com.dcrandroid.util.CoinFormat
import com.dcrandroid.util.PreferenceUtil
import com.dcrandroid.util.Utils
import dcrlibwallet.Dcrlibwallet
import kotlinx.android.synthetic.main.transaction_page_row.view.*

class TransactionPageAdapter(val context: Context, val transactions: ArrayList<Transaction>) : RecyclerView.Adapter<TransactionListViewHolder>() {

    private val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val util = PreferenceUtil(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionListViewHolder {
        val view = layoutInflater.inflate(R.layout.transaction_page_row, parent, false)
        return TransactionListViewHolder(view)
    }


    override fun getItemCount(): Int {
        return transactions.size
    }

    override fun onBindViewHolder(holder: TransactionListViewHolder, position: Int) {

        // backround ripple
        val backgroundResource: Int = when (position) {
            0 -> R.drawable.transactions_row_top
            itemCount - 1 -> R.drawable.transactions_row_bottom_bg
            else -> R.drawable.transactions_row_bg
        }

        holder.itemView.ripple_layout.setBackgroundResource(backgroundResource)

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
        val requiredConfs = if (util.getBoolean(Constants.SPEND_UNCONFIRMED_FUNDS)) 0 else Constants.REQUIRED_CONFIRMATIONS

        holder.itemView.ticket_price.hide()
        holder.itemView.days_to_vote.hide()
        holder.itemView.vote_reward.hide()

        if (transaction.confirmations < requiredConfs) {
            holder.status.setPending()
            holder.statusImg.setImageResource(R.drawable.ic_pending)
        } else {
            holder.status.setConfirmed(transaction.timestamp)
            holder.statusImg.setImageResource(R.drawable.ic_confirmed)
        }

        if (transaction.animate) {
            val blinkAnim = AnimationUtils.loadAnimation(holder.view.context, R.anim.anim_blink)
            holder.view.animation = blinkAnim
            transaction.animate = false
        }

        if (transaction.type == Dcrlibwallet.TxTypeRegular) run {
            val strAmount = Utils.formatDecredWithComma(transaction.amount)

            holder.amount.apply {
                text = CoinFormat.format(strAmount + Constants.NBSP + layoutInflater.context.getString(R.string.dcr), 0.7f)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, context.resources.getDimension(R.dimen.edit_text_size_20))
            }

            holder.itemView.ticket_price.hide()

            val iconRes = when {
                transaction.direction == 0 -> R.drawable.ic_send
                transaction.direction == 1 -> R.drawable.ic_receive
                else -> R.drawable.ic_tx_transferred
            }
            holder.icon.setImageResource(iconRes)
        } else if(Dcrlibwallet.compareTxFilter(Dcrlibwallet.TxFilterStaking, transaction.type, transaction.direction)){

            holder.amount.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.resources.getDimension(R.dimen.edit_text_size_18))

            holder.itemView.ticket_price.apply {
                show()
                text = CoinFormat.format(transaction.amount, 0.715f)
            }

            var icon = 0
            var title = 0
            when(transaction.type){
                Dcrlibwallet.TxTypeTicketPurchase ->{
                    if (transaction.confirmations < BuildConfig.TicketMaturity) {
                        icon = R.drawable.ic_ticket_immature
                        title = R.string.immature
                    }else{
                        icon = R.drawable.ic_ticket_live
                        title = R.string.live
                    }
                }
                Dcrlibwallet.TxTypeVote ->{
                    title = R.string.vote
                    icon = R.drawable.ic_ticket_voted
                    holder.itemView.vote_reward.show()

                    val reward = Utils.formatDecred(104044861)
                    holder.itemView.vote_reward.text = CoinFormat.format("+$reward DCR", 0.715f)
                }
                Dcrlibwallet.TxTypeRevocation -> {
                    title = R.string.revoked
                    icon = R.drawable.ic_ticket_revoked
                }
            }

            if(transaction.type != Dcrlibwallet.TxTypeTicketPurchase){
                holder.itemView.days_to_vote.show()
            }

            holder.icon.setImageResource(icon)
            holder.amount.setText(title)

        }

        holder.itemView.setOnClickListener { println("Hash: ${transaction.hash}") }

    }


}