/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast

import com.dcrandroid.BuildConfig
import com.dcrandroid.R
import com.dcrandroid.util.SnackBar
import com.dcrandroid.util.Utils
import kotlinx.coroutines.*

import java.io.File
import java.lang.Exception

const val MENU_ITEM = 1

class LogViewer : BaseActivity() {

    private lateinit var updateJob: Job
    private var logTextView: TextView? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_log_viewer)
        logTextView = findViewById(R.id.log_text)

        updateJob = GlobalScope.launch(Dispatchers.IO){
           try{
               val logPath = filesDir.toString() + BuildConfig.LogDir
               val file = File(logPath)
               if (!file.exists()) {
                   SnackBar.showError(this@LogViewer, R.string.log_file_not_found, Toast.LENGTH_LONG)
                   return@launch
               }

               val p = Runtime.getRuntime().exec("tail -f -n500 $file")
               val input = java.io.BufferedReader(java.io.InputStreamReader(p.inputStream))
               var line = input.readLine()

               while(line != null){
                   addLine("\n" + line)
                   line = input.readLine()
               }

           }catch (e: Exception) {
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(Menu.NONE, MENU_ITEM, Menu.NONE, "Copy").setIcon(R.drawable.ic_copy).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            MENU_ITEM -> {
                Utils.copyToClipboard(this, logTextView!!.text.toString(), R.string.wallet_log_copied) // TODO:
                true
            }

            else -> false
        }
    }

}
