/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.show
import kotlinx.android.synthetic.main.tab_row.view.*

data class NavigationTab(
    @StringRes val title: Int,
    @DrawableRes val activeIcon: Int,
    @DrawableRes val inactiveIcon: Int
)

class NavigationTabsAdapter(
    val context: Context,
    var activeTab: Int,
    var deviceWidth: Int,
    var backupsNeeded: Int,
    var tabSelected: (position: Int) -> Unit
) : RecyclerView.Adapter<NavigationTabsAdapter.NavigationTabViewHolder>() {

    private val layoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private var tabs: ArrayList<NavigationTab> = ArrayList()

    init {
        tabs.add(
            NavigationTab(
                R.string.overview,
                R.drawable.ic_overview,
                R.drawable.ic_overview_inactive
            )
        )
        tabs.add(
            NavigationTab(
                R.string.transactions,
                R.drawable.ic_transactions,
                R.drawable.ic_transactions_inactive
            )
        )
        tabs.add(NavigationTab(R.string.wallets, R.drawable.ic_wallet, R.drawable.ic_wallet02))
        tabs.add(NavigationTab(R.string.more, R.drawable.ic_menu, R.drawable.ic_menu_inactive))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NavigationTabViewHolder {
        val view = layoutInflater.inflate(R.layout.tab_row, parent, false)
        return NavigationTabViewHolder(view)
    }

    override fun getItemCount(): Int {
        return tabs.size
    }

    override fun onBindViewHolder(holder: NavigationTabViewHolder, position: Int) {
        holder.title.text = context.getString(tabs[position].title)

        if (activeTab == position) {
            holder.title.setTextColor(Color.parseColor("#091440"))
            holder.icon.setImageResource(tabs[position].activeIcon)
        } else {
            holder.title.setTextColor(Color.parseColor("#596d81"))
            holder.icon.setImageResource(tabs[position].inactiveIcon)
        }

        if (position == 2 && (backupsNeeded > 0)) { // Wallets Page
            holder.backupIcon.show()
        } else {
            holder.backupIcon.hide()
        }

        holder.itemView.setOnClickListener {
            val oldActiveTab = activeTab
            activeTab = position

            // updating only changed items. calling notifyDataSetChanged
            // does not retain the ripple effect.
            notifyItemChanged(position)
            notifyItemChanged(oldActiveTab)

            tabSelected(position)
        }

        holder.itemView.layoutParams =
            ViewGroup.LayoutParams(deviceWidth / 4, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    fun changeActiveTab(position: Int) {
        if (activeTab == position) {
            return
        }
        activeTab = position
        notifyDataSetChanged()
    }

    inner class NavigationTabViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title = itemView.tab_title
        val icon = itemView.tab_icon
        val backupIcon = itemView.backup_icon
    }
}