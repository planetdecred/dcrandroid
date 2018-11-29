package com.dcrandroid.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dcrandroid.R
import kotlinx.android.synthetic.main.create_wallet_list_row.view.*


class CreateWalletAdapter(val context: Context, private val listOfSeeds: ArrayList<ArrayList<InputSeed>>) : RecyclerView.Adapter<CreateWalletAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CreateWalletAdapter.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.create_wallet_list_row, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return listOfSeeds.size
    }

    override fun getItemId(position: Int): Long {
        setHasStableIds(true)
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onBindViewHolder(holder: CreateWalletAdapter.ViewHolder, position: Int) {
        Log.d("confirmSeed", "onBindViewHolder - listOfSeeds: $listOfSeeds")
        val currentSeedList = (listOfSeeds[holder.adapterPosition])
        Log.d("confirmSeed", "onBindViewHolder - correctSeedPosition: ${holder.adapterPosition}")

        holder.tvFirstSeed.text = currentSeedList[0].phrase
        holder.tvSecondSeed.text = currentSeedList[1].phrase
        holder.tvThirdSeed.text = currentSeedList[2].phrase
        holder.tvCurrentWordNumber.text = String.format(context.getString(R.string.correctWordIs) + (holder.adapterPosition + 1))
    }


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFirstSeed = view.tvFirstSeed!!
        val tvSecondSeed = view.tvSecondSeed!!
        val tvThirdSeed = view.tvThirdSeed!!
        val tvCurrentWordNumber = view.tvCorrectWordNumber!!
    }
}