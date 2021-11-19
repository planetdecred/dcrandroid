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
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.LayoutRes
import com.dcrandroid.R

class SuggestionsTextAdapter(
    context: Context,
    @LayoutRes private val layoutResource: Int,
    private val suggestions: List<String>
) :
    ArrayAdapter<String>(context, layoutResource, suggestions) {

    private val filteredArray = ArrayList<String>()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createViewFromResource(position, convertView, parent)
    }

    private fun createViewFromResource(
        position: Int,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        var view = convertView

        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.dropdown_item_1, parent, false)
        }

        val suggestionTextView = view!!.findViewById<TextView>(android.R.id.text1)
        suggestionTextView.text = filteredArray[position]

        val layoutParams = suggestionTextView.layoutParams as LinearLayout.LayoutParams
        if (position == count - 1) {
            layoutParams.bottomMargin =
                context.resources.getDimensionPixelOffset(R.dimen.margin_padding_size_4)
        } else {
            layoutParams.bottomMargin = 0
        }
        suggestionTextView.layoutParams = layoutParams

        val backgroundResource = when (position) {
            0 -> R.drawable.curved_top_4dp_ripple
            count - 1 -> R.drawable.curved_bottom_4dp_ripple
            else -> R.drawable.surface_bg_ripple
        }

        suggestionTextView.setBackgroundResource(backgroundResource)

        return view
    }

    override fun getCount(): Int {
        return if (filteredArray.size >= 4) {
            4
        } else {
            filteredArray.size
        }
    }

    override fun getFilter(): Filter {
        return StringFilter(this, suggestions)
    }

    override fun getItem(position: Int): String? {
        return filteredArray[position]
    }

    private inner class StringFilter(
        val textAdapter: SuggestionsTextAdapter,
        val suggestionList: List<String>
    ) : Filter() {

        var filteredList = ArrayList<String>()

        override fun performFiltering(constraint: CharSequence?): FilterResults {
            filteredList.clear()
            val filterResults = FilterResults()
            if (constraint.isNullOrEmpty()) {
                filteredList.addAll(suggestionList)
            } else {
                val filterPattern = constraint.toString()
                for (item in suggestionList) {
                    if (item.startsWith(filterPattern, true)) filteredList.add(item)
                }
            }
            filterResults.values = filteredList
            filterResults.count = filteredList.size

            return filterResults
        }

        override fun publishResults(constraint: CharSequence?, filterResults: FilterResults) {
            textAdapter.filteredArray.clear()
            textAdapter.filteredArray.addAll(filterResults.values as List<String>)
            textAdapter.notifyDataSetChanged()
        }

    }

}