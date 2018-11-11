package com.dcrandroid.dialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import com.dcrandroid.R

class CrashDialog(context: Context?) : Dialog(context), View.OnClickListener {

    private var btnPositiveClick: DialogInterface.OnClickListener? = null
    private var btnNegativeClick: DialogInterface.OnClickListener? = null

    private var ViewHideReportClick: View.OnClickListener? = null
    private var CopyReportClick: View.OnClickListener? = null

    private var dialogTitle: CharSequence? = null
    private var message: CharSequence? = null

    private var btnPositiveText: String? = null
    private var btnNegativeText: String? = null

    private var tvViewHideReport: TextView? = null
    private var tvReport: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.crash_dialog)

        val btnPositive = findViewById<TextView>(R.id.btn_positive)
        val btnNegative = findViewById<TextView>(R.id.btn_negative)

        val tvTitle = findViewById<TextView>(R.id.title)
        val tvMessage = findViewById<TextView>(R.id.message)

        tvViewHideReport = findViewById(R.id.view_hide_report)
        tvReport = findViewById(R.id.report)

        val tvCopyReport = findViewById<TextView>(R.id.copy_report)

        tvTitle.text = dialogTitle
        tvMessage.text = message

        if (ViewHideReportClick != null) {
            tvViewHideReport!!.setOnClickListener(ViewHideReportClick)
        }

        if (CopyReportClick != null) {
            tvCopyReport.setOnClickListener(CopyReportClick)
        }

        if (btnPositiveText != null) {
            btnPositive.visibility = View.VISIBLE
            btnPositive.text = btnPositiveText
            btnPositive.setOnClickListener(this)
        }

        if (btnNegativeText != null) {
            btnNegative.visibility = View.VISIBLE
            btnNegative.text = btnNegativeText
            btnNegative.setOnClickListener(this)
        }

        if (btnNegativeText == null && btnPositiveText == null) {
            findViewById<LinearLayout>(R.id.btn_layout).visibility = View.GONE
        }
    }

    fun setDialogTitle(title: CharSequence?): CrashDialog {
        this.dialogTitle = title
        return this
    }

    fun setMessage(message: CharSequence?): CrashDialog {
        this.message = message
        return this
    }

    fun setPositiveButton(text: String, listener: DialogInterface.OnClickListener?): CrashDialog {
        this.btnPositiveText = text
        this.btnPositiveClick = listener
        return this
    }

    fun setNegativeButton(text: String, listener: DialogInterface.OnClickListener?): CrashDialog {
        this.btnNegativeText = text
        this.btnNegativeClick = listener
        return this
    }

    fun setViewHideReportClickListener(listener: View.OnClickListener): CrashDialog {
        this.ViewHideReportClick = listener
        return this
    }

    fun setCopyReportClickListener(listener: View.OnClickListener): CrashDialog {
        this.CopyReportClick = listener
        return this
    }

    fun showReport(report: String) {
        tvReport!!.visibility = View.VISIBLE
        tvReport!!.text = report
        tvViewHideReport!!.text = context.getString(R.string.hide_report)
    }

    fun hideReport(report: String) {
        tvReport!!.visibility = View.GONE
        tvReport!!.text = report
        tvViewHideReport!!.text = context.getString(R.string.view_report)
    }

    fun isHidden(): Boolean {
        val tvReport = findViewById<TextView>(R.id.report)
        if (tvReport.visibility == View.VISIBLE) {
            return false
        }
        return true
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