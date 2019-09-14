/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import kotlinx.android.synthetic.main.popup_layout.view.*
import kotlinx.android.synthetic.main.popup_layout_row.view.*

class PopupItem(@StringRes val title: Int, @ColorRes val color: Int = R.color.darkBlueTextColor)

class PopupMenuAdapter(private val context: Context, private val items: Array<PopupItem>, private val itemClicked:(position: Int) -> Unit): RecyclerView.Adapter<PopupMenuAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
                LayoutInflater
                        .from(context)
                        .inflate(R.layout.popup_layout_row, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.setText(items[position].title)
        holder.textView.setTextColor(context.resources.getColor(items[position].color))

        when (position) {
            0 -> holder.itemView.setBackgroundResource(R.drawable.curved_top_ripple)
            itemCount - 1 -> holder.itemView.setBackgroundResource(R.drawable.curved_bottom_ripple)
            else -> holder.itemView.setBackgroundResource(R.drawable.ripple)
        }

        holder.itemView.setOnClickListener {
            itemClicked(position)
        }
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val textView = itemView.popup_text
    }
}

class PopupUtil {
    companion object {
        fun showPopup(anchorView: View, items: Array<PopupItem>, itemClicked: (window: PopupWindow, position: Int) -> Unit) {
            val context = anchorView.context
            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.popup_layout, null)
            val window = PopupWindow(view, LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT, true)

            val recyclerView = view.popup_rv

            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = PopupMenuAdapter(context, items) { index ->
                itemClicked(window, index)
            }
            window.showAsDropDown(anchorView)
        }
    }
}