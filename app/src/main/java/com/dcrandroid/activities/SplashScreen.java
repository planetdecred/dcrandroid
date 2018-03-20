package com.dcrandroid.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.dcrandroid.MainActivity;
import com.dcrandroid.R;
import com.dcrandroid.util.DcrConstants;
import com.dcrandroid.util.MyCustomTextView;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.Utils;

import java.io.File;

import mobilewallet.LibWallet;

/**
 * Created by Macsleven on 24/12/2017.
 */

public class SplashScreen extends AppCompatActivity implements Animation.AnimationListener {
    Animation animRotate;
    ImageView imgAnim;
    PreferenceUtil util;
    MyCustomTextView tvLoading;
    Thread loadThread;
    private DcrConstants constants;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        util = new PreferenceUtil(this);
        setContentView(R.layout.splash_page);
        imgAnim = findViewById(R.id.splashscreen_icon);
        imgAnim.setOnClickListener(new DoubleClickListener() {
            @Override
            public void onSingleClick(View v) {

            }

            @Override
            public void onDoubleClick(View v) {
                if(loadThread != null) {
                    loadThread.interrupt();
                }
                Intent intent = new Intent(getApplicationContext(),SettingsActivity.class);
                startActivityForResult(intent,2);
            }
        });
        animRotate = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.anim_rotate);
        animRotate.setAnimationListener(this);
        imgAnim.startAnimation(animRotate);
        startup();
    }

    private void startup(){
        tvLoading = findViewById(R.id.loading_status);
        constants = DcrConstants.getInstance();
        String homeDir = getFilesDir()+"/dcrwallet/testnet2";
        constants.wallet = new LibWallet(homeDir);
        constants.wallet.initLoader();
        //String walletPath = Dcrwallet.getHomeDir()+"/mainnet/wallet.db";
        File f = new File(homeDir, "wallet.db");
        if(!f.exists()){
            loadThread = new Thread(){
                public void run(){
                    try{
                        sleep(3000);
                        createWallet();
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            };
            loadThread.start();
        }else{
            load();
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
        loadThread = new Thread(){
            public void run(){
                if(isInterrupted()){
                    return;
                }
                if(isInterrupted()){
                    return;
                }
                Intent i = new Intent(SplashScreen.this, SetupWalletActivity.class);
                startActivity(i);
                finish();
            }
        };
        loadThread.start();
    }

    public void load(){
        loadThread = new Thread(){
            public void run() {
                try {
                    System.out.println("Opening");
                    setText(getString(R.string.opening_wallet));
                    constants.wallet.openWallet();
                    setText(getString(R.string.waiting_for_dcrd));
                    String dcrdAddress = Utils.getDcrdNetworkAddress(SplashScreen.this);
                    if (util.getInt("network_mode") != 0) {
                        for (; ; ) {
                            try {
                                if (isInterrupted()) {
                                    return;
                                }
                                if (constants.wallet.startRpcClient(dcrdAddress, "dcrwallet", "dcrwallet", Utils.getConnectionCertificate(SplashScreen.this).getBytes())) {
                                    break;
                                }
                                sleep(2500);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            if (isInterrupted()) {
                                return;
                            }
                        }
                    }
                    if (isInterrupted()) {
                        return;
                    }
                    if (util.getInt("network_mode") == 0) {
                        System.out.println("Connecting to peer");
                    }
                    if (isInterrupted()) {
                        return;
                    }
                    constants.wallet.subscribeToBlockNotifications();
                    constants.wallet.loadActiveDataFilters();
                    PreferenceUtil util = new PreferenceUtil(SplashScreen.this);
                    setText(getString(R.string.fetching_headers));
                    long blockHeight = constants.wallet.fetchHeaders();
                    if (blockHeight != -1) {
                        util.setInt(PreferenceUtil.BLOCK_HEIGHT, (int) blockHeight);
                    }
                    System.out.println("Finished fetching headers");
                    if (isInterrupted()) {
                        return;
                    }
                    setText(getString(R.string.publish_unmined_transaction));
                    try {
                        constants.wallet.publishUnminedTransactions();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (isInterrupted()) {
                        return;
                    }
                    Intent i = new Intent(SplashScreen.this, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                    //Finish all the activities before this
                    ActivityCompat.finishAffinity(SplashScreen.this);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }};
        loadThread.start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 2){
            load();
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