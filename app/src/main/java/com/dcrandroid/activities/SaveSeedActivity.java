package com.dcrandroid.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dcrandroid.util.DcrConstants;
import com.dcrandroid.R;
import com.dcrandroid.view.SeedLayout;


/**
 * Created by Macsleven on 25/12/2017.
 */

public class SaveSeedActivity extends AppCompatActivity {
    private SeedLayout saveSeedTextView;
    private Toolbar toolbar;
    private ImageView ivBackArrow;

    String seed = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_save_seed_page);
        Button saveSeedContBtn = findViewById(R.id.save_seed_btn_continue);
        saveSeedTextView = findViewById(R.id.seedLayout);
        toolbar = findViewById(R.id.toolbarSeedActivity);
        setSupportActionBar(toolbar);

        ivBackArrow = findViewById(R.id.ivBackArrow);
        ivBackArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        LayoutInflater layoutInflater = getLayoutInflater();

        DcrConstants constants = DcrConstants.getInstance();
        try {
            seed = constants.wallet.generateSeed();
            String tempSeed[] = seed.split(" ");
            for(int i = 0; i <= tempSeed.length-1; i++){
               String seed = tempSeed[i];
                View seedView = layoutInflater.inflate(R.layout.seed_layout,null,false);
                TextView seedTexView = seedView.findViewById(R.id.seedTextView);
                seedTexView.setText(seed);
                saveSeedTextView.addView(seedView);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        saveSeedContBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnCopyPhrase();
            }
        });
    }

    private void btnCopyPhrase(){
        if(!seed.equals("")) {
            Intent i = new Intent(SaveSeedActivity.this, ConfirmSeedActivity.class)
                    .putExtra("seed", seed)
                    .putExtra("restore", false);
            startActivity(i);
        }else{
            Toast.makeText(SaveSeedActivity.this, R.string.error_seed_not_generated, Toast.LENGTH_SHORT).show();
        }
    }

}
