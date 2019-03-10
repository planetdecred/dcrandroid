/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.dialog

import android.content.Context
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.dcrandroid.R
import com.google.android.material.bottomsheet.BottomSheetDialog

class BiometricDialogV23(context: Context) : BottomSheetDialog(context) {

    private var cancel: Button? = null
    private var tvTitle: TextView? = null
    private var imgLogo: ImageView? = null

    private var listener: CancelListener? = null

    init {
        val bottomSheetView = layoutInflater.inflate(R.layout.fingerprint_bottom_sheet, null)
        setContentView(bottomSheetView)

        cancel = bottomSheetView.findViewById(R.id.btn_cancel)
        cancel!!.setOnClickListener {
            dismiss()
            if(listener != null){
                listener!!.onCancel()
            }
        }

        tvTitle = bottomSheetView.findViewById(R.id.item_title)
        imgLogo = bottomSheetView.findViewById(R.id.img_logo)

        try {
            val drawable = getContext().packageManager.getApplicationIcon(context.packageName)
            imgLogo!!.setImageDrawable(drawable)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setTitle(title: String) {
        tvTitle!!.text = title
    }

    fun setCancelListener(listener: CancelListener){
        this.listener = listener
    }

    public interface CancelListener{
        abstract fun onCancel()
    }
}