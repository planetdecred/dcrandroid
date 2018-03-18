package com.dcrandroid;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.dcrandroid.data.BestBlock;
import com.dcrandroid.service.DcrdService;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import dcrwallet.Dcrwallet;
import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.config.ACRAConfiguration;
import org.acra.config.ConfigurationBuilder;
import org.json.JSONException;

/**
 * Created by collins on 12/26/17.
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
        util = new PreferenceUtil(this);
        try {
            Utils.writeDcrdFiles(this);
            Utils.writeDcrwalletFiles(this);
            Utils.writeDcrdCertificate(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Utils.removeDcrwalletConfig("spv");
        if(util.getInt("network_mode") == 1){
            //local full-node
            System.out.println("Starting local server");
            Intent i = new Intent(this, DcrdService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(i);
            }else{
                startService(i);
            }
        }else if(util.getInt("network_mode") == 0){
            //spv
            Utils.setDcrwalletConfig("spv","true");
        }
    }
}
