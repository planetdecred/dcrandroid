/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import com.dcrandroid.R
import com.dcrandroid.adapter.SuggestionsTextAdapter
import com.dcrandroid.data.Constants
import com.dcrandroid.fragments.PasswordPinDialogFragment
import com.dcrandroid.util.SnackBar
import com.dcrandroid.util.WalletData
import com.dcrandroid.view.SeedEditTextLayout
import com.dcrandroid.view.util.SeedEditTextHelper
import dcrlibwallet.Dcrlibwallet
import dcrlibwallet.MultiWallet
import kotlinx.android.synthetic.main.activity_restore_wallet.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

const val SEED_COUNT = 33

class RestoreWalletActivity : AppCompatActivity(), PasswordPinDialogFragment.PasswordPinListener {

    private val multiWallet: MultiWallet?
        get() = WalletData.multiWallet

    var allSeedWords = ArrayList<String>()
    val seedInputHelperList = ArrayList<SeedEditTextHelper>()

    val allSeedIsValid: Boolean
        get() {
            for (helper in seedInputHelperList) {
                if (!validateSeed(helper.getSeed())) {
                    return false
                }
            }

            return true
        }

    private val enteredSeeds: String
        get() {
            val seeds = ArrayList<String>()
            seedInputHelperList.forEach { seeds.add(it.getSeed()) }

            return seeds.joinToString(" ")
        }

    private val validateSeed: (seed: String) -> Boolean = {
        allSeedWords.indexOf(it) > 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restore_wallet)

        loadSeedSuggestions()
        val suggestionsAdapter = SuggestionsTextAdapter(this, R.layout.dropdown_item_1, allSeedWords)

        for (i in 0 until SEED_COUNT) {
            val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            val seedEditText = SeedEditTextLayout(this)
            seedEditText.layoutParams = layoutParams

            seed_input_list.addView(seedEditText)

            val seedInputHelper = SeedEditTextHelper(seedEditText, suggestionsAdapter, i)

            seedInputHelper.validateSeed = validateSeed

            seedInputHelper.seedChanged = {
                btn_restore.isEnabled = allSeedIsValid
                Unit
            }

            seedInputHelper.moveToNextRow = { currentItem ->
                val nextRow = currentItem + 1
                if (nextRow < SEED_COUNT) {
                    val scrollY = seedInputHelperList[nextRow].requestFocus()
                    nested_scroll_view.scrollTo(0, scrollY)

                }
            }

            seedInputHelperList.add(seedInputHelper)
        }

        nested_scroll_view.setOnScrollChangeListener { _: NestedScrollView?, _: Int, scrollY: Int, _: Int, _: Int ->
            app_bar.elevation = when {
                scrollY > 0 -> resources.getDimension(R.dimen.app_bar_elevation)
                else -> 0f
            }
        }

        btn_restore.setOnClickListener {
            val seedValid = Dcrlibwallet.verifySeed(enteredSeeds)

            if (!seedValid) {
                SnackBar.showError(this, R.string.invalid_restore_seed)
                return@setOnClickListener
            }

            PasswordPinDialogFragment(R.string.create, true, isChange = false, passwordPinListener = this).show(this)
        }

        go_back.setOnClickListener { finish() }
    }

    private fun loadSeedSuggestions() = GlobalScope.launch(Dispatchers.IO) {
        val seedWords = Dcrlibwallet.AlternatingWords.split("\n")
        allSeedWords.addAll(seedWords)
    }

    override fun onEnterPasswordOrPin(newPassphrase: String, passphraseType: Int) {
        createWallet(newPassphrase, passphraseType, enteredSeeds)
    }

    private fun createWallet(spendingKey: String, spendingPassType: Int, seed: String) = GlobalScope.launch(Dispatchers.IO) {
        try {
            val wallet = multiWallet!!.restoreWallet(seed, spendingKey, spendingPassType)
            wallet.unlockWallet(spendingKey.toByteArray())

            val intent = Intent(this@RestoreWalletActivity, RestoreSuccessActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
            intent.putExtra(Constants.WALLET_ID, wallet.id)
            startActivity(intent)

            finish()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}