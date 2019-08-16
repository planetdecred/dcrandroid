/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.LinearLayout;

import androidx.core.app.ActivityCompat;

import com.dcrandroid.MainActivity;
import com.dcrandroid.R;
import com.dcrandroid.data.Constants;
import com.dcrandroid.fragments.PasswordPinDialogFragment;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.WalletData;

import org.jetbrains.annotations.NotNull;

import dcrlibwallet.Dcrlibwallet;
import dcrlibwallet.LibWallet;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

/**
 * Created by Macsleven on 25/12/2017.
 */

public class SetupWalletActivity extends BaseActivity implements PasswordPinDialogFragment.PasswordPinListener {

    private PreferenceUtil preferenceUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_setup_page);

        preferenceUtil = new PreferenceUtil(this);

        LinearLayout restoreView = findViewById(R.id.ll_restore_wallet);
        LinearLayout createView = findViewById(R.id.ll_create_wallet);

        final AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.7F);

        createView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.startAnimation(buttonClick);
                showPassWordPinDialog();
            }
        });

        restoreView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.startAnimation(buttonClick);
                navigateToRestoreSeedWorkflow();
            }
        });
    }

    /**
     * Callback when the user submits spending password or pin
     *
     * @param spendingKey - either a spending password or pin
     * @param isPassword  - flag to tell whether its a password or pin
     */
    @Override
    public void onEnterPasswordOrPin(@NotNull String spendingKey, boolean isPassword) {
        if (isPassword) {
            preferenceUtil.set(Constants.SPENDING_PASSPHRASE_TYPE, Constants.PASSWORD);
        } else {
            preferenceUtil.set(Constants.SPENDING_PASSPHRASE_TYPE, Constants.PIN);
        }

        createWallet(spendingKey);
    }

    /**
     * Shows the spending pin and password bottom sheet dialog
     */
    private void showPassWordPinDialog() {
        PasswordPinDialogFragment passwordPinDialog = new PasswordPinDialogFragment();
        passwordPinDialog.setCancelable(false);
        passwordPinDialog.show(getSupportFragmentManager(), "passwordPinDialog");
    }

    /**
     * Navigates user to main activity
     *
     * @param spendingKey - spending password or pin
     */
    private void navigateToMainActivity(final String spendingKey) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SetupWalletActivity.this, MainActivity.class);
                intent.putExtra(Constants.PASSPHRASE, spendingKey);
                startActivity(intent);
                ActivityCompat.finishAffinity(SetupWalletActivity.this);
            }
        });
    }

    /**
     * Creates the wallet and navigate user to main activity
     *
     * @param spendingKey - spending password or pin
     */
    private void createWallet(final String spendingKey) {

        newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {

                    LibWallet wallet = WalletData.getInstance().wallet;
                    if (wallet == null) {
                        throw new NullPointerException(getString(R.string.create_wallet_uninitialized));
                    }

                    String seed = Dcrlibwallet.generateSeed();
                    preferenceUtil.set(Constants.SEED, seed);
                    preferenceUtil.setBoolean(Constants.VERIFIED_SEED, false);
                    preferenceUtil.setBoolean(Constants.RESTORE_WALLET, false);
                    wallet.createWallet(spendingKey, seed);
                    wallet.unlockWallet(spendingKey.getBytes());

                    navigateToMainActivity(spendingKey);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void navigateToRestoreSeedWorkflow() {
        preferenceUtil.setBoolean(Constants.RESTORE_WALLET, true);
        // TODO - hook up restore wallet workflow after implementing restore wallet UI
    }
}