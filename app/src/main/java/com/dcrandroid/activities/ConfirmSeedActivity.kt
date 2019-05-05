/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.dcrandroid.R
import com.dcrandroid.adapter.CreateWalletAdapter
import com.dcrandroid.adapter.InputSeed
import com.dcrandroid.adapter.MultiSeed
import com.dcrandroid.adapter.RestoreWalletAdapter
import com.dcrandroid.data.Constants
import dcrlibwallet.Dcrlibwallet
import kotlinx.android.synthetic.main.confirm_seed_page.*
import java.util.*
import kotlin.collections.ArrayList


private const val CLICK_THRESHOLD = 300 //millisecond

// TODO: Split this class into VerifySeedActivity  for old wallet and EnterSeedActivity for new wallet.
class ConfirmSeedActivity : AppCompatActivity(), View.OnTouchListener {

    private var restore: Boolean = false
    private var verifiedSeed: Boolean = false
    private var allSeeds = ArrayList<String>()
    private val seedsForInput = ArrayList<InputSeed>()
    private val confirmedSeedsArray = ArrayList<InputSeed>()
    private var sortedList = listOf<InputSeed>()
    private val arrayOfRandomSeeds = ArrayList<InputSeed>()
    private val arrayOfSeedLists = ArrayList<MultiSeed>()
    private val shuffledSeeds = ArrayList<InputSeed>()
    private var finalSeedsString = ""
    private var lastConfirmClick: Long = 0
    private var handler: Handler? = null
    private var currentSeedPosition = 0
    private lateinit var restoreWalletAdapter: RestoreWalletAdapter
    private lateinit var createWalletAdapter: CreateWalletAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val decorView = window.decorView
            decorView.systemUiVisibility = WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        setContentView(R.layout.confirm_seed_page)
        recyclerViewSeeds.isNestedScrollingEnabled = false
        linearLayoutManager = LinearLayoutManager(this)
        recyclerViewSeeds.layoutManager = linearLayoutManager
        button_confirm_seed.setOnTouchListener(this)
        prepareData()
    }

    private fun prepareData() {
        val bundle = intent.extras
        val temp = arrayOfNulls<String>(33)
        if (!bundle.isEmpty) {
            val seed = bundle.getString(Constants.SEED)
            restore = bundle.getBoolean(Constants.RESTORE)
            allSeeds = ArrayList(seed.split(" "))
            if (restore) {
                temp.forEachIndexed { number, _ ->
                    seedsForInput.add(InputSeed(number, " "))
                    initOldWalletAdapter()
                }
            } else {
                tvHeader.text = getString(R.string.seed_phrase_verification)
                tvHint.text = getString(R.string.please_confirm_your_seed_by_typing_and_tapping_each_word_accordingly)
                headerTop.isFocusableInTouchMode = true
                initNewWalletAdapter()
            }
        }
    }

    private fun initOldWalletAdapter() {
        restoreWalletAdapter = RestoreWalletAdapter(seedsForInput, allSeeds, applicationContext,
                { savedSeed: InputSeed ->
                    confirmedSeedsArray.add(savedSeed)
                    sortedList = confirmedSeedsArray.sortedWith(compareBy { it.number }).distinct()
                    if (sortedList.size < 33) {
                        tvError.visibility = View.VISIBLE
                        tvError.text = getString(R.string.notAllSeedsEntered)
                    }
                    val itemView = recyclerViewSeeds.findViewById<RelativeLayout>(R.id.restoreSeedRow)
                    val itemHeight = itemView.measuredHeight + itemView.measuredHeight / 60
                    val maxAllowedHeight = nestedScrollView.getChildAt(0).bottom - itemHeight * 3
                    val currentHeight = (nestedScrollView.scrollY + nestedScrollView.height)

                    if (currentHeight > maxAllowedHeight) {
                        recyclerViewSeeds.isFocusableInTouchMode = false
                        llButtons.isFocusableInTouchMode = true
                        llButtons.requestFocus()
                    }
                },
                { removeSeed: InputSeed ->
                    confirmedSeedsArray.clear()
                    confirmedSeedsArray.addAll(sortedList)
                    confirmedSeedsArray.remove(removeSeed)
                    sortedList = confirmedSeedsArray.sortedWith(compareBy { it.number })
                }, { isAllEntered: Boolean ->
            if (isAllEntered && sortedList.size == 33) {
                handleSingleTap()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(llButtons.windowToken, 0)
            }
        })
        recyclerViewSeeds.adapter = restoreWalletAdapter
    }

    private fun initNewWalletAdapter() {
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
        val firstRandom = (1..allSeeds.size).random()
        val secondRandom = (1..allSeeds.size).random()
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

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        when (event!!.action) {
            MotionEvent.ACTION_DOWN -> {
                lastConfirmClick = System.currentTimeMillis()
                handler = Handler()
                handler!!.postDelayed(longHold, 5000)
            }
            MotionEvent.ACTION_UP -> {
                handler!!.removeCallbacks(longHold)
                if ((System.currentTimeMillis() - lastConfirmClick) <= CLICK_THRESHOLD) {
                    if (verifiedSeed) {
                        val intent = Intent(this, EncryptWallet::class.java)
                        intent.putExtra(Constants.SEED, finalSeedsString)
                        startActivity(intent)
                    }
                }
            }
        }

        return false
    }

    private val longHold = Runnable {
        val enteredSeed = ""
        if (enteredSeed.isNotEmpty()) {
            val i = Intent(this@ConfirmSeedActivity, EncryptWallet::class.java)
                    .putExtra(Constants.SEED, enteredSeed)
            startActivity(i)
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

                if (restore) {
                    tvError.text = getString(R.string.restore_wallet_incorrect_seed_input)
                } else {
                    tvError.text = getString(R.string.create_wallet_incorrect_seeds_input)
                }
            }

        } else if (sortedList.isNotEmpty() && (sortedList.size != 33)) {
            tvError.text = getString(R.string.notAllSeedsEntered)
        } else {
            tvError.text = getString(R.string.theInputFieldIsEmpty)
        }
    }

    private fun IntRange.random() = Random().nextInt((allSeeds.size + 1) - start)
}
