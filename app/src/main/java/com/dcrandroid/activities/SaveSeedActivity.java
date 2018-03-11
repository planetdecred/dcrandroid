package com.dcrandroid.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.dcrandroid.workers.BackgroundWorker;
import com.dcrandroid.util.DcrResponse;
import com.dcrandroid.R;
import com.dcrandroid.util.Utils;

import org.json.JSONException;

import java.lang.reflect.Method;

import dcrwallet.Dcrwallet;

/**
 * Created by Macsleven on 25/12/2017.
 *
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
        try {
            Method method = Dcrwallet.class.getDeclaredMethod("generateSeed");
            Method callback = SaveSeedActivity.this.getClass().getDeclaredMethod("generateSeedCallback", String.class);
            ProgressDialog pd = Utils.getProgressDialog(SaveSeedActivity.this, false, false, "Generating Seed...");
            new BackgroundWorker(callback, pd, SaveSeedActivity.this, true).execute(method);
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

    public void generateSeedCallback(String responseJson){
        try {
            DcrResponse response = DcrResponse.parse(responseJson);
            if(response.errorOccurred){
                Toast.makeText(this, getString(R.string.error_occurred)+response.content, Toast.LENGTH_LONG).show();
            }else{
                seed = response.content.trim();
                saveSeedTextView.setText(seed);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
