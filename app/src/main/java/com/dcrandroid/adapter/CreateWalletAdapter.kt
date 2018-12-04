package com.dcrandroid.adapter

import android.content.Context
import android.os.Build
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.dcrandroid.R
import kotlinx.android.synthetic.main.create_wallet_list_row.view.*


data class MultiSeed(val firstSeed: InputSeed, val secondSeed: InputSeed, val thirdSeed: InputSeed)

class CreateWalletAdapter(val context: Context, private val allListOfSeeds: ArrayList<MultiSeed>, val saveSeed: (InputSeed) -> Unit,
                          val changeSeed: (InputSeed) -> Unit) : RecyclerView.Adapter<CreateWalletAdapter.ViewHolder>() {

    private lateinit var selectedSeed: InputSeed

    private var isCheckedBefore = false


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
        val currentMultiSeed = allListOfSeeds[position]
        holder.firstSeed.text = currentMultiSeed.firstSeed.phrase
        holder.secondSeed.text = currentMultiSeed.secondSeed.phrase
        holder.thirdSeed.text = currentMultiSeed.thirdSeed.phrase
        holder.multiSeedPosition.text = String.format("${context.getString(R.string.correctWordIs)}${holder.adapterPosition + 1}")


        holder.firstSeed.setOnClickListener {
            if (!isCheckedBefore) {
                selectedSeed = InputSeed(position, holder.firstSeed.text.toString())
                transferSeed(selectedSeed)
            } else {
                transferSeed(InputSeed(position, holder.firstSeed.text.toString()))
                selectedSeed = InputSeed(position, holder.firstSeed.text.toString())
            }
            enableClick(holder.firstSeed)
            disableClick(holder.secondSeed, holder.thirdSeed)
            isCheckedBefore = true
        }
        holder.secondSeed.setOnClickListener {
            if (!isCheckedBefore) {
                selectedSeed = InputSeed(position, holder.secondSeed.text.toString())
                transferSeed(selectedSeed)
            } else {
                transferSeed(InputSeed(position, holder.secondSeed.text.toString()))
                selectedSeed = InputSeed(position, holder.secondSeed.text.toString())
            }
            enableClick(holder.secondSeed)
            disableClick(holder.firstSeed, holder.thirdSeed)
            isCheckedBefore = true
        }
        holder.thirdSeed.setOnClickListener {
            if (!isCheckedBefore) {
                selectedSeed = InputSeed(position, holder.thirdSeed.text.toString())
                transferSeed(selectedSeed)
            } else {
                transferSeed(InputSeed(position, holder.thirdSeed.text.toString()))
                selectedSeed = InputSeed(position, holder.thirdSeed.text.toString())
            }
            enableClick(holder.thirdSeed)
            disableClick(holder.secondSeed, holder.firstSeed)
            isCheckedBefore = true
        }

    }

    private fun transferSeed(seed: InputSeed) {
        if (isCheckedBefore) {
            changeSeed(selectedSeed)
            saveSeed(seed)
        } else {
            saveSeed(seed)
        }
    }

    private fun enableClick(seed: View) {
        changeBackground(seed, true)
    }

    private fun disableClick(disableFirst: View, disableSecond: View) {
        changeBackground(disableFirst, false)
        changeBackground(disableSecond, false)
    }

    private fun changeBackground(view: View, isEnableClick: Boolean) {
        if (isEnableClick) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                view.background = ContextCompat.getDrawable(context, R.drawable.btn_shape3)
            } else {
                view.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.btn_shape3))
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

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val firstSeed = itemView.tvFirstSeed!!
        val secondSeed = itemView.tvSecondSeed!!
        val thirdSeed = itemView.tvThirdSeed!!
        val multiSeedPosition = itemView.tvCorrectWordNumber!!
    }


}