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
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import com.dcrandroid.activities.SEED_COUNT
import kotlinx.android.synthetic.main.verify_seed_list_row.view.*

data class InputSeed(val number: Int, var phrase: String)
data class ShuffledSeeds(val seeds: Array<InputSeed>, var selectedIndex: Int = -1)

class VerifySeedAdapter(
    val context: Context, private val seeds: ArrayList<ShuffledSeeds>,
    private val seedTapped: (seedIndex: Int) -> Unit
) : RecyclerView.Adapter<VerifySeedAdapter.SeedViewHolder>() {

    val enteredSeeds = Array(SEED_COUNT) { "" }
    var allSeedsSelected = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeedViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        if (viewType == 0) {
            return SeedViewHolder(
                layoutInflater.inflate(
                    R.layout.verify_seed_header,
                    parent,
                    false
                )
            )
        }
        return SeedViewHolder(layoutInflater.inflate(R.layout.verify_seed_list_row, parent, false))
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) {
            0
        } else 1
    }

    override fun getItemCount(): Int {
        return seeds.size + 1
    }

    override fun getItemId(position: Int): Long {
        setHasStableIds(true)
        return position.toLong()
    }

    override fun onBindViewHolder(holder: SeedViewHolder, position: Int) {
        if (position == 0) {
            return
        }

        val seedIndex = holder.adapterPosition - 1
        holder.itemView.seed_index.text = position.toString()

        val multiSeed = seeds[seedIndex]

        for (i in 0..2) {
            holder.seedText[i].apply {
                text = multiSeed.seeds[i].phrase
                setTextColor(context.resources.getColor(R.color.text3))
                setBackgroundResource(R.drawable.verify_seed_normal)

                setOnClickListener {
                    multiSeed.selectedIndex = i
                    notifyItemChanged(position)

                    saveSeedToArray(multiSeed.seeds[i].phrase, seedIndex)
                }
            }
        }

        if (multiSeed.selectedIndex != -1) {
            holder.seedText[multiSeed.selectedIndex].apply {
                setTextColor(context.resources.getColor(R.color.primary))
                setBackgroundResource(R.drawable.verify_seed_selected)
            }

            holder.itemView.selected_seed.apply {
                text = multiSeed.seeds[multiSeed.selectedIndex].phrase
                setTextColor(context.resources.getColor(R.color.text2))
            }
            holder.itemView.selected_seed.text = multiSeed.seeds[multiSeed.selectedIndex].phrase
        } else {
            holder.itemView.selected_seed.apply {
                text = "—"
                setTextColor(context.resources.getColor(R.color.text3))
            }
        }

    }

    private fun saveSeedToArray(seed: String, position: Int) {
        enteredSeeds[position] = seed

        allSeedsSelected = true
        enteredSeeds.forEach {
            if (it.isEmpty()) {
                allSeedsSelected = false
                return@forEach
            }
        }

        seedTapped(position)
    }

    inner class SeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val seedText = arrayOf(itemView.seed1, itemView.seed2, itemView.seed3)
    }

}