/*
 * Copyright (c) 2018-2019 The Decred developers
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

class SnackBar {

    companion object{
        fun make(anchorView: View, @StringRes text: Int){
            val context = anchorView.context
            val inflater = LayoutInflater.from(context)

            val view = inflater.inflate(R.layout.snackbar, null)

            val textView = view.findViewById<TextView>(android.R.id.text1)
            textView.setText(text)

            val position = IntArray(2)
            anchorView.getLocationOnScreen(position)

            var statusBarHeight = 0
            val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) {
                statusBarHeight = context.resources.getDimensionPixelSize(resourceId)
            }

            val topMargin = context.resources.getDimensionPixelSize(R.dimen.margin_padding_size_64)

            val t = Toast(anchorView.context)
            t.view = view
            t.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, (position[1] - statusBarHeight) + topMargin)
            t.show()
        }
    }

}