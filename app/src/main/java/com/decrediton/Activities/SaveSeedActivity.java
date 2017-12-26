package com.decrediton.Activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.decrediton.workers.BackgroundWorker;
import com.decrediton.DcrResponse;
import com.decrediton.R;
import com.decrediton.Util.Utils;

import org.json.JSONException;

import java.lang.reflect.Method;

import dcrwallet.Dcrwallet;

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
                    Intent i = new Intent(SaveSeedActivity.this, ConfrimSeedActivity.class)
                            .putExtra("seed", seed);
                    startActivity(i);
                }else{
                    Toast.makeText(SaveSeedActivity.this, "Error occurred, Seed was not generated", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void generateSeedCallback(String responseJson){
        try {
            DcrResponse response = DcrResponse.parse(responseJson);
            if(response.errorOccurred){
                Toast.makeText(this, "Error Occurred: "+response.content, Toast.LENGTH_LONG).show();
            }else{
                seed = response.content.trim();
                saveSeedTextView.setText(seed);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
