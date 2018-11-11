package com.dcrandroid;

import android.app.Application;
import android.content.Context;

import com.dcrandroid.activities.CustomCrashReport;
import com.dcrandroid.data.Constants;
import com.dcrandroid.util.PreferenceUtil;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

/**
 * Created by collins on 12/26/17.
 */
@ReportsCrashes(formUri = "https://decred-widget-crash.herokuapp.com/logs/Dcrandroid",
        mode = ReportingInteractionMode.DIALOG,
        resDialogTheme = R.style.LightTheme,
        reportDialogClass = CustomCrashReport.class
)
public class MainApplication extends Application {

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

    @Override
    public void onCreate() {
        super.onCreate();
        PreferenceUtil util = new PreferenceUtil(this);
        util.setInt(Constants.APP_VERSION, BuildConfig.VERSION_CODE);
    }
}
