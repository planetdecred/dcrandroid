package com.dcrandroid.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dcrandroid.R;
import com.dcrandroid.data.Constants;
import com.dcrandroid.util.DcrConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Macsleven on 25/12/2017.
 */

public class ConfirmSeedActivity extends AppCompatActivity {
    private List<String> seeds;
    private Set<String> tempSeeds;
    private TextView confirmView;
    private AutoCompleteTextView seedTv;
    private String seed = "";
    private ArrayAdapter<String> adapter;
    private Boolean restore;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.confirm_seed_page);
        ImageView buttonSeedClear = findViewById(R.id.button_clear_seed);
        Button btnConfirmSeed = findViewById(R.id.button_confirm_seed);
        Button btnDeleteSeed = findViewById(R.id.button_delete_seed);
        seedTv = findViewById(R.id.autoCompleteSeed);
        seedTv.setSingleLine(true);
        seedTv.setCompletionHint(getString(R.string.tap_to_select));

        btnConfirmSeed.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                String enteredSeed = "";
                Intent i = new Intent(ConfirmSeedActivity.this, EncryptWallet.class)
                        .putExtra(Constants.SEED, enteredSeed);
                startActivity(i);
                return true;
            }
        });
        btnConfirmSeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String enteredSeed = confirmView.getText().toString().trim();
                if (!restore) {
                    if (seed.equals(enteredSeed)) {
                        Intent i = new Intent(ConfirmSeedActivity.this, EncryptWallet.class)
                                .putExtra(Constants.SEED, enteredSeed);
                        startActivity(i);
                    } else {
                        Toast.makeText(ConfirmSeedActivity.this, R.string.incorrect_seed_input, Toast.LENGTH_LONG).show();
                    }
                } else {
                    DcrConstants constants = DcrConstants.getInstance();
                    if (constants.wallet.verifySeed(enteredSeed)) {
                        Intent i = new Intent(ConfirmSeedActivity.this, EncryptWallet.class)
                                .putExtra(Constants.SEED, enteredSeed);
                        startActivity(i);
                    } else {
                        Toast.makeText(ConfirmSeedActivity.this, R.string.incorrect_seed_input, Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
        buttonSeedClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmView.setText("");
            }
        });
        btnDeleteSeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String[] enteredSeed = confirmView.getText().toString().trim().split(" ");
                ArrayList<String> temp = new ArrayList<>();
                temp.addAll(Arrays.asList(enteredSeed));
                if(temp.size() > 0){
                    //remove last seed
                    temp.remove(temp.size() - 1);
                    StringBuilder sb = new StringBuilder();
                    for(int i = 0; i < temp.size(); i++){
                        sb.append(" ");
                        sb.append(temp.get(i));
                    }
                    confirmView.setText(sb.toString().trim());
                }else{
                    confirmView.setText("");
                }
            }
        });
        confirmView = findViewById(R.id.seed_display_confirm);
        seedTv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String s = (String)adapterView.getItemAtPosition(i);
                confirmView.setText(String.format("%s %s", confirmView.getText().toString().trim(), s));
                seedTv.setText("");
            }
        });

        prepareData();
    }

    private void prepareData() {
        Intent i = getIntent();
        Bundle b = i.getExtras();
        if(b != null) {
            seed = b.getString(Constants.SEED).trim();
            restore = b.getBoolean(Constants.RESTORE);
            seeds = new ArrayList<>();
            String[] seedsArray = seed.split(" ");
            tempSeeds = new HashSet<>(Arrays.asList(seedsArray));
            List<String> list = new ArrayList<>(tempSeeds);
            seeds.addAll(Arrays.asList(seedsArray));
            if(restore){
                Collections.sort(seeds, new SortIgnoreCase());
            }else{
                Collections.shuffle(seeds);
            }
            adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, list);
            seedTv.setAdapter(adapter);
        }else{
            Toast.makeText(this, R.string.error_bundle_null, Toast.LENGTH_SHORT).show();
        }
    }

    public class SortIgnoreCase implements Comparator<String>{

        @Override
        public int compare(String s, String t1) {
            return s.toLowerCase().compareTo(t1.toLowerCase());
        }
    }
}


