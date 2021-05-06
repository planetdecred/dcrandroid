/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid;

import android.app.Application;
import android.content.Context;

import com.dcrandroid.activities.CustomCrashReport;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

@ReportsCrashes(formUri = "https://decred-widget-crash.herokuapp.com/logs/Dcrandroid",
        mode = ReportingInteractionMode.DIALOG,
        resDialogTheme = R.style.LightTheme,
        reportDialogClass = CustomCrashReport.class
)
public class MainApplication extends Application {

    public static long appUpTimeSeconds = System.currentTimeMillis() / 1000;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        if (BuildConfig.IS_TESTNET) {
            try {
                ACRA.init(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
