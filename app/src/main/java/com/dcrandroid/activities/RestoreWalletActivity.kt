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
import com.dcrandroid.dialog.FullScreenBottomSheetDialog
import com.dcrandroid.dialog.RequestNameDialog
import com.dcrandroid.fragments.PasswordPinDialogFragment
import com.dcrandroid.util.SnackBar
import com.dcrandroid.util.Utils
import com.dcrandroid.util.WalletData
import com.dcrandroid.view.SeedEditTextLayout
import com.dcrandroid.view.util.SeedEditTextHelper
import dcrlibwallet.Dcrlibwallet
import dcrlibwallet.MultiWallet
import kotlinx.android.synthetic.main.activity_restore_wallet.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList

const val SEED_COUNT = 33

class RestoreWalletActivity : AppCompatActivity() {

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
                    seedInputHelperList[nextRow].requestFocus()

                    var topRow = currentItem - 1
                    if (topRow < 0) topRow = 0
                    nested_scroll_view.scrollTo(0, seedInputHelperList[topRow].scrollY)

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

            if (multiWallet!!.loadedWalletsCount() == 0) {
                requestWalletSpendingPass(getString(R.string.mywallet))
            } else {
                RequestNameDialog(R.string.wallet_name, "", true) { newName ->
                    try {
                        if (multiWallet!!.walletNameExists(newName)) {
                            return@RequestNameDialog Exception(Dcrlibwallet.ErrExist)
                        }

                        requestWalletSpendingPass(newName)

                    } catch (e: Exception) {
                        return@RequestNameDialog e
                    }
                    return@RequestNameDialog null
                }.show(this)
            }
        }

        go_back.setOnClickListener { finish() }
    }

    private fun loadSeedSuggestions() = GlobalScope.launch(Dispatchers.IO) {
        val seedWords = Dcrlibwallet.AlternatingWords.split("\n")
        allSeedWords.addAll(seedWords)
    }

    private fun requestWalletSpendingPass(walletName: String) {
        PasswordPinDialogFragment(R.string.create, true, isChange = false) { dialog, passphrase, passphraseType ->
            createWallet(dialog, walletName, passphrase, passphraseType, enteredSeeds)
        }.show(this)
    }

    private fun createWallet(dialog: FullScreenBottomSheetDialog, walletName: String, spendingKey: String, spendingPassType: Int, seed: String) = GlobalScope.launch(Dispatchers.IO) {
        val op = this@RestoreWalletActivity.javaClass.name + ".createWallet"
        try {
            val wallet = multiWallet!!.restoreWallet(walletName, seed, spendingKey, spendingPassType)
            if(Locale.getDefault().language != Locale.ENGLISH.language){
                wallet.renameAccount(Constants.DEF_ACCOUNT_NUMBER, getString(R.string._default))
            }
            wallet.unlockWallet(spendingKey.toByteArray())

            withContext(Dispatchers.Main) {
                dialog.dismiss()
            }

            val intent = Intent(this@RestoreWalletActivity, RestoreSuccessActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
            intent.putExtra(Constants.WALLET_ID, wallet.id)
            startActivity(intent)

            finish()

        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                dialog.dismiss()
                Dcrlibwallet.logT(op, e.message)
                Utils.showErrorDialog(this@RestoreWalletActivity, op + ": " + e.message)
            }
        }
    }
}