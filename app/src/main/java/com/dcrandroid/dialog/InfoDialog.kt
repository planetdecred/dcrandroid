/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.dialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.dcrandroid.R
import kotlinx.android.synthetic.main.info_dialog.*

class InfoDialog(context: Context?) : Dialog(context), View.OnClickListener {

    private var btnPositiveClick: DialogInterface.OnClickListener? = null
    private var btnNegativeClick: DialogInterface.OnClickListener? = null

    private var messageClick: View.OnClickListener? = null

    private var dialogTitle: CharSequence? = null
    private var message: CharSequence? = null

    private var btnPositiveText: String? = null
    private var btnNegativeText: String? = null

    private var titleTextColor: Int? = null
    private var messageTextColor: Int? = null
    private var btnPositiveTextColor: Int? = null
    private var btnNegativeTextColor: Int? = null

    private var mView: View? = null

    private var iconResId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.info_dialog)

        val btnPositive = findViewById<TextView>(R.id.btn_positive)
        val btnNegative = findViewById<TextView>(R.id.btn_negative)

        val tvTitle = findViewById<TextView>(R.id.title)
        val tvMessage = findViewById<TextView>(R.id.message)

        val ivIcon = findViewById<ImageView>(R.id.icon)

        tvTitle.text = dialogTitle
        tvMessage.text = message

        if (messageClick != null) {
            tvMessage.setOnClickListener(messageClick)
        }

        if (titleTextColor != null) {
            tvTitle.setTextColor(titleTextColor!!)
        }

        if (messageTextColor != null) {
            tvMessage.setTextColor(messageTextColor!!)
        }

        if (iconResId != null) {
            ivIcon.visibility = View.VISIBLE
            ivIcon.setImageResource(iconResId!!)
        }

        if (mView != null) {
            view_layout.addView(mView)
        }

        if (btnPositiveText != null) {
            btnPositive.visibility = View.VISIBLE
            btnPositive.text = btnPositiveText
            if (btnPositiveTextColor != null) {
                btnPositive.setTextColor(btnPositiveTextColor!!)
            }
            btnPositive.setOnClickListener(this)
        }

        if (btnNegativeText != null) {
            btnNegative.visibility = View.VISIBLE
            btnNegative.text = btnNegativeText
            if (btnNegativeTextColor != null) {
                btnNegative.setTextColor(btnNegativeTextColor!!)
            }
            btnNegative.setOnClickListener(this)
        }

        if (btnNegativeText == null && btnPositiveText == null) {
            findViewById<LinearLayout>(R.id.btn_layout).visibility = View.GONE
        }
    }

    fun setDialogTitle(title: CharSequence?): InfoDialog {
        this.dialogTitle = title
        return this
    }

    fun setTitleTextColor(color: Int): InfoDialog {
        this.titleTextColor = color
        return this
    }

    fun setMessage(message: CharSequence?): InfoDialog {
        this.message = message
        return this
    }

    fun setMessageTextColor(color: Int): InfoDialog {
        this.messageTextColor = color
        return this
    }

    fun setPositiveButton(text: String, listener: DialogInterface.OnClickListener?): InfoDialog {
        this.btnPositiveText = text
        this.btnPositiveClick = listener
        return this
    }

    fun setNegativeButton(text: String, listener: DialogInterface.OnClickListener?): InfoDialog {
        this.btnNegativeText = text
        this.btnNegativeClick = listener
        return this
    }

    fun setMessageClickListener(listener: View.OnClickListener): InfoDialog {
        this.messageClick = listener
        return this
    }

    fun setButtonTextColor(color: Int, which: Int): InfoDialog {
        when (which) {
            DialogInterface.BUTTON_POSITIVE -> {
                btnPositiveTextColor = color
            }
            DialogInterface.BUTTON_NEGATIVE -> {
                btnNegativeTextColor = color
            }
        }
        return this
    }

    fun setIcon(resId: Int): InfoDialog {
        this.iconResId = resId
        return this
    }

    fun setView(view: View): InfoDialog {
        this.mView = view
        return this
    }

    override fun onClick(v: View?) {
        dismiss()
        when (v?.id) {
            R.id.btn_positive -> {
                if (btnPositiveClick != null) {
                    btnPositiveClick?.onClick(this, DialogInterface.BUTTON_POSITIVE)
                }
            }
            R.id.btn_negative -> {
                if (btnNegativeClick != null) {
                    btnNegativeClick?.onClick(this, DialogInterface.BUTTON_NEGATIVE)
                }
            }
        }
    }
}