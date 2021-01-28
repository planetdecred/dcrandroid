/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities

import android.os.Bundle
import com.dcrandroid.R
import com.dcrandroid.adapter.DisabledAccounts
import com.dcrandroid.data.Constants
import com.dcrandroid.view.util.AccountCustomSpinner
import dcrlibwallet.Wallet
import kotlinx.android.synthetic.main.activity_manual_mixer_setup.*
import kotlinx.android.synthetic.main.activity_setup_mixer_accounts.go_back
import kotlinx.android.synthetic.main.activity_setup_mixer_accounts.wallet_name
import java.util.*

class ManualMixerSetup : BaseActivity() {

    private lateinit var wallet: Wallet
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual_mixer_setup)

        wallet = multiWallet!!.walletWithID(intent.extras!!.getLong(Constants.WALLET_ID))
        wallet_name.text = wallet.name
        val disabledAccounts = EnumSet.of(DisabledAccounts.MixerMixedAccount)
        val mixed = AccountCustomSpinner(supportFragmentManager,
                mixed_account_spinner, R.string.dest_account_picker_title, disabledAccounts)

        val unmixed = AccountCustomSpinner(supportFragmentManager,
                unmixed_account_spinner, R.string.dest_account_picker_title, disabledAccounts)

        go_back.setOnClickListener { finish() }
    }
}