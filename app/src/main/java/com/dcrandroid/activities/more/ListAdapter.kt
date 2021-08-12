/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities.more

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import com.dcrandroid.extensions.show
import kotlinx.android.synthetic.main.more_list_row_2.view.*

class ListAdapter(val context: Context, val items: Array<ListItem>) :
    RecyclerView.Adapter<ListAdapter.ViewHolder>() {

    var itemTapped: ((position: Int) -> Unit?)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.more_list_row_2, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = items[position]

        holder.itemView.tv_title.setText(item.title)

        if (item.subtitle != null) {
            holder.itemView.tv_subtitle.apply {
                text = item.subtitle
                show()
            }
        }

        holder.itemView.setOnClickListener {
            itemTapped?.invoke(position)
        }

        val background = when {
            itemCount == 1 -> R.drawable.ripple_bg_surface_corners_8dp
            position == 0 -> R.drawable.curved_top_ripple
            position == itemCount - 1 -> R.drawable.curved_bottom_ripple
            else -> R.drawable.surface_ripple
        }

        holder.itemView.setBackgroundResource(background)
    }

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v)
}

data class ListItem(val title: Int, val subtitle: String? = null)