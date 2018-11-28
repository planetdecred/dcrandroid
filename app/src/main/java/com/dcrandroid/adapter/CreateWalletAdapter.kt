package com.dcrandroid.adapter

import android.app.Activity
import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import com.dcrandroid.R
import kotlinx.android.synthetic.main.recover_wallet_list_row.view.*


class CreateWalletAdapter(private val seedItems: List<InputSeed>, val context: Context,
                          var currentSeed: (InputSeed) -> Unit, val reseivedSeed: (InputSeed) -> Unit,
                          var isRemoveItem: Boolean) : RecyclerView.Adapter<CreateWalletAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CreateWalletAdapter.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.recover_wallet_list_row, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return seedItems.size
    }

    override fun getItemId(position: Int): Long {
        setHasStableIds(true)
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onBindViewHolder(holder: CreateWalletAdapter.ViewHolder, position: Int) {
        holder.savedSeed.isCursorVisible = false
        holder.savedSeed.isFocusable = false
        val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(holder.savedSeed.windowToken, 0)

        val currentSeed = seedItems[position]
        val str = "Word #${currentSeed.number + 1}"

        holder.positionOfSeed.text = str
        if (isRemoveItem) {
            holder.savedSeed.text.clear()
        }
    }


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val positionOfSeed = view.tvPositionOfSeed!!
        val savedSeed = view.tvSavedSeed!!
    }
}