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
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.dcrandroid.R
import com.dcrandroid.adapter.CreateWalletAdapter
import com.dcrandroid.adapter.InputSeed
import com.dcrandroid.adapter.MultiSeed
import com.dcrandroid.data.Constants
import dcrlibwallet.Dcrlibwallet
import kotlinx.android.synthetic.main.confirm_seed_page.*

class VerifySeedActivity : BaseActivity() {

    private var verifiedSeed: Boolean = false

    private var finalSeedsString = ""

    private var currentSeedPosition = 0

    private var allSeeds = ArrayList<String>()
    private val shuffledSeeds = ArrayList<InputSeed>()
    private val arrayOfSeedLists = ArrayList<MultiSeed>()
    private var sortedList = listOf<InputSeed>()
    private val confirmedSeedsArray = ArrayList<InputSeed>()
    private val arrayOfRandomSeeds = ArrayList<InputSeed>()

    private lateinit var createWalletAdapter: CreateWalletAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        setContentView(R.layout.confirm_seed_page)
        recyclerViewSeeds.isNestedScrollingEnabled = false
        linearLayoutManager = LinearLayoutManager(this)
        recyclerViewSeeds.layoutManager = linearLayoutManager
        button_confirm_seed.setOnClickListener {
            if (verifiedSeed) {
                val intent = Intent(this, EncryptWallet::class.java)
                intent.putExtra(Constants.SEED, finalSeedsString)
                startActivity(intent)
            }
        }
        prepareData()
    }

    private fun prepareData() {
        val bundle = intent.extras
        if (bundle != null && !bundle.isEmpty) {
            val seed = bundle.getString(Constants.SEED)
            if(seed != null) {
                allSeeds = ArrayList(seed.split(" "))
                tvHeader.text = getString(R.string.seed_phrase_verification)
                tvHint.text = getString(R.string.please_confirm_your_seed_by_typing_and_tapping_each_word_accordingly)
                headerTop.isFocusableInTouchMode = true
                initSeedAdapter()
            }
        }
    }

    private fun initSeedAdapter() {
        createWalletAdapter = CreateWalletAdapter(applicationContext, arrayOfSeedLists, { enteredSeeds: ArrayList<InputSeed> ->
            confirmedSeedsArray.clear()
            confirmedSeedsArray.addAll(enteredSeeds)
            sortedList = confirmedSeedsArray.sortedWith(compareBy { it.number }).distinct()

            if (sortedList.size < 33) {
                tvError.visibility = View.VISIBLE
                tvError.text = getString(R.string.notAllSeedsEntered)
            }
            val itemView = recyclerViewSeeds.findViewById<RelativeLayout>(R.id.rlButtons)
            val itemHeight = itemView.measuredHeight
            val maxAllowedHeight = nestedScrollView.getChildAt(0).bottom - (itemView.measuredHeight + itemView.measuredHeight / 2)

            val currentHeight = (nestedScrollView.scrollY + nestedScrollView.height)
            if (currentHeight > maxAllowedHeight) {
                nestedScrollView.smoothScrollBy(0, itemHeight * 2)
            }
        }, { isAllEntered: Boolean ->
            if (isAllEntered && sortedList.size == 33) {
                handleSingleTap()
            }
        })
        recyclerViewSeeds.adapter = createWalletAdapter
        generateRandomSeeds()
    }

    private fun generateRandomSeeds() {
        val firstRandom = (0 until allSeeds.size).random()
        val secondRandom = (0 until allSeeds.size).random()
        var currentItemPosition = 0
        if (currentSeedPosition != 33) {
            currentItemPosition = (currentSeedPosition)
        }

        if (firstRandom != secondRandom && firstRandom != currentItemPosition && secondRandom != currentItemPosition) {
            for (item in allSeeds) {
                when (item) {
                    allSeeds[firstRandom] -> arrayOfRandomSeeds.add(InputSeed(firstRandom, item))
                    allSeeds[secondRandom] -> arrayOfRandomSeeds.add(InputSeed(secondRandom, item))
                    allSeeds[currentItemPosition] -> arrayOfRandomSeeds.add(InputSeed(currentItemPosition, item))
                }
            }
            addSeedsToAdapter()
        } else {
            generateRandomSeeds()
        }
    }

    private fun addSeedsToAdapter() {
        shuffledSeeds.addAll(arrayOfRandomSeeds.shuffled().distinct())
        if (shuffledSeeds.size == 3) {
            when {
                currentSeedPosition < 32 -> {
                    arrayOfSeedLists.add(MultiSeed(shuffledSeeds[0], shuffledSeeds[1], shuffledSeeds[2]))
                    createWalletAdapter.notifyDataSetChanged()
                    currentSeedPosition++
                    arrayOfRandomSeeds.clear()
                    shuffledSeeds.clear()
                    generateRandomSeeds()
                }
                currentSeedPosition == 32 -> {
                    arrayOfSeedLists.add(MultiSeed(shuffledSeeds[0], shuffledSeeds[1], shuffledSeeds[2]))
                    createWalletAdapter.notifyDataSetChanged()
                    arrayOfRandomSeeds.clear()
                    shuffledSeeds.clear()
                }
            }
        } else {
            arrayOfRandomSeeds.clear()
            shuffledSeeds.clear()
            generateRandomSeeds()
        }
    }

    private fun handleSingleTap() {

        if (sortedList.isNotEmpty() && (sortedList.size == 33)) {
            finalSeedsString = sortedList.joinToString(" ", "", "", -1, "...") { it.phrase }
            verifiedSeed = Dcrlibwallet.verifySeed(finalSeedsString)

            if (verifiedSeed) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    button_confirm_seed.background = ContextCompat.getDrawable(applicationContext, R.drawable.btn_shape3)
                } else {
                    button_confirm_seed.setBackgroundDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.btn_shape3))
                }

                tvError.visibility = View.GONE

            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    button_confirm_seed.background = ContextCompat.getDrawable(applicationContext, R.drawable.btn_shape2)
                } else {
                    button_confirm_seed.setBackgroundDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.btn_shape2))
                }

                tvError.visibility = View.VISIBLE
                tvError.text = getString(R.string.create_wallet_incorrect_seeds_input)
            }

        } else if (sortedList.isNotEmpty() && (sortedList.size != 33)) {
            tvError.text = getString(R.string.notAllSeedsEntered)
        } else {
            tvError.text = getString(R.string.theInputFieldIsEmpty)
        }
    }
}