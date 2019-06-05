/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import com.dcrandroid.adapter.SaveSeedAdapter
import com.dcrandroid.data.Constants
import dcrlibwallet.Dcrlibwallet

class SaveSeedActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        setContentView(R.layout.activity_save_seed_page)

        try {
            val seed = Dcrlibwallet.generateSeed()
            val tempSeed = seed!!.split(Constants.NBSP.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val items = ArrayList<SaveSeedAdapter.SeedRow>()

            for (i in 0..30 step 3) {
                val seed1 = "${i + 1}." + tempSeed[i]
                val seed2 = "${i + 2}." + tempSeed[i + 1]
                val seed3 = "${i + 3}." + tempSeed[i + 2]
                items.add(SaveSeedAdapter.SeedRow(seed1, seed2, seed3))
            }

            val adp = SaveSeedAdapter(items)
            val recyclerView = findViewById<RecyclerView>(R.id.seeds_recycler_view)
            recyclerView.isNestedScrollingEnabled = false
            recyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
            recyclerView.adapter = adp

            findViewById<Button>(R.id.save_seed_btn_continue).setOnClickListener {
                val i = Intent(this@SaveSeedActivity, ConfirmSeedActivity::class.java)
                        .putExtra(Constants.SEED, seed)
                        .putExtra(Constants.RESTORE, false)
                startActivity(i)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}