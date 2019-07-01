/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricConstants;
import androidx.biometric.BiometricPrompt;

import com.dcrandroid.R;
import com.dcrandroid.data.Constants;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.Utils;
import com.dcrandroid.util.WalletData;

/**
 * Created by Macsleven on 28/12/2017.
 */

public class AddAccountActivity extends AppCompatActivity {

    private ProgressDialog pd;
    private PreferenceUtil util;
    private final int PASSCODE_REQUEST_CODE = 2;
    private EditText accountName, passphrase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS |
                    View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        setTitle(getString(R.string.add_account));
        setContentView(R.layout.add_account_activity);

        accountName = findViewById(R.id.add_acc_name);
        passphrase = findViewById(R.id.add_acc_passphrase);

        util = new PreferenceUtil(this);

        final String biometricOption = util.get(Constants.USE_BIOMETRIC);
        if (util.get(Constants.SPENDING_PASSPHRASE_TYPE).equals(Constants.PIN) || biometricOption.equals(Constants.FINGERPRINT)) {
            passphrase.setVisibility(View.GONE);
        }


        pd = Utils.getProgressDialog(this, false, false, getString(R.string.creating_account));
        findViewById(R.id.add_acc_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String privatePassphrase = passphrase.getText().toString();
                final String name = accountName.getText().toString().trim();
                if (name.equals("")) {
                    accountName.setError(getString(R.string.input_account_name));
                } else {

                    if (util.get(Constants.SPENDING_PASSPHRASE_TYPE).equals(Constants.PASSWORD) && !biometricOption.equals(Constants.FINGERPRINT)) {
                        if (privatePassphrase.equals("")) {
                            passphrase.setError(getString(R.string.input_private_phrase));
                            return;
                        }
                    }

                    checkBiometric();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PASSCODE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                final String passcode = data.getStringExtra(Constants.PASSPHRASE);
                createAccount(passcode.getBytes());
            }
        }
    }

    private void createAccount(final byte[] privatePassphrase) {
        pd.show();
        new Thread() {
            public void run() {
                try {
                    String name = accountName.getText().toString().trim();
                    WalletData.getInstance().wallet.nextAccount(name, privatePassphrase);
                    setResult(RESULT_OK);
                    finish();
                } catch (final Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Utils.showMessage(AddAccountActivity.this, Utils.translateError(AddAccountActivity.this, e), Toast.LENGTH_LONG);
                        }
                    });
                    setResult(RESULT_CANCELED);
                }
                if (pd.isShowing()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pd.dismiss();
                        }
                    });
                }
            }
        }.start();
    }

    private void promptPass() {
        if (util.get(Constants.SPENDING_PASSPHRASE_TYPE).equals(Constants.PASSWORD)) {
            createAccount(passphrase.getText().toString().getBytes());
        } else {
            Intent enterPinIntent = new Intent(AddAccountActivity.this, EnterPassCode.class);
            startActivityForResult(enterPinIntent, PASSCODE_REQUEST_CODE);
        }
    }

    private void checkBiometric() {
        String biometricOption = util.get(Constants.USE_BIOMETRIC);
        if (biometricOption.equals(Constants.OFF)) {

            System.out.println("Biometric not enabled in settings");

            // proceed to ask for/get password/pin since biometric option
            // isn't enabled
            promptPass();

            return;
        }

        if (!Utils.Biometric.displayBiometricPrompt(this, authenticationCallback)) {
            Utils.showMessage(this, getString(R.string.no_fingerprint_error), Toast.LENGTH_LONG);
        }
    }

    private BiometricPrompt.AuthenticationCallback authenticationCallback = new BiometricPrompt.AuthenticationCallback() {
        @Override
        public void onAuthenticationError(final int errorCode, @NonNull final CharSequence errString) {
            super.onAuthenticationError(errorCode, errString);

            System.out.println("Biometric Error Code: " + errorCode + " Error String: " + errString);

            if(errorCode == BiometricConstants.ERROR_NEGATIVE_BUTTON){
                return;
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final String message = Utils.Biometric.translateError(AddAccountActivity.this, errorCode);
                    if (message != null) {
                        Toast.makeText(AddAccountActivity.this, message, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(AddAccountActivity.this, errString, Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

        @Override
        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
            super.onAuthenticationSucceeded(result);

            String biometricOption = util.get(Constants.USE_BIOMETRIC);
            if (biometricOption.equals(Constants.FINGERPRINT)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String pass = Utils.Biometric.getPassFromKeystore(AddAccountActivity.this, Constants.SPENDING_PASSPHRASE_TYPE);
                            createAccount(pass.getBytes());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

                return;
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    promptPass();
                }
            });
        }
    };

}
