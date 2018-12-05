package com.dcrandroid.adapter

import android.content.Context
import android.database.Cursor
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.CursorAdapter
import android.widget.TextView
import com.dcrandroid.R
import kotlinx.android.synthetic.main.recover_wallet_list_row.view.*
import java.util.*


data class InputSeed(val number: Int, var phrase: String)

class RestoreWalletAdapter(private val seedItems: List<InputSeed>, private val allStringSeedArray: ArrayList<String>,
                           val context: Context, val saveSeed: (InputSeed) -> Unit, val removeSeed: (InputSeed) -> Unit) : RecyclerView.Adapter<RestoreWalletAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
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

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentSeed = seedItems[position]
        var currentText = ""
        val str = "Word #${currentSeed.number + 1}"
        val hintAdapter = SuggestionsTextAdapter(context, R.layout.dropdown_item_1line, allStringSeedArray)
        getHintView(context)
        holder.positionOfSeed.text = str
        holder.savedSeed.completionHint = context.getString(R.string.tap_to_select)
        holder.savedSeed.setSingleLine()
        holder.savedSeed.setAdapter(hintAdapter)
        holder.savedSeed.imeOptions = EditorInfo.IME_ACTION_NEXT

        holder.savedSeed.onItemClickListener = AdapterView.OnItemClickListener { _, _, _, _ ->
            val enteredItem = holder.savedSeed.text.toString()
            currentSeed.phrase = enteredItem
            currentText = enteredItem
            saveSeed(currentSeed)
            holder.savedSeed.setText(enteredItem)
        }


        holder.savedSeed.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val isCorrectSeed = s.toString() == currentText
                val view = holder.savedSeed.focusSearch(View.FOCUS_DOWN)
                holder.savedSeed.setTextColor(ContextCompat.getColor(context, R.color.darkBlueTextColor))

                when {
                    s!!.isNotEmpty() -> holder.ivClearText.visibility = View.VISIBLE
                    s.isNullOrEmpty() -> holder.ivClearText.visibility = View.GONE
                }

                if (isCorrectSeed && holder.adapterPosition < 32 && s.toString().isNotEmpty()) {
                    holder.savedSeed.setSelection(currentText.length)
                    view.requestFocus()
                } else {
                    holder.savedSeed.setSelection(s.toString().length)
                    removeSeed(seedItems[holder.adapterPosition])
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
                if (!allStringSeedArray.contains(holder.savedSeed.text.toString())) {
                    holder.savedSeed.setTextColor(ContextCompat.getColor(context, R.color.orangeTextColor))
                } else {
                    holder.savedSeed.setTextColor(ContextCompat.getColor(context, R.color.darkBlueTextColor))
                }
                holder.ivClearText.visibility = View.GONE
            } else if (isFocused && holder.savedSeed.text.isNotEmpty()) {
                holder.ivClearText.visibility = View.VISIBLE
            }
        }

    }


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val positionOfSeed = view.tvPositionOfSeed!!
        var savedSeed = view.tvSavedSeed!!
        val ivClearText = view.ivClearText!!
    }

    private fun getHintView(context: Context): View? {
        val hintView = LayoutInflater.from(context).inflate(R.layout.completion_hint_view, null).findViewById(android.R.id.text1) as TextView
        hintView.setText(R.string.tap_to_select)
        return hintView
    }

}
