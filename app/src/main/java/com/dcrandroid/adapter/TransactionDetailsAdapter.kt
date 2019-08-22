package com.dcrandroid.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.dcrandroid.R

class TransactionDetailsAdapter constructor(val context: Context, private val items: MutableList<TransactionDebitCredit>) : BaseAdapter() {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var vi = convertView
        val item = items[position]

        if (vi == null) {
            vi = if (item.type == TransactionDebitCredit.ItemType.ITEM) {
                LayoutInflater.from(context).inflate(R.layout.list_item_adapter, parent, false)
            } else {
                LayoutInflater.from(context).inflate(R.layout.tx_details_list_header, parent, false)
            }
        }

        if (item.type == TransactionDebitCredit.ItemType.ITEM) {
            vi!!.findViewById<TextView>(R.id.tv_visible_wallet_balance).text = item.title
            vi.findViewById<TextView>(R.id.tv_info).text = item.info
        } else {
            vi!!.findViewById<TextView>(R.id.text).text = item.title
        }

        return vi
    }

    override fun getItem(position: Int): TransactionDebitCredit {
        return items[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return items.size
    }

    override fun getViewTypeCount(): Int {
        return 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position].type == TransactionDebitCredit.ItemType.HEADER) 0 else 1
    }

    class TransactionDebitCredit(val title: String?, val info: String?, val type: ItemType?, val direction: Direction?) {

        constructor(headerTitle: String, type: ItemType) : this(headerTitle, null, type, null) {}

        enum class Direction {
            CREDIT,
            DEBIT
        }

        enum class ItemType {
            HEADER,
            ITEM
        }
    }
}