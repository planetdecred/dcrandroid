/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.dcrandroid.BuildConfig
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.util.PreferenceUtil
import com.dcrandroid.util.Utils

class SetupWalletActivity : AppCompatActivity() {

    private val buttonClick = AlphaAnimation(1f, 0.7f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val decorView = window.decorView
            decorView.systemUiVisibility = WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }

        setContentView(R.layout.activity_setup_page)
        val createWalletLl = findViewById<RelativeLayout>(R.id.button_create_wallet)
        val retrieveWalletLl = findViewById<RelativeLayout>(R.id.button_retrieve_wallet)

        findViewById<TextView>(R.id.build_date).text = BuildConfig.VERSION_NAME // set build date text

        val preferenceUtil = PreferenceUtil(this)

        createWalletLl.setOnClickListener {
            it.startAnimation(buttonClick)

            preferenceUtil.setBoolean(Constants.RESTORE_WALLET, false)

            val i = Intent(this@SetupWalletActivity, SaveSeedActivity::class.java)
            startActivity(i)
        }

        retrieveWalletLl.setOnClickListener {
            it.startAnimation(buttonClick)

            preferenceUtil.setBoolean(Constants.RESTORE_WALLET, true)

            val i = Intent(this@SetupWalletActivity, ConfirmSeedActivity::class.java)
                    .putExtra(Constants.SEED, Utils.getWordList(this@SetupWalletActivity))
            startActivity(i)
        }
    }
}