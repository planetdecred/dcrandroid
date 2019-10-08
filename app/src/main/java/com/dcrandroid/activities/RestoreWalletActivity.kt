/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities

import android.os.Bundle
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import com.dcrandroid.R
import com.dcrandroid.adapter.RestoreWalletAdapter
import com.dcrandroid.util.SnackBar
import dcrlibwallet.Dcrlibwallet
import kotlinx.android.synthetic.main.activity_restore_wallet.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class RestoreWalletActivity: BaseActivity() {

    var allSeedWords = ArrayList<String>()
    lateinit var adapter: RestoreWalletAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restore_wallet)

        nested_scroll_view.setOnScrollChangeListener { _: NestedScrollView?, _: Int, scrollY: Int, _: Int, _: Int ->
            app_bar.elevation = when {
                scrollY > 0 -> resources.getDimension(R.dimen.app_bar_elevation)
                else -> 0f
            }
        }

        val layoutManager = LinearLayoutManager(this@RestoreWalletActivity)
        recycler_view_seeds.layoutManager = layoutManager

        adapter = RestoreWalletAdapter(this@RestoreWalletActivity, allSeedWords)
        recycler_view_seeds.adapter = adapter

        adapter.seedChanged = { _, allValid ->
            btn_restore.isEnabled = allValid
            Unit
        }

        loadSeedSuggestions()

        btn_restore.setOnClickListener{
            val seed = adapter.enteredSeeds.joinToString(" ")
            val seedValid = Dcrlibwallet.verifySeed(seed)
            
            if(!seedValid){
                SnackBar.showError(this, R.string.invalid_restore_seed)
                return@setOnClickListener
            }
        }

        go_back.setOnClickListener { finish() }
    }


    private fun loadSeedSuggestions() = GlobalScope.launch(Dispatchers.IO){
        val seedWords = Dcrlibwallet.AlternatingWords.split("\n")
        allSeedWords.addAll(seedWords)
    }
}