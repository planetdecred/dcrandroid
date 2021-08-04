/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.dialog.txdetails

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.dcrandroid.R
import com.dcrandroid.extensions.toggleVisibility
import com.dcrandroid.util.Utils
import kotlinx.android.synthetic.main.input_output_dropdown.view.*

class InputOutputDropdown(
    private val layout: View,
    items: Array<DropDownItem>,
    toastAnchor: View,
    isInput: Boolean = true
) : View.OnClickListener {

    val context = layout.context

    init {
        layout.rv_input_output.layoutManager = LinearLayoutManager(context)
        val adapter = DropdownAdapter(items)
        layout.rv_input_output.adapter = adapter

        layout.input_output_consumed.text = when {
            isInput -> context.getString(R.string.x_inputs_consumed, items.size)
            else -> context.getString(R.string.x_output_created, items.size)
        }

        layout.input_output_toggle.setOnClickListener(this)

        adapter.addressTapped = { position ->
            val address = items[position].address

            val toastMessage = if (isInput) {
                R.string.previous_outpoint_copied
            } else {
                R.string.address_copy_text
            }

            Utils.copyToClipboard(toastAnchor, address, toastMessage)
        }
    }

    override fun onClick(v: View?) {
        layout.rv_input_output.toggleVisibility()

        val dropDownIcon = if (layout.rv_input_output.visibility == View.VISIBLE) {
            R.drawable.ic_collapse
        } else {
            R.drawable.ic_expand
        }

        layout.input_output_toggle_icon.setImageResource(dropDownIcon)

    }
}