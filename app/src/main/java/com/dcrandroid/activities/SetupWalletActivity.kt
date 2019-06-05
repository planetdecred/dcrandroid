/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.StatFs
import android.view.View
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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

        checkStorageSpace()
    }

    private fun checkStorageSpace() {
        val currentTime = System.currentTimeMillis() / 1000 // Divided by 1000 to convert to unix timestamp
        val estimatedBlocksSinceGenesis = (currentTime - BuildConfig.GenesisTimestamp) / BuildConfig.TargetTimePerBlock

        val estimatedHeadersSize = estimatedBlocksSinceGenesis / 1000 // estimate of block headers(since genesis) size in mb
        val freeInternalMemory = getFreeMemory()

        if (estimatedHeadersSize > freeInternalMemory) {
            val message = getString(R.string.low_storage_message, estimatedHeadersSize, freeInternalMemory)

            AlertDialog.Builder(this)
                    .setTitle(R.string.low_storage_space)
                    .setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok, null)
                    .setNeutralButton(R.string.exit_cap) { _, _ ->
                        ActivityCompat.finishAffinity(this)
                    }
                    .show()
        }
    }

    // returns free internal memory(in mb)
    private fun getFreeMemory(): Long {
        val statsFs = StatFs(filesDir.absolutePath)
        val blocks = statsFs.availableBlocks
        val blockSize = statsFs.blockSize

        return (blocks * blockSize) / 1048576L // convert to megabytes
    }
}