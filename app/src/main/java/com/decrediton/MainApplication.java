package com.decrediton;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.decrediton.util.PreferenceUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import dcrwallet.Dcrwallet;
import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.config.ACRAConfiguration;
import org.acra.config.ConfigurationBuilder;
/**
 * Created by collins on 12/26/17.
 */
@ReportsCrashes(formUri = "https://decred-widget-crash.herokuapp.com/logs/Decrediton",
        mode = ReportingInteractionMode.DIALOG,
        resDialogText = R.string.crash_dialog_text,
        resDialogTheme = R.style.AppTheme_Dialog
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

    public void writeDcrwalletFiles() throws Exception{
        File path = new File(Dcrwallet.getHomeDir()+"/");
        path.mkdirs();
        String[] files = {"dcrwallet.conf","rpc.key","rpc.cert"};
        String[] assetFilesName = {"sample-dcrwallet.conf","rpc.key","rpc.cert"};
        for(int i = 0; i < files.length; i++) {
            File file = new File(path, files[i]);
            if (!file.exists()) {
                file.createNewFile();
                FileOutputStream fout = new FileOutputStream(file);
                InputStream in = getAssets().open(assetFilesName[i]);
                int len;
                byte[] buff = new byte[8192];
                //read file till end
                while ((len = in.read(buff)) != -1) {
                    fout.write(buff, 0, len);
                }
                fout.flush();
                fout.close();
            }
        }
    }

    public void writeDcrdFiles() throws Exception{
        File path = new File(getFilesDir().getPath(),"/dcrd");
        //File path = new File("./sdcard/.dcrd");
        path.mkdirs();
        String[] files = {"rpc.key","rpc.cert","dcrd.conf"};
        String[] assetFilesName = {"dcrdrpc.key","dcrdrpc.cert","dcrd.conf"};
        //String[] assetFilesName = {"dcrdrpc.key","devrpc.cert","dcrd.conf"};
        for(int i = 0; i < files.length; i++) {
            File file = new File(path, files[i]);
            if (!file.exists() || true) {
                file.createNewFile();
                FileOutputStream fout = new FileOutputStream(file);
                InputStream in = getAssets().open(assetFilesName[i]);
                int len;
                byte[] buff = new byte[8192];
                //read file till end
                while ((len = in.read(buff)) != -1) {
                    fout.write(buff, 0, len);
                }
                fout.flush();
                fout.close();
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            writeDcrdFiles();
            writeDcrwalletFiles();
        } catch (Exception e) {
            e.printStackTrace();
        }
        util = new PreferenceUtil(this);
        if(util.getBoolean(getString(R.string.key_connection_local_dcrd), true)){
            System.out.println("Starting local server");
            //Dcrwallet.runDrcd();
        }else{
            System.out.println("Not starting local server");
        }
    }
}
