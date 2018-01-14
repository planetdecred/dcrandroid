package com.decrediton.Activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.decrediton.Util.DcrResponse;
import com.decrediton.MainActivity;
import com.decrediton.R;
import com.decrediton.Util.MyCustomTextView;
import com.decrediton.Util.PreferenceUtil;
import com.decrediton.Util.Utils;
import com.decrediton.workers.BackgroundWorker;

import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;

import dcrwallet.Dcrwallet;

/**
 * Created by Macsleven on 24/12/2017.
 */

public class SplashScreen extends AppCompatActivity {
    // Splash screen timer
    private static int SPLASH_TIME_OUT = 5000;
    private void startServer(){
        new Thread(){
            public void run(){
                try {
                    this.setPriority(MAX_PRIORITY);
                    writeDcrwalletFiles();
                    writeDcrdFiles();
                    System.out.println("Dcrwallet starting");
                    Dcrwallet.main();
                    System.out.println("Dcrwallet started");
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void writeDcrwalletFiles() throws Exception{
        System.out.println("Wallet Home Dir: "+ Dcrwallet.getHomeDir());
        File path = new File(Dcrwallet.getHomeDir()+"/");
        path.mkdirs();
        String[] files = {"dcrwallet.conf","rpc.key","rpc.cert"};
        String[] assetFilesName = {"sample-dcrwallet.conf","rpc.key","rpc.cert"};
        for(int i = 0; i < files.length; i++) {
            File file = new File(path, files[i]);
            if (!file.exists()) {
                System.out.println("File: "+file.getAbsolutePath());
                file.createNewFile();
                System.out.println("Writing file "+file.getAbsolutePath());
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
                System.out.println("Written file "+file.getAbsolutePath());
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
                System.out.println("Writing file "+file.getAbsolutePath());
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
                System.out.println("Written file "+file.getAbsolutePath());
            }
        }
    }
    MyCustomTextView tvLoading;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startServer();
        setContentView(R.layout.splash_page);
        tvLoading = (MyCustomTextView) findViewById(R.id.loading_status);
        String walletPath = Dcrwallet.getHomeDir()+"/mainnet/wallet.db";
        if(Dcrwallet.isTestNet()){
            walletPath = Dcrwallet.getHomeDir()+"/testnet2/wallet.db";
        }
        File f = new File(walletPath);
        if(!f.exists()){
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    createWallet();
                }
            }, 3000);
        }else{
            openWallet();
        }
    }

    private void setText(final String str){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvLoading.setText(str);
            }
        });
    }

    private void createWallet(){
        new Thread(){
            public void run(){
                setText("Waiting for dcrwallet to come online");
                for(;;) {
                    if(Dcrwallet.testConnect()){
                        break;
                    }
                    try {
                        sleep(1500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
//                setText("Waiting for dcrd to come online");
//                String dcrdAddress = "127.0.0.1:9109";
//                if(Dcrwallet.isTestNet()){
//                    dcrdAddress = "127.0.0.1:19109";
//                }
//                for(;;) {
//                    if(Dcrwallet.connectToDcrd(dcrdAddress)){
//                        break;
//                    }
//                    try {
//                        sleep(1500);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
                Intent i = new Intent(SplashScreen.this, SetupWalletActivity.class);
                startActivity(i);
                finish();
            }
        }.start();
    }

    private void openWallet(){
        new Thread(){
            public void run(){
                System.out.println("Dcrwallet");
                setText("Waiting for dcrwallet to come online");
                for(;;) {
                    if(Dcrwallet.testConnect()){
                        break;
                    }
                    try {
                        sleep(1500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                setText("Waiting for dcrd to come online");
                String dcrdAddress = "127.0.0.1:9109";
                if(Dcrwallet.isTestNet()){
                    dcrdAddress = "127.0.0.1:19109";
                }
                for(;;) {
                    if(Dcrwallet.connectToDcrd(dcrdAddress)){
                        break;
                    }
                    try {
                        sleep(1500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("Opening");
                setText("Opening wallet...");
                final String json = Dcrwallet.openWallet();
                System.out.println("Blocks");
                setText("Subscribing to block notifications...");
                Dcrwallet.subscibeToBlockNotifications();
                PreferenceUtil util = new PreferenceUtil(SplashScreen.this);
                if(!util.get("discover_address").equals("true")) {
                    setText("Discovering addresses...");
                    Dcrwallet.discoverAddresses(util.get("key"));
                    util.set("discover_address","true");
                }
                setText("Fetching Headers...");
                int blockHeight = Dcrwallet.fetchHeaders();
                if(blockHeight != -1){
                    util.set(PreferenceUtil.BLOCK_HEIGHT,String.valueOf(blockHeight));
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        openWalletCallback(json);
                    }
                });
            }
        }.start();
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
    @Override
    public void onBackPressed() {
    }

}