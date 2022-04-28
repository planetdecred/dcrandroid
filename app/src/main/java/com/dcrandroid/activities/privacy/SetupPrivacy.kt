/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities.privacy

import android.content.Intent
import android.os.Bundle
import android.text.Html
import com.dcrandroid.R
import com.dcrandroid.activities.BaseActivity
import com.dcrandroid.data.Constants
import com.dcrandroid.dialog.InfoDialog
import com.dcrandroid.util.PassPromptTitle
import com.dcrandroid.util.PassPromptUtil
import com.dcrandroid.util.SnackBar
import dcrlibwallet.Wallet
import kotlinx.android.synthetic.main.account_row.*
import kotlinx.android.synthetic.main.activity_setup_mixer_accounts.*
import kotlinx.android.synthetic.main.activity_setup_privacy.*
import kotlinx.android.synthetic.main.activity_setup_privacy.go_back
import kotlinx.android.synthetic.main.activity_setup_privacy.wallet_name
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SetupPrivacy : BaseActivity() {

    private lateinit var wallet : Wallet
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_privacy)

        wallet = multiWallet!!.walletWithID(intent.extras!!.getLong(Constants.WALLET_ID))
        wallet_name.text = wallet.name

        btn_setup_mixer.setOnClickListener {
            // Check for account name "imported" and subtract 1 from total account count
            if (wallet.accounts.toString().contains("imported", ignoreCase = true)) {
                val walletCount = wallet.accountsRaw.count - 1
                if (walletCount > 2) {
                    val intent = Intent(this, SetupMixerAccounts::class.java)
                    intent.putExtra(Constants.WALLET_ID, wallet.id)
                    finish()
                    startActivity(intent)
                } else {
                    InfoDialog(this)
                        .setDialogTitle(getString(R.string.privacy_intro_dialog_title))
                        .setMessage(Html.fromHtml(getString(R.string.privacy_intro_dialog_desc)))
                        .setNegativeButton(getString(R.string.cancel))
                        .setPositiveButton(
                            getString(R.string.begin_setup)
                        ) { _, _ ->
                            checkAccountNameConflict()
                        }
                        .show()
                }
                go_back.setOnClickListener { finish() }

                multiWallet!!.setBoolConfigValueForKey(Constants.CHECKED_PRIVACY_PAGE, true)
            }
        }
    }

    private fun checkAccountNameConflict() {
        if (wallet.hasAccount(Constants.MIXED) || wallet.hasAccount(Constants.UNMIXED)) {
            InfoDialog(this)
                .setDialogTitle(R.string.account_name_taken)
                .setMessage(R.string.account_name_conflict_dialog_desc)
                .setIcon(R.drawable.ic_alert2, R.drawable.grey_dialog_bg)
                .cancelable(false)
                .setPositiveButton(
                    R.string.go_back_rename
                ) { dialog, _ ->
                    dialog.dismiss()
                    finish()
                }
                .show()
            return
        }

        beginAutoSetup()
    }

    private fun beginAutoSetup() {
        val title = PassPromptTitle(
            R.string.confirm_create_needed_accounts,
            R.string.confirm_create_needed_accounts,
            R.string.confirm_create_needed_accounts
        )
        PassPromptUtil(this, wallet.id, title, allowFingerprint = true) { dialog, passphrase ->

            if (passphrase == null) {
                return@PassPromptUtil true
            }

            GlobalScope.launch(Dispatchers.Default) {
                try {
                    wallet.createMixerAccounts(Constants.MIXED, Constants.UNMIXED, passphrase)
                    multiWallet!!.setBoolConfigValueForKey(Constants.HAS_SETUP_PRIVACY, true)
                    val intent = Intent(this@SetupPrivacy, AccountMixerActivity::class.java)
                    intent.putExtra(Constants.WALLET_ID, wallet.id)
                    dialog?.dismissAllowingStateLoss()
                    startActivity(intent)
                    finish()
                    SnackBar.showText(this@SetupPrivacy, R.string.mixer_setup_completed)
                } catch (e: Exception) {
                    e.printStackTrace()

                    PassPromptUtil.handleError(this@SetupPrivacy, e, dialog!!)
                }
            }
            false
        }.show()
    }

}