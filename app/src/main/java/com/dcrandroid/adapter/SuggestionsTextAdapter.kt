package com.dcrandroid.adapter

import android.content.Context
import android.support.annotation.LayoutRes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import kotlinx.android.synthetic.main.dropdown_item_1line.view.*

class SuggestionsTextAdapter(context: Context, @LayoutRes private val layoutResource: Int, private val suggestions: ArrayList<String>) :
        ArrayAdapter<String>(context, layoutResource, suggestions) {

    private val filteredArray = ArrayList<String>()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createViewFromResource(position, convertView, parent)
    }

    private fun createViewFromResource(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView
        if (view == null) {
            view = LayoutInflater.from(context).inflate(layoutResource, parent, false)
        }
        val suggestionTextView = view!!.suggestionText
        suggestionTextView.text = filteredArray[position]

        return view
    }

    override fun getCount(): Int {
        return filteredArray.size
    }

    override fun getFilter(): Filter {
        return StringFilter(this, suggestions)
    }

    override fun getItem(position: Int): String? {
        return filteredArray[position]
    }

    private inner class StringFilter(val textAdapter: SuggestionsTextAdapter, val suggestionList: ArrayList<String>) : Filter() {

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
            textAdapter.filteredArray.addAll(filterResults.values as Collection<String>)
            textAdapter.notifyDataSetChanged()
        }

    }

}