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

public class SplashScreen extends AppCompatActivity implements Animation.AnimationListener {
    Animation animRotate;
    ImageView imgAnim;
    private void startServer(){
        new Thread(){
            public void run(){
                try {
                    this.setPriority(MAX_PRIORITY);
                    writeDcrwalletFiles();
                    writeDcrdFiles();
                    Dcrwallet.main();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }.start();
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
        //String[] assetFilesName = {"dcrdrpc.key","dcrdrpc.cert","dcrd.conf"};
        String[] assetFilesName = {"dcrdrpc.key","devrpc.cert","dcrd.conf"};
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
    MyCustomTextView tvLoading;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startServer();
        setContentView(R.layout.splash_page);
        imgAnim=(ImageView)findViewById(R.id.splashscreen_icon);
        animRotate= AnimationUtils.loadAnimation(getApplicationContext(), R.anim.anim_rotate);
        animRotate.setAnimationListener(this);
        imgAnim.startAnimation(animRotate);
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
                setText(getString(R.string.waiting_for_dcrwallet));
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
                setText(getString(R.string.waiting_for_dcrwallet));
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
                setText(getString(R.string.waiting_for_dcrd));
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
                setText(getString(R.string.opening_wallet));
                final String json = Dcrwallet.openWallet();
                System.out.println("Blocks");
                setText(getString(R.string.subscribe_to_block_notification));
                Dcrwallet.subscibeToBlockNotifications();
                PreferenceUtil util = new PreferenceUtil(SplashScreen.this);
                if(!util.get("discover_address").equals("true")) {
                    setText(getString(R.string.discovering_address));
                    Dcrwallet.discoverAddresses(util.get("key"));
                    util.set("discover_address","true");
                }
                setText(getString(R.string.fetching_headers));
                int blockHeight = Dcrwallet.fetchHeaders();
                if(blockHeight != -1){
                    util.set(PreferenceUtil.BLOCK_HEIGHT,String.valueOf(blockHeight));
                }
                System.out.println("Finished fetching headers");
                setText("Publish Unmined Transactions");
                try {
                    Dcrwallet.publishUnminedTransactions();
                } catch (Exception e) {
                    e.printStackTrace();
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
                Toast.makeText(this, R.string.could_not_open_wallet, Toast.LENGTH_SHORT).show();
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

    @Override
    public void onAnimationStart(Animation animation) {

    }

    @Override
    public void onAnimationEnd(Animation animation) {

        imgAnim.startAnimation(animRotate);
    }

    @Override
    public void onAnimationRepeat(Animation animation) {

    }
}