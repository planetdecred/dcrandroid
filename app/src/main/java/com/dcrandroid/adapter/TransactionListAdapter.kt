/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.adapter

import android.content.Context
import android.graphics.Color
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.data.Transaction
import com.dcrandroid.dialog.txdetails.TransactionDetailsDialog
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.show
import com.dcrandroid.util.CoinFormat
import com.dcrandroid.util.WalletData
import dcrlibwallet.Dcrlibwallet
import kotlinx.android.synthetic.main.transaction_row.view.*
import java.text.SimpleDateFormat
import java.util.*

// TODO: A joint class is needed for transactions and overview pages to avoid redundancy.
class TransactionListAdapter(val context: Context, val transactions: ArrayList<Transaction>) : RecyclerView.Adapter<TransactionListViewHolder>() {

    private val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    val multiWallet = WalletData.multiWallet

    private val spendUnconfirmedFunds: Boolean

    init {
        spendUnconfirmedFunds = multiWallet!!.readBoolConfigValueForKey(Dcrlibwallet.SpendUnconfirmedConfigKey, Constants.DEF_SPEND_UNCONFIRMED)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionListViewHolder {
        val view = layoutInflater.inflate(R.layout.transaction_row, parent, false)
        return TransactionListViewHolder(view)
    }

    override fun getItemCount(): Int {
        return transactions.size
    }

    override fun onBindViewHolder(holder: TransactionListViewHolder, position: Int) {
        val transaction = transactions[position]

        if (multiWallet!!.openedWalletsCount() > 1) {
            holder.walletName.apply {
                show()
                text = transaction.walletName
            }
        } else {
            holder.walletName.hide()
        }

        if (transaction.confirmations > 1 || spendUnconfirmedFunds) {
            holder.status.setConfirmed(transaction.timestamp)
            holder.statusImg.setImageResource(R.drawable.ic_confirmed)
        } else {
            holder.status.setPending()
            holder.statusImg.setImageResource(R.drawable.ic_pending)
        }

        if (transaction.animate) {
            val blinkAnim = AnimationUtils.loadAnimation(holder.view.context, R.anim.anim_blink)
            holder.view.animation = blinkAnim
            transaction.animate = false
        }

        if (transaction.type == Dcrlibwallet.TxTypeRegular) {
            if (transaction.isMixed) {
                holder.amount.text = context.getString(R.string.mix)
            } else {
                val txAmount = if (transaction.direction == Dcrlibwallet.TxDirectionSent) {
                    -transaction.amount
                } else {
                    transaction.amount
                }
                val strAmount = CoinFormat.formatDecred(txAmount)

                holder.amount.text = CoinFormat.format(strAmount + Constants.NBSP + layoutInflater.context.getString(R.string.dcr), 0.7f)
            }

            val iconRes = when (transaction.direction) {
                0 -> R.drawable.ic_send
                1 -> R.drawable.ic_receive
                else -> R.drawable.ic_tx_transferred
            }
            holder.icon.setImageResource(iconRes)
        }

        holder.itemView.setOnClickListener {
            TransactionDetailsDialog(transaction).show(context)
        }

    }
}

class TransactionListViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
    val icon = view.tx_icon
    val amount = view.amount
    val status = view.status
    val statusImg = view.img_status
    val walletName = view.wallet_name
}

fun TextView.setPending() {
    this.setText(R.string.pending)
    this.setTextColor(Color.parseColor("#8997a5"))
}

fun TextView.setConfirmed(timestamp: Long) {
    this.text = getTimestamp(this.context, timestamp * 1000) // convert seconds to milliseconds
    this.setTextColor(Color.parseColor("#596d81"))
}

fun getTimestamp(context: Context, timestamp: Long): String {
    val txDate = GregorianCalendar()
    txDate.time = Date(timestamp)

    val today = GregorianCalendar()

    val difference = System.currentTimeMillis() - timestamp
    val yesterday: Long = 86400000

    val week = DateUtils.WEEK_IN_MILLIS
    val month = week * 4

    return when {
        DateUtils.isToday(timestamp) -> context.getString(R.string.today)
        yesterday > difference -> context.getString(R.string.yesterday)
        week > difference -> SimpleDateFormat("EE", Locale.getDefault()).format(timestamp)
        today.get(Calendar.MONTH) != txDate.get(Calendar.MONTH) && (month > difference) -> SimpleDateFormat(context.getString(R.string.month_day_format), Locale.getDefault()).format(timestamp)
        else -> SimpleDateFormat(context.getString(R.string.date_format), Locale.getDefault()).format(timestamp)
    }
}