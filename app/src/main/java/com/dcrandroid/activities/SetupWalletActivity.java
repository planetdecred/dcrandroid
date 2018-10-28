package com.dcrandroid.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.dcrandroid.BuildConfig;
import com.dcrandroid.R;
import com.dcrandroid.data.Constants;
import com.dcrandroid.util.Utils;



/**
 * Created by Macsleven on 25/12/2017.
 */

public class SetupWalletActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_page);
        TextView buildDate= findViewById(R.id.build_date);
        Button createWalletBtn = findViewById(R.id.button_create_wallet);
        Button retrieveWalletBtn = findViewById(R.id.button_retrieve_wallet);

        buildDate.setText(BuildConfig.VERSION_NAME);

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
                        .putExtra(Constants.SEED, Utils.getWordList(SetupWalletActivity.this))
                        .putExtra(Constants.RESTORE, true);
                startActivity(i);
            }
        });
    }
}