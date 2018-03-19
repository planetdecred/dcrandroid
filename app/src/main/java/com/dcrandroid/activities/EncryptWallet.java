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

import com.dcrandroid.util.DcrConstants;
import com.dcrandroid.util.DcrResponse;
import com.dcrandroid.MainActivity;
import com.dcrandroid.R;
import com.dcrandroid.util.Utils;
import com.dcrandroid.workers.EncryptBackgroundWorker;

import org.json.JSONException;

import mobilewallet.BlockScanResponse;
import mobilewallet.LibWallet;

/**
 * Created by collins on 12/26/17.
 */

public class EncryptWallet extends AppCompatActivity implements BlockScanResponse {
    private String seed;
    ProgressDialog pd;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_passphrase);
        pd = Utils.getProgressDialog(EncryptWallet.this, false,false,"");
        final EditText passPhrase = (EditText) findViewById(R.id.passphrase);
        final EditText verifyPassPhrase = (EditText) findViewById(R.id.verifyPassphrase);
        Button encryptWallet = (Button) findViewById(R.id.button_encrypt_wallet);
        Intent i = getIntent();
        Bundle b = i.getExtras();
        if(b != null)
            seed = b.getString("seed");
        System.out.println("Encrypt Seed: "+seed);
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
//                                show("Opening wallet...");
//                                wallet.openWallet();
                                show("Connecting to dcrd...");
                                for(;;){
                                    if(wallet.startRpcClient(Utils.getDcrdNetworkAddress(EncryptWallet.this),"dcrwallet", "dcrwallet", Utils.getConnectionCertificate(EncryptWallet.this).getBytes())){
                                        break;
                                    }
                                    try{
                                        sleep(1500);
                                    }catch (InterruptedException e){
                                        e.printStackTrace();
                                    }
                                }
                                wallet.subscribeToBlockNotifications();
                                show("Discovering addresses...");
                                wallet.discoverActiveAddresses(true, pass.getBytes());
                                show("Fetching Headers...");
                                wallet.fetchHeaders();
                                wallet.loadActiveDataFilters();
                                wallet.rescan(0, EncryptWallet.this);
                            } catch (Exception e) {
                                e.printStackTrace();
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

    public void encryptWalletCallback(String responseJson){
        try {
            DcrResponse response = DcrResponse.parse(responseJson);
            if(!response.errorOccurred){
                Intent i = new Intent(this, MainActivity.class);
                startActivity(i);
                //Finish all the activities before this
                ActivityCompat.finishAffinity(this);
            }else{
                Toast.makeText(this, R.string.error_occured_creating_wallet,Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onEnd(final int height, boolean cancelled) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pd.dismiss();
                Toast.makeText(EncryptWallet.this, height + " blocks scanned", Toast.LENGTH_SHORT).show();
                Intent i = new Intent(EncryptWallet.this, MainActivity.class);
                startActivity(i);
                //Finish all the activities before this
                ActivityCompat.finishAffinity(EncryptWallet.this);
            }
        });
    }

    @Override
    public void onError(int code, String message) {
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
    }

    @Override
    public void onScan(int rescanned_through) {
        show("Scanning blocks "+rescanned_through);
    }
}