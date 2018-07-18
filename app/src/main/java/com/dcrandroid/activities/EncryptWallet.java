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
import com.dcrandroid.util.DcrResponse;
import com.dcrandroid.MainActivity;
import com.dcrandroid.R;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.Utils;

import org.json.JSONException;

import mobilewallet.BlockScanResponse;
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
        final EditText passPhrase = (EditText) findViewById(R.id.passphrase);
        final EditText verifyPassPhrase = (EditText) findViewById(R.id.verifyPassphrase);
        Button encryptWallet = (Button) findViewById(R.id.button_encrypt_wallet);
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
                                show("Creating wallet...");
                                wallet.createWallet(pass, seed);
                                show("Connecting to dcrd...");
                                for(;;){
                                    try {
                                        wallet.startRPCClient(Utils.getDcrdNetworkAddress(EncryptWallet.this, (MainApplication) getApplicationContext()), "dcrwallet", "dcrwallet", Utils.getConnectionCertificate(EncryptWallet.this, (MainApplication) getApplicationContext()).getBytes());
                                        break;
                                    }catch (final Exception e){
                                        if(util.getBoolean(Constants.KEY_DEBUG_MESSAGES)) {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Toast.makeText(EncryptWallet.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                        e.printStackTrace();
                                    }
                                    try{
                                        sleep(1500);
                                    }catch (InterruptedException e){
                                        e.printStackTrace();
                                    }
                                }
                                wallet.subscribeToBlockNotifications(constants.notificationError);
                                show("Discovering addresses...");
                                wallet.discoverActiveAddresses(true, pass.getBytes());
                                show("Fetching Headers...");
                                wallet.fetchHeaders();
                                long rescanHeight = constants.wallet.fetchHeaders();
                                if (rescanHeight != -1) {
                                    util.setInt(PreferenceUtil.RESCAN_HEIGHT, (int) rescanHeight);
                                }
                                System.out.println("Rescan Height: "+rescanHeight);
                                wallet.loadActiveDataFilters();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        pd.dismiss();
                                        Intent i = new Intent(EncryptWallet.this, MainActivity.class);
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