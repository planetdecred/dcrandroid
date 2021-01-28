/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities

import android.content.Intent
import android.os.Bundle
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.util.PassPromptTitle
import com.dcrandroid.util.PassPromptUtil
import com.dcrandroid.util.SnackBar
import com.dcrandroid.view.util.AccountCustomSpinner
import dcrlibwallet.Wallet
import kotlinx.android.synthetic.main.activity_manual_mixer_setup.*
import kotlinx.android.synthetic.main.activity_setup_mixer_accounts.go_back
import kotlinx.android.synthetic.main.activity_setup_mixer_accounts.wallet_name
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ManualMixerSetup : BaseActivity() {

    private lateinit var wallet: Wallet
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual_mixer_setup)

        wallet = multiWallet!!.walletWithID(intent.extras!!.getLong(Constants.WALLET_ID))
        wallet_name.text = wallet.name

        val mixed = AccountCustomSpinner(supportFragmentManager, mixed_account_spinner)
        mixed.pickerTitle = R.string.dest_account_picker_title

        val unmixed = AccountCustomSpinner(supportFragmentManager, unmixed_account_spinner)
        unmixed.pickerTitle = R.string.dest_account_picker_title

        mixed.init {
            true
        }
        unmixed.init {
            true
        }

        go_back.setOnClickListener { finish() }

        btn_setup.setOnClickListener {
            if (mixed.selectedAccount!!.accountNumber == unmixed.selectedAccount!!.accountNumber) {
                SnackBar.showError(this, R.string.same_mixed_unmixed)
                return@setOnClickListener
            }

            val title = PassPromptTitle(R.string.confirm_setup_mixer, R.string.confirm_setup_mixer, R.string.confirm_setup_mixer)
            PassPromptUtil(this, wallet.id, title, allowFingerprint = true) { dialog, passphrase ->
                if (passphrase == null) {
                    return@PassPromptUtil true
                }

                GlobalScope.launch(Dispatchers.Default) {
                    try {
                        wallet.setAccountMixerConfig(mixed.selectedAccount!!.accountNumber, unmixed.selectedAccount!!.accountNumber, passphrase)
                        multiWallet!!.setBoolConfigValueForKey(Constants.HAS_SETUP_PRIVACY, true)
                        val intent = Intent(this@ManualMixerSetup, AccountMixerActivity::class.java)
                        intent.putExtra(Constants.WALLET_ID, wallet.id)
                        dialog?.dismissAllowingStateLoss()
                        startActivity(intent)
                        finish()
                        SnackBar.showText(this@ManualMixerSetup, R.string.mixer_setup_completed)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        PassPromptUtil.handleError(this@ManualMixerSetup, e, dialog!!)
                    }
                }
                false
            }.show()
        }
    }
}