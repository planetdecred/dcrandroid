/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.dcrandroid.R;
import com.dcrandroid.data.Constants;
import com.dcrandroid.dialog.BiometricDialogV23;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.Utils;
import com.dcrandroid.util.WalletData;

import java.security.Signature;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.hardware.fingerprint.FingerprintManagerCompat;

/**
 * Created by Macsleven on 28/12/2017.
 */

public class AddAccountActivity extends AppCompatActivity {

    private ProgressDialog pd;
    private PreferenceUtil util;
    private final int PASSCODE_REQUEST_CODE = 2;
    private EditText accountName, passphrase;

    private BiometricDialogV23 biometricDialogV23;

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
        if (util.get(Constants.SPENDING_PASSPHRASE_TYPE).equals(Constants.PIN)) {
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
                    if (util.get(Constants.SPENDING_PASSPHRASE_TYPE).equals(Constants.PASSWORD)) {
                        if (privatePassphrase.equals("")) {
                            passphrase.setError(getString(R.string.input_private_phrase));
                            return;
                        }
                        checkBiometric();
                    } else {
                        Intent enterPinIntent = new Intent(AddAccountActivity.this, EnterPassCode.class);
                        startActivityForResult(enterPinIntent, PASSCODE_REQUEST_CODE);
                    }
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
                createAccount(accountName.getText().toString().trim(), passcode.getBytes());
            }
        }
    }

    private void createAccount(final String name, final byte[] privatePassphrase) {
        pd.show();
        new Thread() {
            public void run() {
                try {
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

    private void checkBiometric(){
        if (!util.getBoolean(Constants.USE_BIOMETRIC, false)) {
            System.out.println("Biometric not enabled in settings");
            createAccount(accountName.getText().toString().trim(), passphrase.getText().toString().getBytes());
            return;
        }

        if (Utils.Biometric.isSupportBiometricPrompt(this)) {
            displayBiometricPrompt();
        }else if (Utils.Biometric.isSupportFingerprint(this)){
            System.out.println("Device does support biometric prompt");
            showFingerprintDialog();
        }else{
            createAccount(accountName.getText().toString().trim(), passphrase.getText().toString().getBytes());
        }
    }

    @SuppressLint("NewApi")
    private void displayBiometricPrompt(){
        try {
            Utils.Biometric.generateKeyPair(Constants.SPENDING_PASSPHRASE_TYPE, true);
            Signature signature = Utils.Biometric.initSignature(Constants.SPENDING_PASSPHRASE_TYPE);

            if (signature != null) {

                 BiometricPrompt biometricPrompt = new BiometricPrompt.Builder(this)
                        .setTitle(getString(R.string.app_name))
                        .setNegativeButton("Cancel", getMainExecutor(), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .build();

                biometricPrompt.authenticate(new BiometricPrompt.CryptoObject(signature), getBiometricCancellationSignal(), getMainExecutor(), biometricAuthenticationCallback);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showFingerprintDialog(){
        FingerprintManagerCompat fingerprintManager = FingerprintManagerCompat.from(this);
        if(fingerprintManager.hasEnrolledFingerprints()){
            try {
                Utils.Biometric.generateKeyPair(Constants.SPENDING_PASSPHRASE_TYPE, true);
                Signature signature = Utils.Biometric.initSignature(Constants.SPENDING_PASSPHRASE_TYPE);

                if (signature != null) {

                    fingerprintManager.authenticate(new FingerprintManagerCompat.CryptoObject(signature), 0,
                            getFingerprintCancellationSignal(), fingerprintAuthCallback, null);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            biometricDialogV23 = new BiometricDialogV23(AddAccountActivity.this);
                            biometricDialogV23.setTitle(getString(R.string.app_name));
                            biometricDialogV23.show();
                        }
                    });
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }else{
            createAccount(accountName.getText().toString().trim(), passphrase.getText().toString().getBytes());
        }
    }

    @SuppressLint("NewApi")
    private CancellationSignal getBiometricCancellationSignal() {
        // With this cancel signal, we can cancel biometric prompt operation
        CancellationSignal cancellationSignal = new CancellationSignal();
        cancellationSignal.setOnCancelListener(new CancellationSignal.OnCancelListener() {
            @Override
            public void onCancel() {
                System.out.println("Cancel result, signal triggered");
            }
        });

        return cancellationSignal;
    }

    @SuppressLint("NewApi")
    private androidx.core.os.CancellationSignal getFingerprintCancellationSignal() {
        // With this cancel signal, we can cancel biometric prompt operation
        androidx.core.os.CancellationSignal cancellationSignal = new androidx.core.os.CancellationSignal();
        cancellationSignal.setOnCancelListener(new androidx.core.os.CancellationSignal.OnCancelListener() {
            @Override
            public void onCancel() {
                System.out.println("Cancel result, signal triggered");
            }
        });

        return cancellationSignal;
    }

    @SuppressLint("NewApi")
    private BiometricPrompt.AuthenticationCallback biometricAuthenticationCallback = new BiometricPrompt.AuthenticationCallback() {

        @Override
        public void onAuthenticationError(int errorCode, CharSequence errString) {
            super.onAuthenticationError(errorCode, errString);
            Toast.makeText(AddAccountActivity.this, errString, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
            super.onAuthenticationSucceeded(result);
            createAccount(accountName.getText().toString().trim(), passphrase.getText().toString().getBytes());
        }
    };

    private FingerprintManagerCompat.AuthenticationCallback fingerprintAuthCallback = new FingerprintManagerCompat.AuthenticationCallback() {
        @Override
        public void onAuthenticationError(int errMsgId, CharSequence errString) {
            super.onAuthenticationError(errMsgId, errString);
            Toast.makeText(AddAccountActivity.this, errString, Toast.LENGTH_LONG).show();
            if(biometricDialogV23 != null){
                biometricDialogV23.dismiss();
            }
        }

        @Override
        public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
            super.onAuthenticationHelp(helpMsgId, helpString);
            Toast.makeText(AddAccountActivity.this, helpString, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
            super.onAuthenticationSucceeded(result);
            if(biometricDialogV23 != null){
                biometricDialogV23.dismiss();
            }

            createAccount(accountName.getText().toString().trim(), passphrase.getText().toString().getBytes());
        }

        @Override
        public void onAuthenticationFailed() {
            super.onAuthenticationFailed();
            Toast.makeText(AddAccountActivity.this, R.string.biometric_auth_failed, Toast.LENGTH_SHORT).show();
        }
    };
}
