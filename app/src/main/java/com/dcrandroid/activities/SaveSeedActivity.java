package com.dcrandroid.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.dcrandroid.R;
import com.dcrandroid.adapter.SeedAdapter;
import com.dcrandroid.data.Constants;
import com.dcrandroid.util.DcrConstants;

import java.util.Arrays;

/**
 * Created by Macsleven on 25/12/2017.
 */

public class SaveSeedActivity extends AppCompatActivity {
    String seed = "";
    private SeedAdapter seedAdapter;

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

            RecyclerView recyclerView = findViewById(R.id.seeds_recycler_view);
            seedAdapter = new SeedAdapter(Arrays.asList(tempSeed));

            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
            int noOfColumns = ((int) (dpWidth / 180) < 2) ? 2 : 3;
            recyclerView.setLayoutManager(new GridLayoutManager(this, noOfColumns));

            recyclerView.setAdapter(seedAdapter);
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
