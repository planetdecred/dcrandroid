/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities.privacy

import android.app.Activity
import android.content.DialogInterface
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
import kotlinx.android.synthetic.main.activity_setup_mixer_accounts.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SetupMixerAccounts : BaseActivity() {

    private lateinit var wallet: Wallet
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_mixer_accounts)

        wallet = multiWallet!!.walletWithID(intent.extras!!.getLong(Constants.WALLET_ID))
        wallet_name.text = wallet.name

        btn_auto_setup.setOnClickListener {
            InfoDialog(this)
                    .setDialogTitle(getString(R.string.privacy_intro_dialog_title))
                    .setMessage(Html.fromHtml(getString(R.string.privacy_intro_dialog_desc)))
                    .setNegativeButton(getString(R.string.cancel))
                    .setPositiveButton(getString(R.string.begin_setup), DialogInterface.OnClickListener { _, _ ->
                        checkAccountNameConflict()
                    })
                    .show()
        }

        btn_manual_setup.setOnClickListener {
            val intent = Intent(this, ManualMixerSetup::class.java)
            intent.putExtra(Constants.WALLET_ID, wallet.id)
            startActivityForResult(intent, MANUAL_MIXER_REQUEST_CODE)
        }

        go_back.setOnClickListener { finish() }


    }

    private fun checkAccountNameConflict() {

        if (wallet.hasAccount(Constants.MIXED) || wallet.hasAccount(Constants.UNMIXED)) {
            InfoDialog(this)
                    .setDialogTitle(R.string.account_name_taken)
                    .setMessage(R.string.account_name_conflict_dialog_desc)
                    .setIcon(R.drawable.ic_alert2, R.drawable.grey_dialog_bg)
                    .cancelable(false)
                    .setPositiveButton(R.string.go_back_rename, DialogInterface.OnClickListener { dialog, _ ->
                        dialog.dismiss()
                        finish()
                    })
                    .show()
            return
        }

        beginAutoSetup()
    }

    private fun beginAutoSetup() {
        val title = PassPromptTitle(R.string.confirm_create_needed_accounts, R.string.confirm_create_needed_accounts, R.string.confirm_create_needed_accounts)
        PassPromptUtil(this, wallet.id, title, allowFingerprint = true) { dialog, passphrase ->

            if (passphrase == null) {
                return@PassPromptUtil true
            }

            GlobalScope.launch(Dispatchers.Default) {
                try {
                    wallet.createMixerAccounts(Constants.MIXED, Constants.UNMIXED, passphrase)
                    multiWallet!!.setBoolConfigValueForKey(Constants.HAS_SETUP_PRIVACY, true)
                    val intent = Intent(this@SetupMixerAccounts, AccountMixerActivity::class.java)
                    intent.putExtra(Constants.WALLET_ID, wallet.id)
                    dialog?.dismissAllowingStateLoss()
                    startActivity(intent)
                    finish()
                    SnackBar.showText(this@SetupMixerAccounts, R.string.mixer_setup_completed)
                } catch (e: Exception) {
                    e.printStackTrace()

                    PassPromptUtil.handleError(this@SetupMixerAccounts, e, dialog!!)
                }
            }
            false
        }.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MANUAL_MIXER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            finish()
        }
    }

}