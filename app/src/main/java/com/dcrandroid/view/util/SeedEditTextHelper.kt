/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.view.util

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import com.dcrandroid.R
import com.dcrandroid.adapter.SuggestionsTextAdapter
import com.dcrandroid.view.SeedEditTextLayout
import kotlinx.android.synthetic.main.restore_wallet_list_row.view.*

class SeedEditTextHelper(val layout: SeedEditTextLayout, adapter: SuggestionsTextAdapter, private val itemPosition: Int) : View.OnFocusChangeListener, TextWatcher, AdapterView.OnItemClickListener {

    private val context = layout.context

    private val editText = layout.seed_et
    private val editTextBackground = layout.list_layout
    private val indexTv = layout.seed_index

    lateinit var seedChanged: () -> Unit?
    lateinit var validateSeed: (seedWord: String) -> Boolean
    lateinit var moveToNextRow: (currentItem: Int) -> Unit?

    init {
        indexTv.text = (itemPosition.plus(1)).toString()

        editText.apply {
            setDropDownBackgroundResource(android.R.color.transparent)
            dropDownVerticalOffset = context.resources.getDimensionPixelOffset(R.dimen.margin_padding_size_14)
            dropDownWidth = ViewGroup.LayoutParams.MATCH_PARENT
        }

        editText.setAdapter(adapter)
        editText.onFocusChangeListener = this
        editText.addTextChangedListener(this)
        editText.onItemClickListener = this
    }

    fun requestFocus(): Int {
        editText.requestFocus()
        if (editText.text.isNotEmpty()) {
            editText.setSelection(0, editText.text.length)
            editText.isCursorVisible = true
        }

        return layout.bottom - context.resources.getDimensionPixelOffset(R.dimen.margin_padding_size_24)
    }

    fun getSeed() = editText.text.toString().trim()

    override fun onFocusChange(v: View?, hasFocus: Boolean) {
        var backgroundResource = R.drawable.input_background
        var indexBackground = R.drawable.seed_index_bg
        var indexTextColor = R.color.darkerBlueGrayTextColor
        var editTextColor = R.color.darkBlueTextColor

        if (hasFocus) {
            backgroundResource = R.drawable.input_background_active
            indexBackground = R.drawable.seed_index_bg_active
            indexTextColor = R.color.blue
        } else {
            val seed = editText.text.toString()

            if (!validateSeed(seed)) {
                backgroundResource = R.drawable.input_background_error
                indexBackground = R.drawable.seed_index_bg_error
                indexTextColor = R.color.colorError
                editTextColor = R.color.colorError
            }
        }

        editTextBackground.setBackgroundResource(backgroundResource)

        indexTv.setBackgroundResource(indexBackground)
        indexTv.setTextColor(context.getColor(indexTextColor))

        editText.setTextColor(context.getColor(editTextColor))
    }

    override fun afterTextChanged(s: Editable?) {
        val seed = s.toString()
        seedChanged.invoke()
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        moveToNextRow(itemPosition)
    }
}