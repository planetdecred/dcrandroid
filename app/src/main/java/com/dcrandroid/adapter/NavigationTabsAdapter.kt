/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import kotlinx.android.synthetic.main.tab_row.view.*

data class NavigationTab(@StringRes val title: Int, @DrawableRes val activeIcon: Int, @DrawableRes val inactiveIcon: Int)

interface OnTabSelectedListener{
    fun onTabSelected(position: Int)
}

class NavigationTabsAdapter(val context: Context, var activeTab: Int, var deviceWidth: Int): RecyclerView.Adapter<NavigationTabsAdapter.NavigationTabViewHolder>() {

    var onTabSelectedListener: OnTabSelectedListener? = null
    private val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private var tabs: ArrayList<NavigationTab> = ArrayList()

    init {
        tabs.add(NavigationTab(R.string.overview, R.drawable.ic_overview, R.drawable.ic_overview_inactive))
        tabs.add(NavigationTab(R.string.transactions, R.drawable.ic_transactions, R.drawable.ic_transactions_inactive))
        tabs.add(NavigationTab(R.string.accounts, R.drawable.ic_accounts, R.drawable.ic_accounts_inactive))
        tabs.add(NavigationTab(R.string.settings, R.drawable.ic_settings, R.drawable.ic_settings_inactive))
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

        if(activeTab == position){
            holder.title.setTextColor(Color.parseColor("#091440"))
            holder.icon.setImageResource(tabs[position].activeIcon)
        }else{
            holder.title.setTextColor(Color.parseColor("#596d81"))
            holder.icon.setImageResource(tabs[position].inactiveIcon)
        }

        holder.itemView.setOnClickListener {
            val oldActiveTab = activeTab
            activeTab = position

            // updating only changed items. calling notifyDataSetChanged
            // does not retain the ripple effect.
            notifyItemChanged(position)
            notifyItemChanged(oldActiveTab)

            onTabSelectedListener?.onTabSelected(position)
        }

        holder.itemView.layoutParams = ViewGroup.LayoutParams(deviceWidth / 4, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    inner class NavigationTabViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.tab_title
        val icon: ImageView = itemView.tab_icon
    }
}