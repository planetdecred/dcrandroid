/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dcrandroid.BuildConfig;
import com.dcrandroid.R;
import com.dcrandroid.data.Constants;
import com.dcrandroid.dialog.ChooseWalletDirDialog;
import com.dcrandroid.util.Utils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;


/**
 * Created by Macsleven on 25/12/2017.
 */

public class SetupWalletActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {

    private AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.7F);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS |
                    View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        setContentView(R.layout.activity_setup_page);
        TextView buildDate = findViewById(R.id.build_date);
        RelativeLayout createWalletLl = findViewById(R.id.button_create_wallet);
        RelativeLayout retrieveWalletLl = findViewById(R.id.button_retrieve_wallet);
        ImageButton settingsPopup = findViewById(R.id.setup_menu_popup);
        buildDate.setText(BuildConfig.VERSION_NAME);

        createWalletLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.startAnimation(buttonClick);
                Intent i = new Intent(SetupWalletActivity.this, SaveSeedActivity.class);
                startActivity(i);
            }
        });

        retrieveWalletLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.startAnimation(buttonClick);
                Intent i = new Intent(SetupWalletActivity.this, ConfirmSeedActivity.class)
                        .putExtra(Constants.SEED, Utils.getWordList(SetupWalletActivity.this))
                        .putExtra(Constants.RESTORE, true);
                startActivity(i);
            }
        });

        settingsPopup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPopup(view);
            }
        });
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.change_wallet_menu_setting:
                ChooseWalletDirDialog.INSTANCE.diplayDialogue(SetupWalletActivity.this);
                return true;
            default:
                return false;
        }
    }

    public void showPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.setOnMenuItemClickListener(SetupWalletActivity.this);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.statup_menu, popup.getMenu());
        popup.show();
    }

}