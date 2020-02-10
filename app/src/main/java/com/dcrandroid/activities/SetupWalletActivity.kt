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
import kotlinx.android.synthetic.main.activity_setup_page.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

const val RESTORE_WALLET_REQUEST_CODE = 1

class SetupWalletActivity : BaseActivity(), PasswordPinDialogFragment.PasswordPinListener {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_setup_page)

        ll_create_wallet.setOnClickListener {
            PasswordPinDialogFragment(R.string.create, isSpending = true, isChange = false, passwordPinListener = this).show(this)
        }

        ll_create_watch_only.setOnClickListener {
            CreateWatchOnlyWallet { walletID ->
                navigateToHomeActivity(walletID)
            }.show(this)
        }

        ll_restore_wallet.setOnClickListener {
            val restoreIntent = Intent(this, RestoreWalletActivity::class.java)
            startActivityForResult(restoreIntent, RESTORE_WALLET_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RESTORE_WALLET_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            setResult(Activity.RESULT_OK, data)
            finish()
        }
    }

    /**
     * Callback when the user submits spending password or pin
     *
     * @param newPassphrase - either a spending password or pin
     * @param passphraseType  - flag to tell whether its a password or pin
     */
    override fun onEnterPasswordOrPin(newPassphrase: String, passphraseType: Int) {
        createWallet(newPassphrase, passphraseType)
    }

    private fun navigateToHomeActivity(walletID: Long) = GlobalScope.launch(Dispatchers.Main) {
        if (multiWallet!!.openedWalletsCount() > 1) {
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

    private fun createWallet(spendingKey: String, type: Int) = GlobalScope.launch(Dispatchers.IO) {
        try {
            val wallet = multiWallet!!.createNewWallet(spendingKey, type)
            navigateToHomeActivity(wallet.id)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}