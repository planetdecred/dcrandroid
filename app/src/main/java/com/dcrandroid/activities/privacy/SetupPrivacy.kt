/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities.privacy

import android.content.Intent
import android.os.Bundle
import com.dcrandroid.R
import com.dcrandroid.activities.BaseActivity
import com.dcrandroid.data.Constants
import dcrlibwallet.Wallet
import kotlinx.android.synthetic.main.activity_setup_privacy.*

class SetupPrivacy : BaseActivity() {

    private lateinit var wallet: Wallet
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_privacy)

        wallet = multiWallet!!.walletWithID(intent.extras!!.getLong(Constants.WALLET_ID))
        wallet_name.text = wallet.name

        btn_setup_mixer.setOnClickListener {
            val intent = Intent(this, SetupMixerAccounts::class.java)
            intent.putExtra(Constants.WALLET_ID, wallet.id)
            finish()
            startActivity(intent)
        }

        go_back.setOnClickListener { finish() }

        multiWallet!!.setBoolConfigValueForKey(Constants.CHECKED_PRIVACY_PAGE, true)
    }
}