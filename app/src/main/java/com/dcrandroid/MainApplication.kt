/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid

import android.app.Application
import android.content.Context
import com.dcrandroid.activities.CustomCrashReport
import org.acra.ACRA
import org.acra.config.CoreConfigurationBuilder
import org.acra.config.DialogConfigurationBuilder
import org.acra.config.HttpSenderConfigurationBuilder
import org.acra.data.StringFormat
import org.acra.sender.HttpSender

class MainApplication : Application() {
    companion object {
        val appUpTimeSeconds = System.currentTimeMillis() / 1000
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        if (BuildConfig.IS_TESTNET) {
            try {
                val builder = CoreConfigurationBuilder(this)
                builder.setBuildConfigClass(BuildConfig::class.java)
                    .setReportFormat(StringFormat.JSON)

                builder.getPluginConfigurationBuilder(HttpSenderConfigurationBuilder::class.java)
                    .setUri("https://decred-widget-crash.herokuapp.com/logs/Dcrandroid")
                    .setHttpMethod(HttpSender.Method.POST)
                    .setEnabled(true)

                builder.getPluginConfigurationBuilder(DialogConfigurationBuilder::class.java)
                    .setReportDialogClass(CustomCrashReport::class.java)
                    .setResTheme(R.style.AppTheme)
                    .setEnabled(true)

                ACRA.init(this, builder.build())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}