/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import kotlinx.android.synthetic.main.seed_list_row.view.*

class SaveSeedAdapter(private val items: Array<String>) :
    RecyclerView.Adapter<SaveSeedAdapter.MyViewHolder>() {

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.index.text = (position + 1).toString()
        holder.seedText.text = items[position]
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.seed_list_row, parent, false)

        return MyViewHolder(itemView)
    }

    inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val index = view.seed_index
        val seedText = view.seed
    }
}