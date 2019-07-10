/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.adapter

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import kotlinx.android.synthetic.main.create_wallet_list_row.view.*
import kotlinx.android.synthetic.main.verify_seed_footer.view.*

private const val VIEW_TYPE_HEADER = 0
private const val VIEW_TYPE_SEED = 1
private const val VIEW_TYPE_FOOTER = 2

class SeedPageFooter(var error: String?, @DrawableRes var buttonBackground: Int)
data class MultiSeed(val firstSeed: InputSeed, val secondSeed: InputSeed, val thirdSeed: InputSeed)

interface SeedTapListener{
    fun onConfirm(enteredSeeds: ArrayList<String>, emptySeed: Boolean)
    fun onSeedEntered(enteredSeeds: ArrayList<String>, emptySeed: Boolean, position: Int)
}

class CreateWalletAdapter(val context: Context, private val allListOfSeeds: ArrayList<MultiSeed>, val listener: SeedTapListener, val footer: SeedPageFooter) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val enteredSeeds = ArrayList<String>()
    private var emptySeed: Boolean = true

    init {
        for(i in 0..32){
            enteredSeeds.add(i, "")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when(viewType){
            VIEW_TYPE_HEADER -> {
                HeaderViewHolder(layoutInflater.inflate(R.layout.verify_seed_header, parent, false))
            }
            VIEW_TYPE_SEED -> {
                SeedViewHolder(layoutInflater.inflate(R.layout.create_wallet_list_row, parent, false))
            }
            else -> {
                FooterViewHolder(layoutInflater.inflate(R.layout.verify_seed_footer, parent, false))
            }
        }
    }

    override fun getItemCount(): Int {
        return allListOfSeeds.size + 2 // including header and footer
    }

    override fun getItemId(position: Int): Long {
        setHasStableIds(true)
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return when (position){
            0 -> VIEW_TYPE_HEADER
            itemCount - 1 -> VIEW_TYPE_FOOTER
            else -> VIEW_TYPE_SEED
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(position){
            0 -> {}
            itemCount - 1 -> {
                val footerHolder = holder as FooterViewHolder

                if(footer.error != null){
                    footerHolder.error.visibility = View.VISIBLE
                    footerHolder.error.text = footer.error
                }else{
                    footerHolder.error.visibility = View.GONE
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    footerHolder.confirmButton.background = ContextCompat.getDrawable(context, footer.buttonBackground)
                }else{
                    footerHolder.confirmButton.setBackgroundDrawable(ContextCompat.getDrawable(context, footer.buttonBackground))
                }

                footerHolder.confirmButton.setOnClickListener {
                    listener.onConfirm(enteredSeeds, emptySeed)
                }
            }
            else -> {
                val seedHolder = holder as SeedViewHolder
                seedHolder.multiSeedPosition.text = String.format("${context.getString(R.string.correctWordIs)}${seedHolder.adapterPosition}")
                val seedIndex = seedHolder.adapterPosition - 1

                val currentMultiSeed = allListOfSeeds[seedIndex]

                seedHolder.firstSeed.text = currentMultiSeed.firstSeed.phrase
                if(enteredSeeds[seedIndex] == currentMultiSeed.firstSeed.phrase){
                    enableClick(seedHolder.firstSeed)
                }else{
                    disableClick(seedHolder.firstSeed)
                }

                seedHolder.secondSeed.text = currentMultiSeed.secondSeed.phrase
                if(enteredSeeds[seedIndex] == currentMultiSeed.secondSeed.phrase){
                    enableClick(seedHolder.secondSeed)
                }else{
                    disableClick(seedHolder.secondSeed)
                }

                seedHolder.thirdSeed.text = currentMultiSeed.thirdSeed.phrase
                if(enteredSeeds[seedIndex] == currentMultiSeed.thirdSeed.phrase){
                    enableClick(seedHolder.thirdSeed)
                }else{
                    disableClick(seedHolder.thirdSeed)
                }

                seedHolder.firstSeed.setOnClickListener {
                    enableClick(seedHolder.firstSeed)
                    disableClick(seedHolder.secondSeed, seedHolder.thirdSeed)
                    saveSeedToArray(currentMultiSeed.firstSeed.phrase, seedIndex)
                }

                seedHolder.secondSeed.setOnClickListener {
                    enableClick(seedHolder.secondSeed)
                    disableClick(seedHolder.firstSeed, seedHolder.thirdSeed)
                    saveSeedToArray(currentMultiSeed.secondSeed.phrase, seedIndex)
                }

                seedHolder.thirdSeed.setOnClickListener {
                    enableClick(seedHolder.thirdSeed)
                    disableClick(seedHolder.secondSeed, seedHolder.firstSeed)
                    saveSeedToArray(currentMultiSeed.thirdSeed.phrase, seedIndex)
                }
            }
        }
    }

    private fun saveSeedToArray(seed: String, position: Int){
        enteredSeeds[position] = seed

        emptySeed = false
        enteredSeeds.forEach {
            if(it.isEmpty()){
                emptySeed = true
            }
        }

        listener.onSeedEntered(enteredSeeds, emptySeed, position)
    }

    private fun enableClick(seed: View) {
        changeBackground(seed, true)
    }

    private fun disableClick(seed: View){
        changeBackground(seed, false)
    }

    private fun disableClick(disableFirst: View, disableSecond: View) {
        changeBackground(disableFirst, false)
        changeBackground(disableSecond, false)
    }

    private fun changeBackground(view: View, isEnableClick: Boolean) {
        if (isEnableClick) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                view.background = ContextCompat.getDrawable(context, R.drawable.btn_shape2)
            } else {
                view.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.btn_shape2))
            }
            view.isClickable = false
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                view.background = ContextCompat.getDrawable(context, R.drawable.btn_shape4)
            } else {
                view.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.btn_shape4))
            }
            view.isClickable = true
        }
    }

    inner class SeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val firstSeed = itemView.tvFirstSeed!!
        val secondSeed = itemView.tvSecondSeed!!
        val thirdSeed = itemView.tvThirdSeed!!
        val multiSeedPosition = itemView.tvCorrectWordNumber!!
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    inner class FooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val error: TextView = itemView.tvError
        val confirmButton: Button = itemView.button_confirm_seed
    }


}