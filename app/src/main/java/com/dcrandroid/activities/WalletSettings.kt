/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities

import android.os.Bundle
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.preference.ListPreference
import dcrlibwallet.Dcrlibwallet
import kotlinx.android.synthetic.main.activity_wallet_settings.*
import kotlinx.android.synthetic.main.activity_wallet_settings.view.*

class WalletSettings: BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet_settings)

        val walletID = intent.getLongExtra(Constants.WALLET_ID, -1)

        val incomingNotficationsKey = walletID.toString() + Dcrlibwallet.IncomingTxNotificationsConfigKey
        setTxNotificationSummary(multiWallet.readInt32ConfigValueForKey(incomingNotficationsKey, Constants.DEF_TX_NOTIFICATION))
        ListPreference(this, incomingNotficationsKey, Constants.DEF_TX_NOTIFICATION,
                R.array.notification_options, incoming_transactions){
            setTxNotificationSummary(it)
        }

        go_back.setOnClickListener {
            finish()
        }
    }

    private fun setTxNotificationSummary(index: Int){
        val preferenceSummary = resources.getStringArray(R.array.notification_options)[index]
        incoming_transactions.pref_subtitle.text = preferenceSummary
    }
}