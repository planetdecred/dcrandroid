package com.decrediton.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.decrediton.Adapter.SeedAdapter;
import com.decrediton.DcrResponse;
import com.decrediton.data.Seed;
import com.decrediton.R;
import com.decrediton.Util.RecyclerTouchListener;
import com.decrediton.workers.VerifySeedBackgroundWorker;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Macsleven on 25/12/2017.
 */

public class ConfirmSeedActivity extends AppCompatActivity {
    private List<String> seeds;
    private TextView confirmview;
    private AutoCompleteTextView seedTv;
    private String seed = "";
    private ArrayAdapter<String> adapter;
    private Boolean restore;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.confirm_seed_page);
        Button buttonSeedClear =(Button)findViewById(R.id.button_clear_seed);
        Button btnConfirmSeed = (Button) findViewById(R.id.button_confirm_seed);
        Button btnDeleteSeed = (Button) findViewById(R.id.button_delete_seed);
        seedTv = (AutoCompleteTextView) findViewById(R.id.autoCompleteSeed);
        btnConfirmSeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String enteredSeed = confirmview.getText().toString().trim();
                if(!restore){
                    System.out.println("Seed: "+seed);
                    System.out.println("Entered Seed: "+enteredSeed);
                    if(seed.equals(enteredSeed)){
                        Intent i = new Intent(ConfirmSeedActivity.this, EncryptWallet.class)
                                .putExtra("seed",seed);
                        startActivity(i);
                    }else{
                        Toast.makeText(ConfirmSeedActivity.this, "You entered an incorrect seed, Please confirm your seed", Toast.LENGTH_LONG).show();
                    }
                }else{
                    System.out.println("Verifying: "+enteredSeed);
                    new VerifySeedBackgroundWorker(ConfirmSeedActivity.this).execute(enteredSeed);
                    Toast.makeText(ConfirmSeedActivity.this, "Not restoring", Toast.LENGTH_SHORT).show();
                }
            }
        });
        buttonSeedClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmview.setText("");
            }
        });
        btnDeleteSeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String[] enteredSeed = confirmview.getText().toString().trim().split(" ");
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
                    confirmview.setText(sb.toString().trim());
                }else{
                    confirmview.setText("");
                }
            }
        });
        confirmview =(TextView)findViewById(R.id.seed_display_confirm);
        seedTv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String s = (String)adapterView.getItemAtPosition(i);
                confirmview.append(" "+s);
                seedTv.setText("");
            }
        });
        prepareData();
    }

    public void verifySeedCallback(String responseJson){
        try {
            DcrResponse response = DcrResponse.parse(responseJson);
            if(response.errorOccurred){
                Toast.makeText(this, response.content, Toast.LENGTH_LONG).show();
            }else{
                String enteredSeed = confirmview.getText().toString().trim();
                System.out.println("Seed: "+seed);
                System.out.println("Confirm Seed: "+enteredSeed);
                Intent i = new Intent(ConfirmSeedActivity.this, EncryptWallet.class)
                        .putExtra("seed",enteredSeed);
                startActivity(i);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void prepareData() {
        Intent i = getIntent();
        Bundle b = i.getExtras();
        if(b != null) {
            seed = b.getString("seed").trim();
            restore = b.getBoolean("restore");
            seeds = new ArrayList<>();
            String[] seedsArray = seed.split(" ");
            seeds.addAll(Arrays.asList(seedsArray));
            if(restore){
                Collections.sort(seeds, new SortIgnoreCase());
            }else{
                Collections.shuffle(seeds);
            }
            adapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_dropdown_item_1line, seeds);
            seedTv.setAdapter(adapter);
        }else{
            Toast.makeText(this, "Error occurred, Bundle is null", Toast.LENGTH_SHORT).show();
        }
    }

    public interface ClickListener {
        void onClick(View view, int position);

        void onLongClick(View view, int position);
    }
    public class SortIgnoreCase implements Comparator<String>{

        @Override
        public int compare(String s, String t1) {
            return s.toLowerCase().compareTo(t1.toLowerCase());
        }
    }
}
