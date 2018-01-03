package com.decrediton.Activities;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.decrediton.R;
import com.decrediton.Util.DcrResponse;
import com.decrediton.Util.Utils;

import dcrwallet.Dcrwallet;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

/**
 * Created by Macsleven on 28/12/2017.
 */

public class AddAccountActivity extends AppCompatActivity {
    ProgressDialog pd;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Add Account");
        setContentView(R.layout.add_account_activity);

        final EditText passphrase =findViewById(R.id.add_acc_passphrase);
        final EditText accountName =findViewById(R.id.add_acc_name);

        pd = Utils.getProgressDialog(this, false, false,"Creating Account...");
        findViewById(R.id.add_acc_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String privatePhrase = passphrase.getText().toString();
                final String name = accountName.getText().toString().trim();
                if(name.equals("")){
                    Toast.makeText(AddAccountActivity.this, "Enter a name for your account", Toast.LENGTH_SHORT).show();
                }else if(privatePhrase.equals("")){
                    Toast.makeText(AddAccountActivity.this,"Enter your private passphrase", Toast.LENGTH_SHORT).show();
                }else{
                    pd.show();
                    new Thread(){
                        public void run(){
                            try{
                                DcrResponse response = DcrResponse.parse(Dcrwallet.createAccount(name, privatePhrase));
                                addAccountCallback(response.errorOccurred);
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    }.start();
                }
            }
        });
    }

    private void addAccountCallback(final boolean error){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(pd.isShowing()){
                    pd.dismiss();
                }
                if(error){
                    Toast.makeText(AddAccountActivity.this, "Error occurred while creating account", Toast.LENGTH_SHORT).show();
                    setResult(1);
                    finish();
                }else{
                    setResult(0);
                    finish();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
