package com.dcrandroid.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import com.dcrandroid.MainActivity;
import com.dcrandroid.R;
import com.dcrandroid.util.DcrResponse;
import com.dcrandroid.util.MyCustomTextView;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.Utils;

import org.json.JSONException;

import java.io.File;

import dcrwallet.BlockScanResponse;
import dcrwallet.Dcrwallet;

/**
 * Created by Macsleven on 24/12/2017.
 */

public class SplashScreen extends AppCompatActivity implements Animation.AnimationListener, BlockScanResponse {
    Animation animRotate;
    ImageView imgAnim;
    PreferenceUtil util;
    MyCustomTextView tvLoading;
    private String json;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        util = new PreferenceUtil(this);
        System.out.println("Is Running: "+Dcrwallet.isRunning());
        Dcrwallet.runDcrwallet();
        setContentView(R.layout.splash_page);
        imgAnim= findViewById(R.id.splashscreen_icon);
        imgAnim.setOnClickListener(new DoubleClickListener() {
            @Override
            public void onSingleClick(View v) {
            }
            @Override
            public void onDoubleClick(View v) {
                Intent intent = new Intent(getApplicationContext(),SettingsActivity.class);
                startActivityForResult(intent,2);
            }
        });
        animRotate = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.anim_rotate);
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
                int i = 0;
                for(;;) {
                    if(Dcrwallet.testConnect()){
                        break;
                    }
                    i++;
                    System.out.println("I: "+i);
                    if(i == 6){
                        System.out.println("I is six");
                        Dcrwallet.runDcrwallet();
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
                for (;;) {
                    try {
                        if (Dcrwallet.connectToDcrd(dcrdAddress, Utils.getConnectionCertificate(SplashScreen.this).getBytes())) {

                            break;
                        }
                        sleep(2500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("Opening");
                setText(getString(R.string.opening_wallet));
                json = Dcrwallet.openWallet();
                System.out.println("Blocks");
                setText(getString(R.string.subscribe_to_block_notification));
                Dcrwallet.subscibeToBlockNotifications();
                PreferenceUtil util = new PreferenceUtil(SplashScreen.this);
                if (!util.get("discover_address").equals("true")) {
                    setText(getString(R.string.discovering_address));
                    Dcrwallet.discoverAddresses(util.get("key"));
                    util.set("discover_address", "true");
                }
                System.out.println("Is Running 3: "+Dcrwallet.isRunning());
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
                setText("Scanning blocks");
                Dcrwallet.reScanBlocks(SplashScreen.this, util.getInt("block_checkpoint"));
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

    @Override
    public void onEnd(long height) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                openWalletCallback(json);
            }
        });
    }

    @Override
    public void onScan(long rescanned_through) {
        setText("Scanning blocks "+rescanned_through);
    }

    public abstract class DoubleClickListener implements View.OnClickListener {

        private static final long DOUBLE_CLICK_TIME_DELTA = 300;//milliseconds

        long lastClickTime = 0;

        @Override
        public void onClick(View v) {
            long clickTime = System.currentTimeMillis();
            if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA){
                onDoubleClick(v);
                lastClickTime = 0;
            } else {
                onSingleClick(v);
            }
            lastClickTime = clickTime;
        }

        public abstract void onSingleClick(View v);
        public abstract void onDoubleClick(View v);
    }
}