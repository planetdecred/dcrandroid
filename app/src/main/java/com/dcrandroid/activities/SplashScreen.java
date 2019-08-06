/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.vectordrawable.graphics.drawable.Animatable2Compat;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;

import com.dcrandroid.BuildConfig;
import com.dcrandroid.HomeActivity;
import com.dcrandroid.MainActivity;
import com.dcrandroid.R;
import com.dcrandroid.data.Constants;
import com.dcrandroid.dialog.InfoDialog;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.Utils;
import com.dcrandroid.util.WalletData;

import java.io.File;

import dcrlibwallet.Dcrlibwallet;
import dcrlibwallet.LibWallet;

/**
 * Created by Macsleven on 24/12/2017.
 */

public class SplashScreen extends BaseActivity {

    private final int PASSWORD_REQUEST_CODE = 1;
    private ImageView imgAnim;
    private PreferenceUtil util;
    private TextView tvLoading;
    private Thread loadThread;
    private WalletData walletData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isTaskRoot()) {
            final Intent intent = getIntent();
            final String intentAction = intent.getAction();
            if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && intentAction != null && intentAction.equals(Intent.ACTION_MAIN)) {
                finish();
                return;
            }
        }

        util = new PreferenceUtil(this);
        setContentView(R.layout.splash_page);

        if (BuildConfig.IS_TESTNET) {
            findViewById(R.id.tv_testnet).setVisibility(View.VISIBLE);
        }

        imgAnim = findViewById(R.id.splashscreen_icon);
        imgAnim.setOnClickListener(new DoubleClickListener() {
            @Override
            public void onSingleClick(View v) {

            }

            @Override
            public void onDoubleClick(View v) {
                if (loadThread != null) {
                    loadThread.interrupt();
                }

                if (walletData.wallet != null) {
                    walletData.wallet.shutdown();
                }

                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivityForResult(intent, 2);
            }
        });

        imgAnim.post(new Runnable() {
            @Override
            public void run() {

                final AnimatedVectorDrawableCompat anim = AnimatedVectorDrawableCompat.create(getApplicationContext(), R.drawable.avd_anim);
                anim.registerAnimationCallback(new Animatable2Compat.AnimationCallback() {
                    @Override
                    public void onAnimationEnd(Drawable drawable) {
                        super.onAnimationEnd(drawable);
                        imgAnim.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                anim.start();
                            }
                        }, 750);
                    }
                });
                imgAnim.setImageDrawable(anim);
                anim.start();
            }
        });

        tvLoading = findViewById(R.id.loading_status);
        startup();
    }

    private void startup() {
        walletData = WalletData.getInstance();

        try {
            if (walletData.wallet != null && walletData.wallet.walletOpened()) {
                walletData.wallet.shutdown();
            }

            walletData.wallet = null;
        } catch (Exception e) {
            e.printStackTrace();
        }

        String homeDir = getFilesDir() + "/wallet";
        walletData.wallet = new LibWallet(homeDir, Constants.BADGER_DB, BuildConfig.NetType);
        Dcrlibwallet.setLogLevels(util.get(Constants.LOGGING_LEVEL));

        String walletDB;

        if (BuildConfig.IS_TESTNET) {
            walletDB = "/testnet3/wallet.db";
        } else {
            walletDB = "/mainnet/wallet.db";
        }

        File f = new File(homeDir, walletDB);
        if (!f.exists()) {
            loadThread = new Thread() {
                public void run() {
                    try {
                        sleep(3000);
                        createWallet();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            loadThread.start();
        } else {
            checkEncryption();
        }
    }

    private void setText(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvLoading.setText(str);
            }
        });
    }

    private void createWallet() {
        Intent i = new Intent(SplashScreen.this, SetupWalletActivity.class);
        startActivity(i);
        finish();
    }

    private void openWallet(final String publicPass) {
        loadThread = new Thread() {
            public void run() {
                try {
                    setText(getString(R.string.opening_wallet));
                    walletData.wallet.openWallet(publicPass.getBytes());
                    Intent i = new Intent(SplashScreen.this, HomeActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                    //Finish all the activities before this
                    ActivityCompat.finishAffinity(SplashScreen.this);
                } catch (final Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            InfoDialog infoDialog = new InfoDialog(SplashScreen.this)
                                    .setDialogTitle(getString(R.string.failed_to_open_wallet))
                                    .setMessage(Utils.translateError(SplashScreen.this, e))
                                    .setIcon(R.drawable.np_amount_withdrawal) //Temporary Icon
                                    .setPositiveButton(getString(R.string.exit_cap), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            finish();
                                        }
                                    });

                            if (e.getMessage().equals(Dcrlibwallet.ErrInvalidPassphrase)) {
                                if (util.get(Constants.STARTUP_PASSPHRASE_TYPE).equals(Constants.PIN)) {
                                    infoDialog.setMessage(getString(R.string.invalid_pin));
                                }
                                infoDialog.setNegativeButton(getString(R.string.exit_cap), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                    }
                                }).setPositiveButton(getString(R.string.retry_caps), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent i;

                                        if (util.get(Constants.STARTUP_PASSPHRASE_TYPE).equals(Constants.PASSWORD)) {
                                            i = new Intent(SplashScreen.this, EnterPasswordActivity.class);
                                        } else {
                                            i = new Intent(SplashScreen.this, EnterPassCode.class);
                                        }

                                        i.putExtra(Constants.SPENDING_PASSWORD, false);
                                        i.putExtra(Constants.NO_RETURN, true);
                                        startActivityForResult(i, PASSWORD_REQUEST_CODE);
                                    }
                                });
                            }

                            infoDialog.setCancelable(false);
                            infoDialog.setCanceledOnTouchOutside(false);
                            infoDialog.show();

                        }
                    });
                }
            }
        };
        loadThread.start();
    }

    public void checkEncryption() {
        if (util.getBoolean(Constants.ENCRYPT)) {
            Intent i;

            if (util.get(Constants.STARTUP_PASSPHRASE_TYPE).equals(Constants.PASSWORD)) {
                i = new Intent(this, EnterPasswordActivity.class);
            } else {
                i = new Intent(this, EnterPassCode.class);
            }

            i.putExtra(Constants.SPENDING_PASSWORD, false);
            i.putExtra(Constants.NO_RETURN, true);
            startActivityForResult(i, PASSWORD_REQUEST_CODE);
        } else {
            openWallet(Constants.INSECURE_PUB_PASSPHRASE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2) {
            startup();
        } else if (requestCode == PASSWORD_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                openWallet(data.getStringExtra(Constants.PASSPHRASE));
            }
        }
    }

    public abstract class DoubleClickListener implements View.OnClickListener {

        private static final long DOUBLE_CLICK_TIME_DELTA = 300;//milliseconds

        long lastClickTime = 0;

        @Override
        public void onClick(View v) {
            long clickTime = System.currentTimeMillis();
            if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
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