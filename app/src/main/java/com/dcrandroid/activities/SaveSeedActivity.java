/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.dcrandroid.R;
import com.dcrandroid.adapter.SeedAdapter;
import com.dcrandroid.data.Constants;
import com.dcrandroid.util.WalletData;

import java.util.ArrayList;
import java.util.Arrays;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

        WalletData constants = WalletData.getInstance();

        DisplayMetrics ds = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(ds);
        int height = ds.heightPixels;
        Log.d("saveSeed", "display height: " + height);
        if (height > 2000) {

            TextView header = findViewById(R.id.header);
            ViewGroup.MarginLayoutParams headerParams = (ViewGroup.MarginLayoutParams) header.getLayoutParams();
            headerParams.setMargins(0, 50, 0, 0);
            header.setLayoutParams(headerParams);

            TextView subHeader = findViewById(R.id.subheader);
            ViewGroup.MarginLayoutParams subHeaderParams = (ViewGroup.MarginLayoutParams) subHeader.getLayoutParams();
            subHeaderParams.setMargins(0, 50, 0, 0);
            subHeader.setLayoutParams(subHeaderParams);

            NestedScrollView nestedScrollView = findViewById(R.id.nestedScrollView);
            ViewGroup.MarginLayoutParams scrollViewParams = (ViewGroup.MarginLayoutParams) nestedScrollView.getLayoutParams();
            scrollViewParams.setMargins(0, 120, 0, 0);
            nestedScrollView.setLayoutParams(scrollViewParams);
        }

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
            firstRecyclerView.setNestedScrollingEnabled(false);
            secondRecyclerView.setNestedScrollingEnabled(false);
            thirdRecyclerView.setNestedScrollingEnabled(false);

            SeedAdapter firstSeedAdapter = new SeedAdapter(firstArray, firstSeedsPosition);
            SeedAdapter secondSeedAdapter = new SeedAdapter(secondArray, secondSeedsPosition);
            SeedAdapter thirdSeedAdapter = new SeedAdapter(thirdArray, thirdSeedsPosition);

            firstRecyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
            secondRecyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
            thirdRecyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));

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
