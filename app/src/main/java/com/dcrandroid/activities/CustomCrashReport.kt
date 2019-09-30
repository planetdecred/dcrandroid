/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import com.dcrandroid.R
import com.dcrandroid.dialog.CrashDialog
import com.dcrandroid.util.Utils
import org.acra.ACRAConstants
import org.acra.dialog.BaseCrashReportDialog
import org.acra.file.CrashReportPersister
import org.acra.prefs.SharedPreferencesFactory
import org.json.JSONException
import java.io.File
import java.io.IOException

class CustomCrashReport : BaseCrashReportDialog() {
    private var sharedPreferencesFactory: SharedPreferencesFactory? = null

    @CallSuper
    override fun init(savedInstanceState: Bundle?) {
        sharedPreferencesFactory = SharedPreferencesFactory(applicationContext, config)
        val themeResourceId = config.resDialogTheme()
        if (themeResourceId != ACRAConstants.DEFAULT_RES_VALUE) setTheme(themeResourceId)
        buildAndShowDialog(savedInstanceState)
    }

    fun buildAndShowDialog(savedInstanceState: Bundle?) {
        val crashDialog = CrashDialog(this)
        crashDialog.setDialogTitle(getString(R.string.app_crashed))
        crashDialog.setMessage(getString(R.string.crash_dialog_text))
        crashDialog.setCanceledOnTouchOutside(false)
        crashDialog.setPositiveButton(getString(R.string.send_report), DialogInterface.OnClickListener { dialog, _ ->
            sendCrash("", "")
            dialog.dismiss()
            finish()
        })
        crashDialog.setNegativeButton(getString(R.string.dont_send), DialogInterface.OnClickListener { dialog, _ ->
            cancelReports()
            dialog.dismiss()
            finish()
        })
        crashDialog.setCopyReportClickListener(View.OnClickListener { Utils.copyToClipboard(this, loadReports(), R.string.crash_report_copied) }) //TODO:
        crashDialog.setViewHideReportClickListener(View.OnClickListener {
            if (crashDialog.isHidden()) {
                crashDialog.showReport(loadReports())
            } else {
                crashDialog.hideReport(loadReports())
            }
        })
        crashDialog.show()
    }

    fun loadReports(): String {
        val sReportFile = intent.getSerializableExtra(ACRAConstants.EXTRA_REPORT_FILE)
        val reportFile = sReportFile as File
        if (config != null) {
            val persister = CrashReportPersister()
            try {
                val crashData = persister.load(reportFile)
                return crashData.toJSON().toString(2)
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        return ""
    }
}