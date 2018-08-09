package com.dcrandroid.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.dcrandroid.BuildConfig;
import com.dcrandroid.R;
import com.dcrandroid.util.Utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


/**
 * Created by Macsleven on 25/12/2017.
 */

public class SetupWalletActivity extends AppCompatActivity {
    String result;
    SimpleDateFormat formatter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_page);
        TextView buildDate= findViewById(R.id.build_date);
        Button createWalletBtn = findViewById(R.id.button_create_wallet);
        Button retrieveWalletBtn = findViewById(R.id.button_retrieve_wallet);
        formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        Date buildDated = BuildConfig.buildTime;
        result = formatter.format(buildDated);
        buildDate.setText(result);
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