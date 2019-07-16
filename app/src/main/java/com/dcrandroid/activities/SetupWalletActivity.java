/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.LinearLayout;

import com.dcrandroid.R;
import com.dcrandroid.data.Constants;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.Utils;

/**
 * Created by Macsleven on 25/12/2017.
 */

public class SetupWalletActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_setup_page);
        LinearLayout restoreView = findViewById(R.id.ll_restore_wallet);
        LinearLayout createView = findViewById(R.id.ll_create_wallet);

        final PreferenceUtil preferenceUtil = new PreferenceUtil(this);
        final AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.7F);

        createView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.startAnimation(buttonClick);
                preferenceUtil.setBoolean(Constants.RESTORE_WALLET, false);
                Intent i = new Intent(SetupWalletActivity.this, SaveSeedActivity.class);
                startActivity(i);
            }
        });

        restoreView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.startAnimation(buttonClick);
                preferenceUtil.setBoolean(Constants.RESTORE_WALLET, true);
                Intent i = new Intent(SetupWalletActivity.this, ConfirmSeedActivity.class)
                        .putExtra(Constants.SEED, Utils.getWordList(SetupWalletActivity.this))
                        .putExtra(Constants.RESTORE, true);
                startActivity(i);
            }
        });
    }
}