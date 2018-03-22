package com.dcrandroid.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.dcrandroid.util.DcrConstants;
import com.dcrandroid.R;


/**
 * Created by Macsleven on 25/12/2017.
 */

public class SaveSeedActivity extends AppCompatActivity {
    private TextView saveSeedTextView;
    private String seed = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_seed_page);
        Button saveSeedContBtn = (Button)findViewById(R.id.save_seed_btn_continue);
        saveSeedTextView = (TextView) findViewById(R.id.save_seed_text_view);
        DcrConstants constants = DcrConstants.getInstance();
        try {
            seed = constants.wallet.generateSeed();
            saveSeedTextView.setText(seed);
        }catch (Exception e){
            e.printStackTrace();
        }
        saveSeedContBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!seed.equals("")) {
                    Intent i = new Intent(SaveSeedActivity.this, ConfirmSeedActivity.class)
                            .putExtra("seed", seed)
                            .putExtra("restore", false);
                    startActivity(i);
                }else{
                    Toast.makeText(SaveSeedActivity.this, R.string.error_seed_not_generated, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

}
