/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities.more

import android.os.Bundle
import android.text.format.DateUtils
import android.text.format.Formatter
import com.dcrandroid.BuildConfig
import com.dcrandroid.MainApplication
import com.dcrandroid.R
import com.dcrandroid.activities.BaseActivity
import com.dcrandroid.extensions.openedWalletsList
import com.dcrandroid.util.TimeUtils
import com.dcrandroid.util.walletsDirName
import dcrlibwallet.Dcrlibwallet
import kotlinx.android.synthetic.main.activity_statistics.*
import java.util.*

class StatisticsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        go_back.setOnClickListener { finish() }

        stats_scroll_view.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            app_bar.elevation = if (scrollY > 0) {
                resources.getDimension(R.dimen.app_bar_elevation)
            } else {
                0f
            }
        }

        stats_version_name.text = BuildConfig.VERSION_NAME
        stats_connected_peers.text = multiWallet!!.connectedPeers().toString()

        val currentTimeSeconds = System.currentTimeMillis() / 1000

        val durationUpTime = currentTimeSeconds - MainApplication.appUpTimeSeconds
        stats_uptime.text = DateUtils.formatElapsedTime(durationUpTime)

        stats_network.text = BuildConfig.NetType

        val bestBlock = multiWallet!!.bestBlock
        stats_best_block.text = bestBlock.height.toString()
        stats_best_block_timestamp.text = Date(bestBlock.timestamp * 1000).toString()

        val lastBlockRelativeTime = currentTimeSeconds - bestBlock.timestamp
        stats_best_block_age.text = TimeUtils.calculateTime(lastBlockRelativeTime, this)

        stats_wallet_data_directory.text = "$filesDir/$walletsDirName"
        stats_wallet_data.text = Formatter.formatFileSize(this, multiWallet!!.rootDirFileSizeInBytes())
        stats_transaction_count.text = countAllWalletsTransactions().toString()
        stats_wallet_count.text = multiWallet!!.openedWalletsCount().toString()
    }

    private fun countAllWalletsTransactions(): Long {
        var count = 0L
        multiWallet!!.openedWalletsList()
                .forEach { count += it.countTransactions(Dcrlibwallet.TxFilterAll) }
        return count
    }
}