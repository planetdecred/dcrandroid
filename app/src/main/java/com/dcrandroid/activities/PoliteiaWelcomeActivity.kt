/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities

import android.content.Intent
import android.os.Bundle
import com.dcrandroid.R
import com.dcrandroid.activities.more.PoliteiaActivity
import com.dcrandroid.data.Constants
import com.dcrandroid.dialog.InfoDialog
import kotlinx.android.synthetic.main.activity_politeia_welcome.*

class PoliteiaWelcomeActivity: BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_politeia_welcome)
        iv_info.setOnClickListener {
            InfoDialog(this)
                .setDialogTitle(getString(R.string.governance))
                .setMessage(getString(R.string.politeia_welcome_info))
                .setPositiveButton(getString(R.string.got_it), null)
                .show()
        }
        btn_fetch_proposals.setOnClickListener {
            multiWallet?.setBoolConfigValueForKey(Constants.HAS_SHOW_POLITEIA_WELCOME, true)
            startActivity(Intent(this, PoliteiaActivity::class.java))
            finish()
        }
        go_back.setOnClickListener {
            finish()
        }
    }
}