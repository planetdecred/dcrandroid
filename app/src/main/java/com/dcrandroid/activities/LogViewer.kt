/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities

import android.os.Bundle
import android.view.ViewTreeObserver
import android.widget.TextView
import android.widget.Toast
import com.dcrandroid.BuildConfig
import com.dcrandroid.R
import com.dcrandroid.util.SnackBar
import com.dcrandroid.util.Utils
import kotlinx.android.synthetic.main.activity_log_viewer.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

const val MENU_ITEM = 1

class LogViewer : BaseActivity(), ViewTreeObserver.OnScrollChangedListener {

    private lateinit var updateJob: Job
    private var logTextView: TextView? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_log_viewer)
        logTextView = findViewById(R.id.log_text)

        go_back.setOnClickListener { finish() }
        iv_copy_wallet_log.setOnClickListener {
            Utils.copyToClipboard(this, logTextView!!.text.toString(), R.string.wallet_log_copied)
        }
        log_scroll_view.viewTreeObserver.addOnScrollChangedListener(this)

        updateJob = GlobalScope.launch(Dispatchers.IO) {
            try {
                val logPath = filesDir.toString() + BuildConfig.LogDir
                val file = File(logPath)
                if (!file.exists()) {
                    SnackBar.showError(
                        this@LogViewer,
                        R.string.log_file_not_found,
                        Toast.LENGTH_LONG
                    )
                    return@launch
                }

                val p = Runtime.getRuntime().exec("tail -f -n500 $file")
                val input = java.io.BufferedReader(java.io.InputStreamReader(p.inputStream))
                var line = input.readLine()

                while (line != null) {
                    addLine("\n" + line)
                    line = input.readLine()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        updateJob.cancel()
    }

    private fun addLine(line: String) = GlobalScope.launch(Dispatchers.Main) {
        logTextView!!.append(line)
    }

    override fun onScrollChanged() {
        app_bar.elevation = if (log_scroll_view.scrollY > 0) {
            resources.getDimension(R.dimen.app_bar_elevation)
        } else {
            0f
        }
    }

}
