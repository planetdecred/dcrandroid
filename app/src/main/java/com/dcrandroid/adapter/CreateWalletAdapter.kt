package com.dcrandroid.adapter

import android.content.Context
import android.os.Build
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.dcrandroid.R
import kotlinx.android.synthetic.main.create_wallet_list_row.view.*


data class MultiSeed(val firstSeed: InputSeed, val secondSeed: InputSeed, val thirdSeed: InputSeed)

class CreateWalletAdapter(val context: Context, private val allListOfSeeds: ArrayList<MultiSeed>) : RecyclerView.Adapter<CreateWalletAdapter.ViewHolder>() {

    private var isClicked = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.create_wallet_list_row, parent, false))
    }

    override fun getItemCount(): Int {
        return allListOfSeeds.size
    }

    override fun getItemId(position: Int): Long {
        setHasStableIds(true)
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onBindViewHolder(holder: CreateWalletAdapter.ViewHolder, position: Int) {
        Log.d("confirmSeed", "allListOfSeeds: ${allListOfSeeds.size}")
        val currentMultiSeed = allListOfSeeds[position]
        holder.firstSeed.text = currentMultiSeed.firstSeed.phrase
        holder.secondSeed.text = currentMultiSeed.secondSeed.phrase
        holder.thirdSeed.text = currentMultiSeed.thirdSeed.phrase
        holder.multiSeedPosition.text = String.format("${context.getString(R.string.correctWordIs)}${holder.adapterPosition + 1}")


        holder.firstSeed.setOnClickListener {
            enableClick(holder.firstSeed)
            disableClick(holder.secondSeed, holder.thirdSeed)
        }
        holder.secondSeed.setOnClickListener {
            enableClick(holder.secondSeed)
            disableClick(holder.firstSeed, holder.thirdSeed)
        }
        holder.thirdSeed.setOnClickListener {
            enableClick(holder.thirdSeed)
            disableClick(holder.secondSeed, holder.firstSeed)
        }

    }

    private fun enableClick(seed: TextView) {
        changeBackground(seed, true)

    }

    private fun disableClick(disableFirst: TextView, disableSecond: TextView) {
        changeBackground(disableFirst, false)
        changeBackground(disableSecond, false)
    }

    private fun changeBackground(view: TextView, isEnableClick: Boolean) {
        if (isEnableClick) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                view.background = ContextCompat.getDrawable(context, R.drawable.btn_shape3)
            } else {
                view.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.btn_shape3))
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                view.background = ContextCompat.getDrawable(context, R.drawable.btn_shape4)
            } else {
                view.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.btn_shape4))
            }
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val firstSeed = itemView.tvFirstSeed!!
        val secondSeed = itemView.tvSecondSeed!!
        val thirdSeed = itemView.tvThirdSeed!!
        val multiSeedPosition = itemView.tvCorrectWordNumber!!

    }


}