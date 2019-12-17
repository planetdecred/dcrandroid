/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.adapter

import android.app.Activity
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import kotlinx.android.synthetic.main.recover_wallet_list_row.view.*

const val SEED_COUNT = 33

class RestoreWalletAdapter(val context: Activity, val allSeedWords: ArrayList<String>) : RecyclerView.Adapter<RestoreWalletAdapter.ViewHolder>() {

    private var suggestionsAdapter =
            SuggestionsTextAdapter(context, R.layout.dropdown_item_1, allSeedWords)

    val enteredSeeds = ArrayList<String>().apply {
        for (i in 0 until SEED_COUNT) {
            this.add("")
        }
    }
    private var allValid = false

    var nextFocusPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
                .inflate(R.layout.recover_wallet_list_row, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return SEED_COUNT
    }

    override fun getItemId(position: Int): Long {
        setHasStableIds(true)
        return position.toLong()
    }

    var seedChanged: ((Int, Boolean) -> Unit?)? = null

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.seed_index.text = (position + 1).toString()

        holder.itemView.seed_et.apply {
            setDropDownBackgroundResource(android.R.color.transparent)
            dropDownVerticalOffset = context.resources.getDimensionPixelOffset(R.dimen.margin_padding_size_14)
            dropDownWidth = ViewGroup.LayoutParams.MATCH_PARENT
            setAdapter(suggestionsAdapter)
        }

        if (nextFocusPosition == position) {
            nextFocusPosition = -1
            holder.itemView.seed_et.requestFocus()
            holder.itemView.seed_et.isCursorVisible = true
        }

        holder.itemView.seed_et.setOnFocusChangeListener { v, hasFocus ->

            var backgroundResource: Int
            var indexBackground: Int
            var indexTextColor: Int
            var editTextColor: Int = R.color.darkBlueTextColor

            if (hasFocus) {
                backgroundResource = R.drawable.input_background_active
                indexBackground = R.drawable.seed_index_bg_active
                indexTextColor = R.color.blue
            } else {
                backgroundResource = R.drawable.input_background
                indexBackground = R.drawable.seed_index_bg
                indexTextColor = R.color.darkerBlueGrayTextColor
            }

            if (!hasFocus) {
                val seed = holder.itemView.seed_et.text.toString()

                if (allSeedWords.indexOf(seed) < 0) {
                    backgroundResource = R.drawable.input_background_error
                    indexBackground = R.drawable.seed_index_bg_error
                    indexTextColor = R.color.colorError
                    editTextColor = R.color.colorError
                }
            }

            holder.itemView.setBackgroundResource(backgroundResource)

            holder.itemView.seed_index.setBackgroundResource(indexBackground)
            holder.itemView.seed_index.setTextColor(context.getColor(indexTextColor))

            holder.itemView.seed_et.setTextColor(context.getColor(editTextColor))
        }

        holder.itemView.seed_et.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {

                val seed = s.toString()
                enteredSeeds[position] = seed

                allValid = true
                enteredSeeds.forEach {
                    if (allSeedWords.indexOf(it) < 0) {
                        allValid = false
                    }
                }

                seedChanged?.invoke(position, allValid)
            }

        })

        holder.itemView.seed_et.setOnItemClickListener { _, _, _, _ ->
            if (position + 1 < itemCount) {
                nextFocusPosition = position + 1
                notifyItemChanged(nextFocusPosition)
            }
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

}
