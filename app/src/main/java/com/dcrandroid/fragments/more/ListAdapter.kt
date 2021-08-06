/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.fragments.more

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import kotlinx.android.synthetic.main.more_list_row.view.*

class ListAdapter(val context: Context, val items: Array<ListItem>) :
    RecyclerView.Adapter<ListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.more_list_row, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.iv_icon.setImageResource(items[position].iconResource)
        holder.itemView.tv_title.setText(items[position].title)

        holder.itemView.setOnClickListener {
            val intent = items[position].intent
            if (intent != null) {
                context.startActivity(intent)
            }
        }
    }

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v)
}

data class ListItem(val title: Int, @DrawableRes val iconResource: Int, var intent: Intent? = null)