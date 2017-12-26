package com.decrediton.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.decrediton.R;

/**
 * Created by Macsleven on 25/12/2017.
 */

public class SaveSeedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_seed_page);
        Button saveSeedContBtn = (Button)findViewById(R.id.save_seed_btn_continue);
        TextView saveSeedTextView = (TextView) findViewById(R.id.save_seed_text_view);
        saveSeedContBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(SaveSeedActivity.this, ConfrimSeedActivity.class);
                startActivity(i);
            }
        });
    }
}
