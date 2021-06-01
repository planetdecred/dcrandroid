/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.adapter

import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.text.style.RelativeSizeSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.BuildConfig
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
            holder.itemView.wallet_name.apply {
                show()
                text = transaction.walletName
            }
        } else {
            holder.itemView.wallet_name.hide()
        }
        populateTxRow(transaction, holder.itemView, layoutInflater)
    }
}

class TransactionListViewHolder(val view: View) : RecyclerView.ViewHolder(view)

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

fun populateTxRow(transaction: Transaction, layoutRow: View, layoutInflater: LayoutInflater) {

    val context = layoutRow.context
    val multiWallet = WalletData.multiWallet!!

    layoutRow.tx_icon.setImageResource(transaction.iconResource)

    layoutRow.ticket_price.hide()
    layoutRow.days_to_vote.hide()
    layoutRow.vote_reward.hide()

    if (transaction.confirmations < multiWallet.requiredConfirmations()) {
        layoutRow.status.setPending()
        layoutRow.img_status.setImageResource(R.drawable.ic_pending)
    } else {
        layoutRow.status.setConfirmed(transaction.timestamp)
        layoutRow.img_status.setImageResource(R.drawable.ic_confirmed)
    }

    if (transaction.animate) {
        val blinkAnim = AnimationUtils.loadAnimation(context, R.anim.anim_blink)
        layoutRow.animation = blinkAnim
        transaction.animate = false
    }

    if (transaction.type == Dcrlibwallet.TxTypeRegular) {
            val txAmount = if (transaction.direction == Dcrlibwallet.TxDirectionSent) {
                -transaction.amount
            } else {
                transaction.amount
            }
            val strAmount = CoinFormat.formatDecred(txAmount)

            layoutRow.amount.apply {
                text = CoinFormat.format(strAmount + Constants.NBSP + layoutInflater.context.getString(R.string.dcr), 0.7f)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, context.resources.getDimension(R.dimen.edit_text_size_20))
            }

        layoutRow.ticket_price.hide()

    } else if (transaction.type == Dcrlibwallet.TxTypeMixed) {
        val amountDcrFormat = CoinFormat.formatDecred(transaction.mixDenomination)

        layoutRow.amount.text = CoinFormat.format(context.resources.getQuantityString(R.plurals.mixed_dcr_amount, transaction.mixCount, amountDcrFormat, transaction.mixCount))
        layoutRow.ticket_price.hide()
    } else if (Dcrlibwallet.txMatchesFilter(transaction.type, transaction.direction, Dcrlibwallet.TxFilterStaking)) {

        layoutRow.amount.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.resources.getDimension(R.dimen.edit_text_size_18))

        layoutRow.ticket_price.apply {
            show()
            text = CoinFormat.format(transaction.amount, 0.715f)
        }

        var title = 0
        when (transaction.type) {
            Dcrlibwallet.TxTypeTicketPurchase -> {
                title = if (transaction.confirmations < BuildConfig.TicketMaturity) {
                    R.string.immature
                } else {
                    if (multiWallet.walletWithID(transaction.walletID)
                                    .ticketHasVotedOrRevoked(transaction.hash)) {
                        R.string.purchased
                    } else {
                        R.string.live
                    }
                }
            }
            Dcrlibwallet.TxTypeVote -> {
                title = R.string.vote
            }
            Dcrlibwallet.TxTypeRevocation -> {
                title = R.string.revoked
            }
        }

        if (transaction.type == Dcrlibwallet.TxTypeVote || transaction.type == Dcrlibwallet.TxTypeRevocation) {
            layoutRow.vote_reward.apply {
                text = CoinFormat.format(transaction.voteReward, 0.715f)
                show()
            }

            layoutRow.days_to_vote.apply {
                val daysToVoteOrRevoke = transaction.daysToVoteOrRevoke
                text = if (daysToVoteOrRevoke == 1) {
                    context.getString(R.string.one_day)
                } else {
                    context.getString(R.string.x_days, daysToVoteOrRevoke)
                }

                show()
            }
        }

        layoutRow.amount.setText(title)
    }

    layoutRow.transaction_ripple_layout.setOnClickListener {
        TransactionDetailsDialog(transaction).show(context)
    }
}