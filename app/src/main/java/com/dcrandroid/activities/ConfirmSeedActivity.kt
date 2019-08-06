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
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.dcrandroid.R
import com.dcrandroid.adapter.InputSeed
import com.dcrandroid.adapter.RestoreWalletAdapter
import com.dcrandroid.data.Constants
import com.dcrandroid.util.PreferenceUtil
import dcrlibwallet.Dcrlibwallet
import kotlinx.android.synthetic.main.confirm_seed_page.*

private const val CLICK_THRESHOLD = 300 //millisecond

class ConfirmSeedActivity : BaseActivity(), View.OnTouchListener {

    private var verifiedSeed: Boolean = false
    private var lastConfirmClick: Long = 0
    private var finalSeedsString = ""

    private var allSeeds = ArrayList<String>()
    private val seedsForInput = ArrayList<InputSeed>()
    private val confirmedSeedsArray = ArrayList<InputSeed>()
    private var sortedList = listOf<InputSeed>()

    private var handler: Handler? = null

    private lateinit var restoreWalletAdapter: RestoreWalletAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

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
        if (bundle != null && !bundle.isEmpty) {
            val seed = bundle.getString(Constants.SEED)
            allSeeds = ArrayList(seed.split(" "))
            temp.forEachIndexed { number, _ ->
                seedsForInput.add(InputSeed(number, " "))
                initSeedAdapter()
            }
        }
    }

    private fun initSeedAdapter() {
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

                        val util = PreferenceUtil(this)
                        util.set(Constants.SEED, null)
                        util.setBoolean(Constants.VERIFIED_SEED, true)

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
        val enteredSeed = "miser stupendous backward inception slowdown Capricorn uncut visitor slowdown caravan blockade hemisphere repay article necklace hazardous cobra inferno python suspicious minnow Norwegian chairlift backwater surmount impetus cement stupendous snowslide sympathy fallout embezzle afflict"
        if (enteredSeed.isNotEmpty()) {

            val util = PreferenceUtil(this)
            util.set(Constants.SEED, null)
            util.setBoolean(Constants.VERIFIED_SEED, true)

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
                tvError.text = getString(R.string.restore_wallet_incorrect_seed_input)
            }

        } else if (sortedList.isNotEmpty() && (sortedList.size != 33)) {
            tvError.text = getString(R.string.notAllSeedsEntered)
        } else {
            tvError.text = getString(R.string.theInputFieldIsEmpty)
        }
    }
}
