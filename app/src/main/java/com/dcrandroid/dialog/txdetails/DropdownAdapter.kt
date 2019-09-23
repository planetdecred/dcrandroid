/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.dialog.txdetails

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import kotlinx.android.synthetic.main.input_output_row.view.*

data class DropDownItem(val amount: String, val address: String)

class DropdownAdapter(private val items: Array<DropDownItem>): RecyclerView.Adapter<DropdownAdapter.ViewHolder>() {

    lateinit var addressTapped:(position: Int) -> Unit

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(inflater.inflate(R.layout.input_output_row, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.amount.text = items[position].amount
        holder.itemView.address.text = items[position].address

        holder.itemView.address.setOnClickListener {
            addressTapped(position)
        }
    }

    inner class ViewHolder(v: View): RecyclerView.ViewHolder(v)
}