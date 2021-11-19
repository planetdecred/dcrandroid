/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities.more

import android.content.Intent
import android.os.Bundle
import com.dcrandroid.R
import com.dcrandroid.activities.BaseActivity
import com.dcrandroid.activities.LogViewer
import com.dcrandroid.data.Constants
import com.dcrandroid.extensions.hide
import com.dcrandroid.preference.ListPreference
import dcrlibwallet.Dcrlibwallet
import kotlinx.android.synthetic.main.activity_debug.*
import kotlinx.android.synthetic.main.activity_debug.view.*

class DebugActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug)

        setLogLevelSummary(
            multiWallet!!.readInt32ConfigValueForKey(
                Dcrlibwallet.LogLevelConfigKey,
                Constants.DEF_LOG_LEVEL
            )
        )
        ListPreference(
            this, Dcrlibwallet.LogLevelConfigKey, Constants.DEF_LOG_LEVEL,
            R.array.logging_levels, logging_level
        ) {
            setLogLevelSummary(it)
        }

        check_statistics.setOnClickListener {
            startActivity(Intent(this, StatisticsActivity::class.java))
        }

        if (multiWallet!!.isConnectedToDecredNetwork) {
            view_connected_peers.setOnClickListener {
                startActivity(Intent(this, ConnectedPeers::class.java))
            }
        } else {
            view_connected_peers.hide()
        }

        check_wallet_log.setOnClickListener {
            startActivity(Intent(this, LogViewer::class.java))
        }

        go_back.setOnClickListener {
            finish()
        }
    }

    private fun setLogLevelSummary(index: Int) {
        val logLevels = resources.getStringArray(R.array.logging_levels)
        logging_level.pref_subtitle.text = logLevels[index]
        Dcrlibwallet.setLogLevels(logLevels[index])
    }
}