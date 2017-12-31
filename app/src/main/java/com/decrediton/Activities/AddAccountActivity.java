package com.decrediton.Activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.decrediton.R;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

/**
 * Created by Macsleven on 28/12/2017.
 */

public class AddAccountActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Add Account");
        setContentView(R.layout.add_account_activity);

        EditText passphrase =findViewById(R.id.add_acc_passphrase);
        EditText accountName =findViewById(R.id.add_acc_name);
        Button confirm = findViewById(R.id.add_acc_button);
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
