package com.decrediton.Activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.decrediton.DcrResponse;
import com.decrediton.MainActivity;
import com.decrediton.R;
import com.decrediton.Util.Utils;
import com.decrediton.workers.BackgroundWorker;

import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;

import dcrwallet.Dcrwallet;

/**
 * Created by Macsleven on 24/12/2017.
 */

public class SplashScreen extends AppCompatActivity {
    // Splash screen timer
    private static int SPLASH_TIME_OUT = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_page);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // This method will be executed once the timer is over
                //Check if wallet db exists or not
                File f = new File(Dcrwallet.getHomeDir()+"/mainnet/wallet.db");
                if(!f.exists()) {
                    // Start your app main activity
                    Intent i = new Intent(SplashScreen.this, SetupWalletActivity.class);
                    startActivity(i);
                    // close this activity
                    finish();
                }else{
                    Toast.makeText(SplashScreen.this, "Wallet already exist", Toast.LENGTH_SHORT).show();
                    try {
                        Method method = Dcrwallet.class.getDeclaredMethod("openWallet");
                        Method callback = SplashScreen.this.getClass().getDeclaredMethod("openWalletCallback", String.class);
                        ProgressDialog pd = Utils.getProgressDialog(SplashScreen.this, false, false,"Loading Wallet...");
                        new BackgroundWorker(callback, pd, SplashScreen.this, false).execute(method);
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, SPLASH_TIME_OUT);
    }

    public void openWalletCallback(String responseJson){
        try {
            DcrResponse response = DcrResponse.parse(responseJson);
            if(response.errorOccurred){
                Toast.makeText(this, "Could not open wallet", Toast.LENGTH_SHORT).show();
            }else{
                Intent i = new Intent(this, MainActivity.class);
                startActivity(i);
                //Finish all the activities before this
                ActivityCompat.finishAffinity(this);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}