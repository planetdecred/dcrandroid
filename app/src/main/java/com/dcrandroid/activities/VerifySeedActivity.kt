/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.dcrandroid.R
import com.dcrandroid.adapter.*
import com.dcrandroid.data.Constants
import com.dcrandroid.util.Utils
import dcrlibwallet.Dcrlibwallet
import kotlinx.android.synthetic.main.verify_seed_page.*

class VerifySeedActivity : BaseActivity(), SeedTapListener {

    private var seeds = ArrayList<String>()
    private lateinit var allSeeds: Array<String>

    private lateinit var createWalletAdapter: CreateWalletAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager

    private lateinit var footer: SeedPageFooter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        setContentView(R.layout.verify_seed_page)

        linearLayoutManager = LinearLayoutManager(this)
        recyclerViewSeeds.layoutManager = linearLayoutManager

        footer = SeedPageFooter(null, R.drawable.btn_shape2)

        allSeeds = Utils.getWordList(this).split(" ").toTypedArray()

        prepareData()
    }

    private fun prepareData() {
        val bundle = intent.extras
        if (bundle != null && !bundle.isEmpty) {
            val seed = bundle.getString(Constants.SEED)
            if (seed != null) {
                seeds = ArrayList(seed.split(" "))
                initSeedAdapter()
                getMultiSeedList()
            }
        }
    }

    private fun getMultiSeedList(): ArrayList<MultiSeed> {
        val multiSeedList = ArrayList<MultiSeed>()
        for (seed in seeds) {
            multiSeedList.add(getMultiSeed(allSeeds.indexOf(seed)))
        }

        return multiSeedList
    }

    private fun getMultiSeed(realSeedIndex: Int): MultiSeed {

        val list = (0 until 33).toMutableList()
        list.remove(realSeedIndex)

        val firstRandom = list.random()
        val firstInputSeed = InputSeed(firstRandom, allSeeds[firstRandom])
        list.remove(firstRandom)

        val secondRandom = list.random()
        val secondInputSeed = InputSeed(secondRandom, allSeeds[secondRandom])

        val realInputSeed = InputSeed(realSeedIndex, allSeeds[realSeedIndex])

        val arr = ArrayList<InputSeed>()
        arr.add(firstInputSeed)
        arr.add(secondInputSeed)
        arr.add(realInputSeed)

        arr.shuffle()

        return MultiSeed(arr[0], arr[1], arr[2])
    }

    private fun initSeedAdapter() {
        val allSeedWords = getMultiSeedList()
        createWalletAdapter = CreateWalletAdapter(this, allSeedWords, this, footer)
        recyclerViewSeeds.adapter = createWalletAdapter
    }

    private fun notAllSeedsEntered() {
        footer.error = getString(R.string.notAllSeedsEntered)
        // 34: last item on the last after adding up 1 header and 33 seed rows(using 0th index)
        createWalletAdapter.notifyItemChanged(34)
    }

    override fun onConfirm(enteredSeeds: ArrayList<String>, emptySeed: Boolean) {

        if (emptySeed) {
            notAllSeedsEntered()
            return
        }

        val seedString = enteredSeeds.joinToString(" ", "", "", -1, "...")

        if (Dcrlibwallet.verifySeed(seedString)) {
            footer.buttonBackground = R.drawable.btn_shape3
            footer.error = null

            val intent = Intent(this, EncryptWallet::class.java)
            intent.putExtra(Constants.SEED, seedString)
            startActivity(intent)
        } else {
            footer.buttonBackground = R.drawable.btn_shape2
            footer.error = getString(R.string.create_wallet_incorrect_seeds_input)
        }


        createWalletAdapter.notifyItemChanged(34)
    }

    override fun onSeedEntered(enteredSeeds: ArrayList<String>, emptySeed: Boolean, position: Int) {

        linearLayoutManager.scrollToPosition(position + 2)

        if (emptySeed) {
            notAllSeedsEntered()
            return
        }

        val seedString = enteredSeeds.joinToString(" ", "", "", -1, "...")
        if (Dcrlibwallet.verifySeed(seedString)) {
            footer.buttonBackground = R.drawable.btn_shape3
            footer.error = null
        } else {
            footer.buttonBackground = R.drawable.btn_shape2
            footer.error = getString(R.string.create_wallet_incorrect_seeds_input)
        }

        // 34: last item on the last after adding up 1 header and 33 seed rows(using 0th index)
        createWalletAdapter.notifyItemChanged(34)
    }
}