/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R

class SaveSeedAdapter(private val items: List<SeedRow>): RecyclerView.Adapter<SaveSeedAdapter.MyViewHolder>() {

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val seedRow = items[position]
        holder.text1.text = seedRow.seed1
        holder.text2.text = seedRow.seed2
        holder.text3.text = seedRow.seed3
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.save_seed_row, parent, false)

        return MyViewHolder(itemView)
    }

    inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var text1: TextView = view.findViewById(R.id.text1)
        var text2: TextView = view.findViewById(R.id.text2)
        var text3: TextView = view.findViewById(R.id.text3)
    }

     class SeedRow(val seed1: String, val seed2: String, val seed3: String)
}