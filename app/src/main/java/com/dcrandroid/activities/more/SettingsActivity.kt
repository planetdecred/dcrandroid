/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities.more

import android.os.Bundle
import android.view.ViewTreeObserver
import com.dcrandroid.R
import com.dcrandroid.activities.BaseActivity
import com.dcrandroid.preference.SwitchPreference
import dcrlibwallet.Dcrlibwallet
import kotlinx.android.synthetic.main.settings_activity.*

class SettingsActivity: BaseActivity(), ViewTreeObserver.OnScrollChangedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        SwitchPreference(this, Dcrlibwallet.SpendUnconfirmedConfigKey, spend_unconfirmed_funds)
        SwitchPreference(this, Dcrlibwallet.IncomingTxNotificationsConfigKey, incoming_transactions)
        SwitchPreference(this, Dcrlibwallet.BeepNewBlocksConfigKey, beep_new_blocks)
        SwitchPreference(this, Dcrlibwallet.SyncOnCellularConfigKey, wifi_sync)

        go_back.setOnClickListener {
            finish()
        }

        settings_scroll_view.viewTreeObserver.addOnScrollChangedListener(this)
    }

    override fun onScrollChanged() {
        app_bar.elevation = if(settings_scroll_view.scrollY > 0){
            resources.getDimension(R.dimen.app_bar_elevation)
        }else{
            0f
        }
    }
}