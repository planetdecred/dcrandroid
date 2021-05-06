/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.util

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import com.dcrandroid.R

class PopupMessage {
    companion object {
        fun showText(anchorView: View, @StringRes text: Int, length: Int = Toast.LENGTH_SHORT): Toast {
            val inflater = LayoutInflater.from(anchorView.context)
            val view = inflater.inflate(R.layout.popup_message, null)

            val textView = view.findViewById<TextView>(R.id.popup_text)
            textView.setText(text)

            val position = IntArray(2)
            anchorView.getLocationInWindow(position)

            val topMargin = anchorView.context.resources.getDimensionPixelSize(R.dimen.margin_padding_size_12)

            val t = Toast(anchorView.context)
            t.view = view
            t.duration = length
            t.setGravity(Gravity.TOP or Gravity.END, 0, position[1] + topMargin)

            return t
        }
    }
}