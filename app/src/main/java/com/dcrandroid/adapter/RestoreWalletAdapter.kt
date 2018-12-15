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
                           val context: Context, val saveSeed: (InputSeed) -> Unit, val removeSeed: (InputSeed) -> Unit,
                           var isAllSeedsEntered: (Boolean) -> Unit) : RecyclerView.Adapter<RestoreWalletAdapter.ViewHolder>() {

    private var seedsCounter = 0

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
        val currentSeed = seedItems[holder.adapterPosition]
        val str = "Word #${currentSeed.number + 1}"
        var enteredSeed = ""
        val hintAdapter = SuggestionsTextAdapter(context, R.layout.dropdown_item_1line, allStringSeedArray)
        getHintView(context)
        holder.positionOfSeed.text = str
        holder.savedSeed.completionHint = context.getString(R.string.tap_to_select)
        holder.savedSeed.setSingleLine()
        holder.savedSeed.setAdapter(hintAdapter)
        holder.savedSeed.imeOptions = EditorInfo.IME_ACTION_NEXT


        holder.savedSeed.onItemClickListener = AdapterView.OnItemClickListener { _, _, _, _ ->
            val view = holder.savedSeed.focusSearch(View.FOCUS_DOWN)
            enteredSeed = holder.savedSeed.text.toString()
            currentSeed.phrase = enteredSeed
            saveSeed(currentSeed)
            seedsCounter++
            if (seedsCounter >= 33) {
                isAllSeedsEntered(true)
            } else if(holder.adapterPosition < 32){
                view.requestFocus()
            }
        }


        holder.savedSeed.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                holder.savedSeed.setTextColor(ContextCompat.getColor(context, R.color.darkBlueTextColor))
                val view = holder.savedSeed.focusSearch(View.FOCUS_DOWN)

                when {
                    s!!.isNotEmpty() ->  holder.ivClearText.setImageResource(R.drawable.ic_clear)
                    s.isNullOrEmpty() -> holder.ivClearText.setImageResource(0)
                }
                if(enteredSeed.isNotEmpty() && enteredSeed == s.toString() && holder.savedSeed.isFocused && position < 32) {
                    view.requestFocus()
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
            if (seedsCounter > 0) {
                seedsCounter--
            }
            isAllSeedsEntered(false)
        }

        holder.savedSeed.setOnFocusChangeListener { _, isFocused ->
            if (!isFocused) {
                if (!allStringSeedArray.contains(holder.savedSeed.text.toString())) {
                    holder.savedSeed.setTextColor(ContextCompat.getColor(context, R.color.orangeTextColor))
                } else {
                    holder.savedSeed.setTextColor(ContextCompat.getColor(context, R.color.darkBlueTextColor))
                    holder.ivClearText.setImageResource(0)
                }
            } else if (isFocused && holder.savedSeed.text.isNotEmpty()) {
                holder.ivClearText.setImageResource(R.drawable.ic_clear)
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
