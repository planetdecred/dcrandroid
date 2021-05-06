/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.preference

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.annotation.ArrayRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import kotlinx.android.synthetic.main.activity_debug.view.*
import kotlinx.android.synthetic.main.list_preference_dialog.*
import kotlinx.android.synthetic.main.list_preference_row.view.*

class ListPreference(val context: Context, val key: String, val defaultValue: Int,
                     @ArrayRes val entries: Int, val view: View, val valueChanged: ((newValue: Int) -> Unit)? = null) : Preference(context, key, view), View.OnClickListener {

    init {
        view.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        ListPreferenceDialog(context).show()
    }

    inner class ListPreferenceDialog(context: Context) : Dialog(context) {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setContentView(R.layout.list_preference_dialog)

            dialog_title.text = this@ListPreference.view.pref_title.text

            val adapter = ListPreferenceAdapter(context).apply {
                selectedItem = multiWallet!!.readInt32ConfigValueForKey(key, defaultValue)
            }

            list_preference_rv.layoutManager = LinearLayoutManager(context)
            list_preference_rv.adapter = adapter

            btn_negative.setOnClickListener { dismiss() }
            btn_positive.setOnClickListener {
                multiWallet!!.setInt32ConfigValueForKey(key, adapter.selectedItem)
                valueChanged?.invoke(adapter.selectedItem)
                dismiss()
            }
        }
    }

    inner class ListPreferenceAdapter(private val context: Context) : RecyclerView.Adapter<ListPreferenceAdapter.ViewHolder>() {

        var selectedItem = 0

        private val items = context.resources.getStringArray(entries)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.list_preference_row, parent, false)

            return ViewHolder(view)
        }

        override fun getItemCount(): Int {
            return items.size
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {

            val imgResource = when (selectedItem) {
                position -> R.drawable.ic_radial_checked
                else -> R.drawable.ic_radial
            }

            holder.itemView.list_preference_radio.setImageResource(imgResource)
            holder.itemView.list_preference_label.text = items[position]

            holder.itemView.setOnClickListener {
                if (position == selectedItem) {
                    return@setOnClickListener
                }

                val oldSelectedItem = selectedItem
                selectedItem = position

                notifyItemChanged(oldSelectedItem)
                notifyItemChanged(selectedItem)
            }
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    }

}