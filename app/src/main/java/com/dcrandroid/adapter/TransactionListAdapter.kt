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
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.data.Transaction
import com.dcrandroid.util.CoinFormat
import com.dcrandroid.util.PreferenceUtil
import com.dcrandroid.util.Utils
import com.dcrandroid.util.WalletData
import kotlinx.android.synthetic.main.transaction_row.view.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class TransactionListAdapter(val context: Context, val transactions: ArrayList<Transaction>): RecyclerView.Adapter<TransactionListAdapter.TransactionListViewHolder>() {

    private val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    val walletData: WalletData = WalletData.getInstance()
    val util = PreferenceUtil(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionListViewHolder {
        val view = layoutInflater.inflate(R.layout.transaction_row, parent, false)
        return  TransactionListViewHolder(view)
    }

    override fun getItemCount(): Int {
        return transactions.size
    }

    override fun onBindViewHolder(holder: TransactionListViewHolder, position: Int) {
        val transaction = transactions[position]

        if (transaction.confirmations == 0) {
            holder.status.setPending()
        }else if (transaction.confirmations > 1 || util.getBoolean(Constants.SPEND_UNCONFIRMED_FUNDS)){
            holder.status.setConfirmed(transaction.timestamp)
        }

        if(transaction.animate){
            val blinkAnim = AnimationUtils.loadAnimation(holder.view.context, R.anim.anim_blink)
            holder.view.animation = blinkAnim
            transaction.animate = false
        }

        if (transaction.type == Constants.REGULAR) run {
            val strAmount = Utils.formatDecredWithComma(transaction.amount)

            holder.amount.text = CoinFormat.format(strAmount + Constants.NBSP + layoutInflater.context.getString(R.string.dcr))

            val iconRes = when {
                transaction.direction == 0 -> R.drawable.ic_send
                transaction.direction == 1 -> R.drawable.ic_receive
                else -> R.drawable.ic_tx_transferred
            }
            holder.icon.setImageResource(iconRes)
        }

    }

    class TransactionListViewHolder(val view: View): RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.tx_icon
        val amount: TextView = view.amount
        val status: TextView = view.status
    }

    private fun TextView.setPending(){
        this.text = "Pending"
        this.setTextColor(Color.parseColor("#8997a5"))
        this.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pending, 0, 0, 0)
    }

    private fun TextView.setConfirmed(timestamp: Long){
        this.text = getTimestamp(timestamp * 1000) // convert seconds to milliseconds
        this.setTextColor(Color.parseColor("#596d81"))
        this.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_confirmed, 0, 0, 0)
    }

    fun getTimestamp(timestamp: Long): String{
        val txDate = GregorianCalendar()
        txDate.time = Date(timestamp)

        val today = GregorianCalendar()

        val difference = System.currentTimeMillis() - timestamp
        val yesterday: Long = 86400000

        val week = DateUtils.WEEK_IN_MILLIS
        val month = week * 4

        return when {
            DateUtils.isToday(timestamp) -> "Today"
            yesterday > difference -> "Yesterday"
            week > difference -> SimpleDateFormat("EE", Locale.getDefault()).format(timestamp)
            today.get(Calendar.MONTH) != txDate.get(Calendar.MONTH) && (month > difference) -> SimpleDateFormat("MMMM dd", Locale.getDefault()).format(timestamp)
            else -> SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(timestamp)
        }
    }
}