package com.dcrandroid.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.dcrandroid.MainApplication;
import com.dcrandroid.data.Constants;
import com.dcrandroid.util.DcrConstants;
import com.dcrandroid.MainActivity;
import com.dcrandroid.R;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.Utils;

import mobilewallet.LibWallet;

/**
 * Created by collins on 12/26/17.
 */

public class EncryptWallet extends AppCompatActivity{
    private String seed;
    ProgressDialog pd;
    private PreferenceUtil util;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_passphrase);
        util = new PreferenceUtil(EncryptWallet.this);
        pd = Utils.getProgressDialog(EncryptWallet.this, false,false,"");
        final EditText passPhrase = findViewById(R.id.passphrase);
        final EditText verifyPassPhrase = findViewById(R.id.verifyPassphrase);
        Button encryptWallet = findViewById(R.id.button_encrypt_wallet);
        Intent i = getIntent();
        Bundle b = i.getExtras();
        if(b != null)
            seed = b.getString("seed");
            encryptWallet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String pass = passPhrase.getText().toString();
                if(pass.equals("")){
                    Toast.makeText(EncryptWallet.this, R.string.enter_a_passphrase, Toast.LENGTH_SHORT).show();
                    return;
                }
                if(pass.equals(verifyPassPhrase.getText().toString())){
                    new Thread(){
                        public void run(){
                            try {
                                DcrConstants constants = DcrConstants.getInstance();
                                LibWallet wallet = constants.wallet;
                                if (wallet == null){
                                    return;
                                }
                                show("Creating wallet...");
                                wallet.createWallet(pass, seed);
                                //constants.wallet.unlockWallet(pass.getBytes());
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        pd.dismiss();
                                        Intent i = new Intent(EncryptWallet.this, MainActivity.class);
                                        i.putExtra("passphrase", pass);
                                        startActivity(i);
                                        //Finish all the activities before this
                                        ActivityCompat.finishAffinity(EncryptWallet.this);
                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                                //Todo: Handle Error here
                            }
                        }
                    }.start();
                }else{
                    Toast.makeText(EncryptWallet.this, R.string.password_not_match,Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void show(final String str){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pd.setMessage(str);
                pd.show();
            }
        });
    }
}