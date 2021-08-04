/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ListPopupWindow
import android.widget.PopupWindow
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.show
import kotlinx.android.synthetic.main.popup_layout.view.*
import kotlinx.android.synthetic.main.popup_layout_row.view.*

class PopupItem(
    @StringRes val title: Int,
    @ColorRes val color: Int = R.color.textColor,
    val enabled: Boolean = true,
    val showNotificationDot: Boolean = false
)

class PopupDivider(val widthPixels: Int)

const val VIEW_TYPE_ROW = 0
const val VIEW_TYPE_DIVIDER = 1

class PopupMenuAdapter(
    private val context: Context,
    private val items: Array<Any>,
    private val itemClicked: (position: Int) -> Unit
) : RecyclerView.Adapter<PopupMenuAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = when (viewType) {
            VIEW_TYPE_ROW -> R.layout.popup_layout_row
            else -> R.layout.popup_layout_divider
        }

        return ViewHolder(
            LayoutInflater
                .from(context)
                .inflate(layout, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position] is PopupDivider) {
            VIEW_TYPE_DIVIDER
        } else VIEW_TYPE_ROW
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        if (item is PopupItem) {
            holder.itemView.popup_text.setText(item.title)

            val textColor = if (item.enabled) item.color else R.color.colorDisabled
            holder.itemView.popup_text.setTextColor(context.resources.getColor(textColor))
            holder.itemView.isEnabled = item.enabled

            if (item.showNotificationDot) {
                holder.itemView.new_badge.show()
            } else {
                holder.itemView.new_badge.hide()
            }

            holder.itemView.setOnClickListener {
                itemClicked(position)
            }
        } else if (item is PopupDivider) {
            holder.itemView.visibility = View.GONE
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}

class PopupUtil {
    companion object {
        fun showPopup(
            anchorView: View,
            items: Array<Any>,
            itemClicked: (window: PopupWindow, position: Int) -> Unit
        ) {
            val context = anchorView.context
            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.popup_layout, null)
            val window = PopupWindow(
                view, ListPopupWindow.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, true
            )

            val recyclerView = view.popup_rv

            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = PopupMenuAdapter(context, items) { index ->
                itemClicked(window, index)
            }
            window.showAsDropDown(anchorView)
        }
    }
}