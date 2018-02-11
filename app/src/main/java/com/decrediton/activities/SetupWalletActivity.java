package com.decrediton.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.decrediton.R;
import com.decrediton.util.Utils;

import dcrwallet.Dcrwallet;

/**
 * Created by Macsleven on 25/12/2017.
 */

public class SetupWalletActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_page);
        Button createWalletBtn = (Button)findViewById(R.id.button_create_wallet);
        Button retrieveWalletBtn = (Button) findViewById(R.id.button_retrieve_wallet);
        createWalletBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                 Intent i = new Intent(SetupWalletActivity.this, SaveSeedActivity.class);
                 startActivity(i);
            }
        });
        retrieveWalletBtn.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent i = new Intent(SetupWalletActivity.this, ConfirmSeedActivity.class)
                    .putExtra("seed", Utils.getWordList(SetupWalletActivity.this))
                    .putExtra("restore", true);
            startActivity(i);
        }
    });
}
    @Override
    public void onBackPressed() {
    }
}