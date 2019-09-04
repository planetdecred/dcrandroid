/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.util

import android.animation.ObjectAnimator
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import com.dcrandroid.R

class SnackBar {

    companion object{
        fun showText(anchorView: View, @StringRes text: Int, length: Int = Toast.LENGTH_SHORT){
            val position = IntArray(2)
            anchorView.getLocationOnScreen(position)

            showText(position[1], anchorView.context, text, length)
        }

        fun showText(context: Context, @StringRes text: Int, length: Int = Toast.LENGTH_SHORT){
            showText(0, context, text, length)
        }

        private fun showText(x: Int = 0, context: Context, @StringRes text: Int, length: Int = Toast.LENGTH_SHORT){
            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.snackbar, null)

            val textView = view.findViewById<TextView>(android.R.id.text1)
            textView.setText(text)

            var statusBarHeight = 0
            val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) {
                statusBarHeight = context.resources.getDimensionPixelSize(resourceId)
            }

            val topMargin = context.resources.getDimensionPixelSize(R.dimen.margin_padding_size_64)
            val viewX = (x - statusBarHeight) + topMargin
            view.translationY = viewX.toFloat()

            val t = Toast(context)
            t.view = view
            t.duration = length
            t.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, viewX)
            t.show()
            ObjectAnimator.ofFloat(view, "translationY", 0f).start()
        }
    }

}