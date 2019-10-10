/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle

import androidx.core.app.ActivityCompat

import com.dcrandroid.HomeActivity
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.dialog.CreateWatchOnlyWallet
import com.dcrandroid.fragments.PasswordPinDialogFragment
import com.dcrandroid.util.PreferenceUtil
import dcrlibwallet.Dcrlibwallet
import kotlinx.android.synthetic.main.activity_setup_page.*

import java.util.concurrent.Executors.newSingleThreadExecutor


class SetupWalletActivity : BaseActivity(), PasswordPinDialogFragment.PasswordPinListener {

    private var preferenceUtil: PreferenceUtil? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_setup_page)

        preferenceUtil = PreferenceUtil(this)

        ll_create_wallet.setOnClickListener{
            PasswordPinDialogFragment().show(this)
        }

        ll_create_watch_only.setOnClickListener {
            CreateWatchOnlyWallet {walletID ->
                navigateToMainActivity(walletID)
            }.show(this)
        }

        ll_restore_wallet.setOnClickListener{
            startActivity(Intent(this, RestoreWalletActivity::class.java))
        }
    }

    /**
     * Callback when the user submits spending password or pin
     *
     * @param spendingKey - either a spending password or pin
     * @param isPassword  - flag to tell whether its a password or pin
     */
    override fun onEnterPasswordOrPin(spendingKey: String, isPassword: Boolean) {
        if (isPassword) {
            preferenceUtil!!.set(Constants.SPENDING_PASSPHRASE_TYPE, Constants.PASSWORD)
            createWallet(spendingKey, Dcrlibwallet.SpendingPassphraseTypePass)
        } else {
            createWallet(spendingKey, Dcrlibwallet.SpendingPassphraseTypePin)
            preferenceUtil!!.set(Constants.SPENDING_PASSPHRASE_TYPE, Constants.PIN)
        }
    }


    private fun navigateToMainActivity(walletID: Long) {

        runOnUiThread {

            if (multiWallet.openedWalletsCount() > 1) {
                val data = Intent()
                data.putExtra(Constants.WALLET_ID, walletID)
                setResult(Activity.RESULT_OK, data)
                finish()
            } else {
                val intent = Intent(this@SetupWalletActivity, HomeActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)

                startActivity(intent)
                ActivityCompat.finishAffinity(this@SetupWalletActivity)
            }
        }
    }

    /**
     * Creates the wallet and navigate user to main activity
     *
     * @param spendingKey - spending password or pin
     */
    private fun createWallet(spendingKey: String, type: Int) {

        newSingleThreadExecutor().execute {
            try {
                preferenceUtil!!.setBoolean(Constants.RESTORE_WALLET, false)
                val seed = "miser stupendous backward inception slowdown Capricorn uncut visitor slowdown caravan blockade hemisphere repay article necklace hazardous cobra inferno python suspicious minnow Norwegian chairlift backwater surmount impetus cement stupendous snowslide sympathy fallout embezzle afflict"
                val wallet = multiWallet.restoreWallet(seed, spendingKey, type)
                wallet.unlockWallet(spendingKey.toByteArray())

                navigateToMainActivity(wallet.walletID)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}