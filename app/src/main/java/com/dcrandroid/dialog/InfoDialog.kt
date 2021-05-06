/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.dialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Html
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.dcrandroid.R
import com.dcrandroid.extensions.show

class InfoDialog(context: Context) : Dialog(context), View.OnClickListener {

    private var btnPositiveClick: DialogInterface.OnClickListener? = null
    private var btnNegativeClick: DialogInterface.OnClickListener? = null

    private var messageClick: View.OnClickListener? = null

    private var dialogTitle: CharSequence? = null
    private var message: CharSequence? = null

    private var btnPositiveText: String? = null
    var btnPositiveColor: Int = R.color.blue

    private var btnNegativeText: String? = null
    var btnNegativeColor: Int = R.color.blue

    private var iconBackground = R.color.white
    private var icon: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setContentView(R.layout.info_dialog)

        val btnPositive = findViewById<TextView>(R.id.btn_positive)
        val btnNegative = findViewById<TextView>(R.id.btn_negative)

        val tvTitle = findViewById<TextView>(R.id.title)
        val tvMessage = findViewById<TextView>(R.id.message)

        val llIconBackground = findViewById<LinearLayout>(R.id.icon_background)
        val ivIcon = findViewById<ImageView>(R.id.iv_icon)

        if (dialogTitle != null) {
            tvTitle.show()
            tvTitle.text = dialogTitle
        }

        tvMessage.text = message

        if (messageClick != null) {
            tvMessage.setOnClickListener(messageClick)
        }

        if (btnPositiveText != null) {
            btnPositive.visibility = View.VISIBLE
            btnPositive.text = btnPositiveText
            btnPositive.setTextColor(context.getColor(btnPositiveColor))
            btnPositive.setOnClickListener(this)
        }

        if (btnNegativeText != null) {
            btnNegative.visibility = View.VISIBLE
            btnNegative.text = btnNegativeText
            btnNegative.setTextColor(context.getColor(btnNegativeColor))
            btnNegative.setOnClickListener(this)
        }

        if (btnNegativeText == null && btnPositiveText == null) {
            findViewById<LinearLayout>(R.id.btn_layout).visibility = View.GONE
        }

        if (icon != null) {
            llIconBackground.setBackgroundDrawable(context.getDrawable(iconBackground))
            ivIcon.setImageResource(icon!!)
            ivIcon.show()
        }
    }

    fun setDialogTitle(@StringRes title: Int): InfoDialog {
        return this.setDialogTitle(Html.fromHtml(context.getString(title)))
    }

    fun setDialogTitle(title: CharSequence?): InfoDialog {
        this.dialogTitle = title
        return this
    }

    fun setMessage(@StringRes message: Int): InfoDialog {
        return this.setMessage(Html.fromHtml(context.getString(message)))
    }

    fun setMessage(message: CharSequence?): InfoDialog {
        this.message = message
        return this
    }

    fun setPositiveButton(text: Int, listener: DialogInterface.OnClickListener? = null): InfoDialog {
        return this.setPositiveButton(context.getString(text), listener)
    }

    fun setPositiveButton(text: String, listener: DialogInterface.OnClickListener? = null): InfoDialog {
        this.btnPositiveText = text
        this.btnPositiveClick = listener
        return this
    }

    fun setNegativeButton(text: Int, listener: DialogInterface.OnClickListener? = null): InfoDialog {
        return this.setNegativeButton(context.getString(text), listener)
    }

    fun setNegativeButton(text: String, listener: DialogInterface.OnClickListener? = null): InfoDialog {
        this.btnNegativeText = text
        this.btnNegativeClick = listener
        return this
    }

    fun setIcon(@DrawableRes iconResource: Int, @DrawableRes iconBackground: Int = R.color.white): InfoDialog {
        this.icon = iconResource
        this.iconBackground = iconBackground
        return this
    }

    fun cancelable(cancelable: Boolean): InfoDialog {
        setCancelable(cancelable)
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