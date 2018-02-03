package com.decrediton.activities;

import android.app.Application;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import com.decrediton.MainActivity;
import com.decrediton.R;
import com.decrediton.util.DcrResponse;
import com.decrediton.util.MyCustomTextView;
import com.decrediton.util.PreferenceUtil;
import com.decrediton.util.Utils;

import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import dcrwallet.Dcrwallet;

/**
 * Created by Macsleven on 24/12/2017.
 */

public class SplashScreen extends AppCompatActivity implements Animation.AnimationListener {
    Animation animRotate;
    ImageView imgAnim;
    PreferenceUtil util;
    MyCustomTextView tvLoading;
    private void startServer(){
        new Thread(){
            public void run(){
                try {
                    this.setPriority(MAX_PRIORITY);
                    Dcrwallet.main();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }.start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        util = new PreferenceUtil(this);
        startServer();
        setContentView(R.layout.splash_page);
        imgAnim= findViewById(R.id.splashscreen_icon);
        animRotate= AnimationUtils.loadAnimation(getApplicationContext(), R.anim.anim_rotate);
        animRotate.setAnimationListener(this);
        imgAnim.startAnimation(animRotate);
        tvLoading = findViewById(R.id.loading_status);
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
                load();
            }
        }.start();
    }

    public void load(){
        new Thread(){
            public void run() {
                setText(getString(R.string.waiting_for_dcrd));
                String dcrdAddress = Utils.getDcrdNetworkAddress(SplashScreen.this);
                for (int i = 0; i < 10 ; i++ ) {
                    try {
                        if (Dcrwallet.connectToDcrd(dcrdAddress, Utils.getConnectionCertificate(SplashScreen.this).getBytes())) {
                            break;
                        }
                        sleep(2500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (i == 4){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                new AlertDialog.Builder(SplashScreen.this)
                                        .setTitle(R.string.error_camel)
                                        .setMessage(R.string.error_msg_could_not_connect_dcrd_10_secs)
                                        .setPositiveButton(R.string.retry_caps, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                load();
                                            }
                                        }).setNegativeButton(R.string.exit_cap, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        Dcrwallet.exit();
                                        finish();
                                    }
                                }).setNeutralButton(R.string.settings_cap, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        Intent intent = new Intent(SplashScreen.this,SettingsActivity.class);
                                        startActivityForResult(intent,2);
                                    }
                                }).show();
                            }
                        });
                        return;
                    }
                }
                System.out.println("Opening");
                setText(getString(R.string.opening_wallet));
                final String json = Dcrwallet.openWallet();
                System.out.println("Blocks");
                setText(getString(R.string.subscribe_to_block_notification));
                Dcrwallet.subscibeToBlockNotifications();
                PreferenceUtil util = new PreferenceUtil(SplashScreen.this);
                if (!util.get("discover_address").equals("true")) {
                    setText(getString(R.string.discovering_address));
                    Dcrwallet.discoverAddresses(util.get("key"));
                    util.set("discover_address", "true");
                }
                setText(getString(R.string.fetching_headers));
                int blockHeight = Dcrwallet.fetchHeaders();
                if (blockHeight != -1) {
                    util.set(PreferenceUtil.BLOCK_HEIGHT, String.valueOf(blockHeight));
                }
                System.out.println("Finished fetching headers");
                setText(getString(R.string.publish_unmined_transaction));
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
            }}.start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 2){
            System.out.println("Activity Finished");
            load();
        }
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