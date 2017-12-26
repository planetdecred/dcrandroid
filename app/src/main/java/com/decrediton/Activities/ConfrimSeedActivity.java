package com.decrediton.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.decrediton.Adapter.SeedAdapter;
import com.decrediton.data.Seed;
import com.decrediton.MainActivity;
import com.decrediton.R;
import com.decrediton.Util.RecyclerTouchListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Macsleven on 25/12/2017.
 */

public class ConfrimSeedActivity extends AppCompatActivity {
    private List<Seed> seedList = new ArrayList<>();
    private List<Seed> confirmSeedList = new ArrayList<>();

    private SeedAdapter seedAdapter;
    private TextView confirmview;

    private String seed = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.confirm_seed_page);
        Button buttonSeedClear =(Button)findViewById(R.id.button_clear_seed);
        Button btnConfirmSeed = (Button) findViewById(R.id.button_confirm_seed);
        btnConfirmSeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String enteredSeed = confirmview.getText().toString().trim();
                if(!seed.equals(enteredSeed)){
                    Intent i = new Intent(ConfrimSeedActivity.this, EncryptWallet.class)
                            .putExtra("seed",seed);
                    startActivity(i);
                }else{
                    Toast.makeText(ConfrimSeedActivity.this, "You entered an incorrect seed, Please confirm your seed", Toast.LENGTH_LONG).show();
                }
            }
        });
        buttonSeedClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!confirmSeedList.isEmpty()) {
                    seedList.addAll(confirmSeedList);
                    confirmSeedList.clear();
                    seedAdapter.notifyDataSetChanged();
                    confirmview.setText("");
                }
            }
        });
        confirmview =(TextView)findViewById(R.id.seed_display_confirm);
        //Setup List
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        seedAdapter = new SeedAdapter(seedList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));

        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Seed seed = seedList.get(position);
                Toast.makeText(getApplicationContext(),confirmSeedList.size()+"",Toast.LENGTH_SHORT).show();
                confirmSeedList.add(seed);
                seedList.remove(position);
                seedAdapter.notifyDataSetChanged();
                confirmview.append(seed.getSeed()+" ");
            }
            @Override
            public void onLongClick(View view, int position) {

            }
        }));
        recyclerView.setAdapter(seedAdapter);
        prepareData();
    }

    private void prepareData() {
        Intent i = getIntent();
        Bundle b = i.getExtras();
        if(b != null) {
            seed = b.getString("seed").trim();
            String[] seedsArray = seed.split(" ");
            for (int x = 0; x < seedsArray.length; x++) {
                Seed seed = new Seed(seedsArray[x]);
                seedList.add(seed);
            }
            Collections.shuffle(seedList);
            seedAdapter.notifyDataSetChanged();
        }else{
            Toast.makeText(this, "Error occurred, Bundle is null", Toast.LENGTH_SHORT).show();
        }
    }

    public interface ClickListener {
        void onClick(View view, int position);

        void onLongClick(View view, int position);
    }
}
