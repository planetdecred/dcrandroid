package com.dcrandroid.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.dcrandroid.R;
import com.dcrandroid.adapter.SeedAdapter;
import com.dcrandroid.data.Constants;
import com.dcrandroid.util.DcrConstants;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Macsleven on 25/12/2017.
 */

public class SaveSeedActivity extends AppCompatActivity {
    String seed = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS |
                    View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        setContentView(R.layout.activity_save_seed_page);
        Button saveSeedContBtn = findViewById(R.id.save_seed_btn_continue);

        DcrConstants constants = DcrConstants.getInstance();
        try {
            seed = constants.wallet.generateSeed();
            String tempSeed[] = seed.split(Constants.NBSP);

            ArrayList<String> firstArray = new ArrayList<>();
            ArrayList<String> secondArray = new ArrayList<>();
            ArrayList<String> thirdArray = new ArrayList<>();

            ArrayList<Integer> firstSeedsPosition = new ArrayList<>();
            ArrayList<Integer> secondSeedsPosition = new ArrayList<>();
            ArrayList<Integer> thirdSeedsPosition = new ArrayList<>();

            for (int i = 0; i < Arrays.asList(tempSeed).size(); i++) {
                if (i <= 10) {
                    firstArray.add(Arrays.asList(tempSeed).get(i));
                    firstSeedsPosition.add(i);
                } else if (i <= 21) {
                    secondArray.add(Arrays.asList(tempSeed).get(i));
                    secondSeedsPosition.add(i);
                } else {
                    thirdArray.add(Arrays.asList(tempSeed).get(i));
                    thirdSeedsPosition.add(i);
                }
            }

            RecyclerView firstRecyclerView = findViewById(R.id.seeds_recycler_view);
            RecyclerView secondRecyclerView = findViewById(R.id.seeds_recycler_view1);
            RecyclerView thirdRecyclerView = findViewById(R.id.seeds_recycler_view2);

            SeedAdapter firstSeedAdapter = new SeedAdapter(firstArray, firstSeedsPosition);
            SeedAdapter secondSeedAdapter = new SeedAdapter(secondArray, secondSeedsPosition);
            SeedAdapter thirdSeedAdapter = new SeedAdapter(thirdArray, thirdSeedsPosition);

            firstRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
            secondRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
            thirdRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

            firstRecyclerView.setAdapter(firstSeedAdapter);
            secondRecyclerView.setAdapter(secondSeedAdapter);
            thirdRecyclerView.setAdapter(thirdSeedAdapter);

        } catch (Exception e) {
            e.printStackTrace();
        }

        saveSeedContBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnCopyPhrase();
            }
        });
    }

    private void btnCopyPhrase() {
        if (!seed.equals("")) {
            Intent i = new Intent(SaveSeedActivity.this, ConfirmSeedActivity.class)
                    .putExtra(Constants.SEED, seed)
                    .putExtra(Constants.RESTORE, false);
            startActivity(i);
        } else {
            Toast.makeText(SaveSeedActivity.this, R.string.error_seed_not_generated, Toast.LENGTH_SHORT).show();
        }
    }

}
