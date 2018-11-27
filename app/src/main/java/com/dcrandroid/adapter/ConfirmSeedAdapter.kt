package com.dcrandroid.adapter

import android.content.Context
import android.database.Cursor
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.Selection
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.CursorAdapter
import android.widget.TextView
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
        val currentSeed = seedItems[position]
        var currentText = ""
        val str = "Word #${currentSeed.number + 1}"
        val hintAdapter = SuggestionsTextAdapter(context, R.layout.dropdown_item_1line, allStringSeedArray)
        getHintView(context)

        holder.savedSeed.completionHint = context.getString(R.string.tap_to_select)
        holder.savedSeed.setSingleLine()
        holder.savedSeed.setAdapter(hintAdapter)
        holder.savedSeed.imeOptions = EditorInfo.IME_ACTION_NEXT
        holder.positionOfSeed.text = str

        holder.savedSeed.setOnItemClickListener { parent, _, pos, _ ->
            val s = parent.getItemAtPosition(pos) as String
            currentSeed.phrase = s
            currentText = s
            saveSeed(currentSeed)
            holder.savedSeed.setText(s)
        }
        holder.savedSeed.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val isCorrectSeed = s.toString() == currentText
                Log.d("confirmSeed", "currentText: $currentText")
                Log.d("confirmSeed", "isCorrectSeed: $isCorrectSeed")

                when {
                    s!!.isNotEmpty() -> holder.ivClearText.visibility = View.VISIBLE
                    s.isNullOrEmpty() -> holder.ivClearText.visibility = View.GONE
                }
                when {
                    isCorrectSeed -> Log.d("confirmSeed", "is Correct Seed!")
                    !isCorrectSeed -> Log.d("confirmSeed", "is Incorrect Seed!")
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

    private fun getHintView(context: Context): View? {
        val hintView = LayoutInflater.from(context).inflate(R.layout.completion_hint_view, null).findViewById(android.R.id.text1) as TextView
        hintView.setText(R.string.tap_to_select)
        return hintView
    }


}
