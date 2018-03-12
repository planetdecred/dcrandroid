package com.dcrandroid;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.dcrandroid.service.DcrdService;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.Utils;


import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

/**
 * Created by collins on 12/26/17.
 *
 */
@ReportsCrashes(formUri = "https://decred-widget-crash.herokuapp.com/logs/Decrediton",
        mode = ReportingInteractionMode.DIALOG,
        resDialogText = R.string.crash_dialog_text,
        resDialogTheme = R.style.AppTheme
)
public class MainApplication extends Application {

    PreferenceUtil util;
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            ACRA.init(this);
            Log.d("ACRA","ACRA INIT SUCCESS");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            Utils.writeDcrdFiles(this);
            Utils.writeDcrwalletFiles(this);
            Utils.writeDcrdCertificate(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        util = new PreferenceUtil(this);
        if(util.getBoolean(getString(R.string.key_connection_local_dcrd), true)){
            System.out.println("Starting local server");
            Intent i = new Intent(this, DcrdService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(i);
            }else{
                startService(i);
            }
            //Dcrwallet.runDcrd();
        }else{
            System.out.println("Not starting local server");
        }
    }
}