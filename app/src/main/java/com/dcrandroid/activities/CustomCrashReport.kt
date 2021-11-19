/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities

import android.content.DialogInterface
import android.os.Bundle
import com.dcrandroid.R
import com.dcrandroid.dialog.CrashDialog
import com.dcrandroid.util.Utils
import org.acra.ACRAConstants
import org.acra.config.ConfigUtils
import org.acra.config.DialogConfiguration
import org.acra.dialog.CrashReportDialog
import org.acra.dialog.CrashReportDialogHelper
import org.acra.file.CrashReportPersister
import org.acra.interaction.DialogInteraction
import org.json.JSONException
import java.io.File
import java.io.IOException

class CustomCrashReport : CrashReportDialog() {
    private var helper: CrashReportDialogHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            helper = CrashReportDialogHelper(this, intent)
            val dialogConfiguration =
                ConfigUtils.getPluginConfiguration(helper!!.config, DialogConfiguration::class.java)
            val themeResourceId = dialogConfiguration.resTheme()
            if (themeResourceId != ACRAConstants.DEFAULT_RES_VALUE) setTheme(themeResourceId)
            buildAndShowDialog(savedInstanceState)
        } catch (e: IllegalArgumentException) {
            finish()
        }
    }

    override fun buildAndShowDialog(savedInstanceState: Bundle?) {
        val crashDialog = CrashDialog(this)
        crashDialog.setDialogTitle(getString(R.string.app_crashed))
        crashDialog.setMessage(getString(R.string.crash_dialog_text))
        crashDialog.setCanceledOnTouchOutside(false)
        crashDialog.setPositiveButton(
            getString(R.string.send_report),
            DialogInterface.OnClickListener { dialog, _ ->
                helper?.sendCrash("", "")
                dialog.dismiss()
                finish()
            })
        crashDialog.setNegativeButton(
            getString(R.string.dont_send),
            DialogInterface.OnClickListener { dialog, _ ->
                helper?.cancelReports()
                dialog.dismiss()
                finish()
            })
        crashDialog.setCopyReportClickListener {
            Utils.copyToClipboard(
                this,
                loadReports(),
                R.string.crash_report_copied
            )
        } //TODO:
        crashDialog.setViewHideReportClickListener {
            if (crashDialog.isHidden()) {
                crashDialog.showReport(loadReports())
            } else {
                crashDialog.hideReport(loadReports())
            }
        }
        crashDialog.show()
    }

    private fun loadReports(): String {
        val sReportFile = intent.getSerializableExtra(DialogInteraction.EXTRA_REPORT_FILE)
        val reportFile = sReportFile as File
        if (helper != null) {
            val persister = CrashReportPersister()
            try {
                val crashData = persister.load(reportFile)
                return crashData.toJSON()
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        return ""
    }
}