package com.dcrandroid.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import com.dcrandroid.R
import kotlinx.android.synthetic.main.saved_seeds_list_row.view.*

data class InputSeed(val number: Int, var phrase: String)

class ConfirmSeedAdapter(private val seedItems: List<InputSeed>, private val allStringSeedArray: ArrayList<String>,
                         val context: Context, val saveSeed: (InputSeed) -> Unit, val removeSeed: (InputSeed) -> Unit) : RecyclerView.Adapter<ConfirmSeedAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.saved_seeds_list_row, parent, false)
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

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val str = "Word #${seedItems[position].number + 1}"
        val hintAdapter = ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, allStringSeedArray)

        holder.savedSeed.completionHint = context.getString(R.string.tap_to_select)
        holder.savedSeed.setSingleLine()
        holder.savedSeed.setAdapter(hintAdapter)
        holder.savedSeed.imeOptions = EditorInfo.IME_ACTION_NEXT
        holder.positionOfSeed.text = str

        holder.savedSeed.setOnItemClickListener { parent, _, pos, _ ->
            val s = parent.getItemAtPosition(pos) as String
            holder.savedSeed.setText(s)
            holder.savedSeed.setSelection(holder.savedSeed.text.length)
            seedItems[holder.adapterPosition].phrase = s
            saveSeed(seedItems[holder.adapterPosition])
        }
        holder.savedSeed.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s!!.isNotEmpty()) {
                    holder.ivClearText.visibility = View.VISIBLE
                } else if (s.isNullOrEmpty()) {
                    holder.ivClearText.visibility = View.GONE
                }

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })
        holder.ivClearText.setOnClickListener {
            holder.savedSeed.text.clear()
            removeSeed(seedItems[holder.adapterPosition])
        }

        holder.savedSeed.setOnFocusChangeListener { _, isFocused ->
            if (!isFocused) {
                holder.ivClearText.visibility = View.GONE
            } else if (isFocused && holder.savedSeed.text.isNotEmpty()) {
                holder.ivClearText.visibility = View.VISIBLE
            }
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val positionOfSeed = view.tvPositionOfSeed!!
        val savedSeed = view.tvSavedSeed!!
        val ivClearText = view.ivClearText!!
    }


}
