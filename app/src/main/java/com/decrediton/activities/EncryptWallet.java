package com.decrediton.activities;

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

import com.decrediton.util.DcrResponse;
import com.decrediton.MainActivity;
import com.decrediton.R;
import com.decrediton.util.Utils;
import com.decrediton.workers.EncryptBackgroundWorker;

import org.json.JSONException;

/**
 * Created by collins on 12/26/17.
 */

public class EncryptWallet extends AppCompatActivity{
    private String seed;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_passphrase);
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
                String pass = passPhrase.getText().toString();
                if(pass.equals("")){
                    Toast.makeText(EncryptWallet.this, R.string.enter_a_passphrase, Toast.LENGTH_SHORT).show();
                    return;
                }
                if(pass.equals(verifyPassPhrase.getText().toString())){
                    ProgressDialog pd = Utils.getProgressDialog(EncryptWallet.this, false,false,"Creating Wallet...");
                    new EncryptBackgroundWorker(pd,EncryptWallet.this).execute(pass, seed);
                }else{
                    Toast.makeText(EncryptWallet.this, R.string.password_not_match,Toast.LENGTH_SHORT).show();
                }
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
}