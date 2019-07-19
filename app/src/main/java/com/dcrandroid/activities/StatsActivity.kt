/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities

import android.os.Bundle
import android.text.format.DateUtils
import android.widget.TextView

import com.dcrandroid.BuildConfig
import com.dcrandroid.R
import com.dcrandroid.data.Account
import com.dcrandroid.data.Constants
import com.dcrandroid.util.PreferenceUtil
import com.dcrandroid.util.WalletData

import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date

import androidx.appcompat.app.AppCompatActivity

import dcrlibwallet.Dcrlibwallet

class StatsActivity : AppCompatActivity() {

    private var chainData: TextView? = null          // Wallet data
    private var peersConnected: TextView? = null     // Peers connected to
    private var buildName: TextView? = null          // BuildConfigs versionName
    private var networkType: TextView? = null        // Nettype mainly testnet3 and mainnet
    private var bestBlock: TextView? = null          // Chains best block
    private var bestBlockTimestamp: TextView? = null // Best block's time stamp
    private var bestBlockAge: TextView? = null       // Time diff between bestBlockTimestamp and now
    private var uptime: TextView? = null             // Application uptime
    private var transactions: TextView? = null       // Number of transactions executed
    private var accounts: TextView? = null           // Number of accounts in the wallet.
    private var walletFile: TextView? = null         // Location of wallet file
    private var walletData: WalletData? = null
    private var util: PreferenceUtil? = null
    private var uiUpdateThread: Thread? = null
    private val TAG = javaClass.simpleName


    private
    val fileSize: Long
        get() {
            val homeDir = "$filesDir/wallet"
            val walletDB: String
            if (BuildConfig.IS_TESTNET) {
                walletDB = "$homeDir/testnet3/wallet.db"
            } else {
                walletDB = "$homeDir/mainnet/wallet.db"
            }
            return dirSize(File(walletDB))
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)
        util = PreferenceUtil(this)
        walletData = WalletData.getInstance() // walletData singleton instance.

        chainData = findViewById(R.id.stats_chain_data_value)
        peersConnected = findViewById(R.id.stats_peers_connected_value)
        buildName = findViewById(R.id.stats_build_value)
        networkType = findViewById(R.id.stats_network_value)
        bestBlock = findViewById(R.id.stats_best_block_value)
        bestBlockTimestamp = findViewById(R.id.stats_block_timestamp_value)
        bestBlockAge = findViewById(R.id.stats_block_age_value)
        uptime = findViewById(R.id.stats_uptime_value)
        transactions = findViewById(R.id.stats_transactions_value)
        accounts = findViewById(R.id.stats_accounts_value)
        walletFile = findViewById(R.id.stats_wallet_file_value)

        // Set static values
        buildName!!.text = BuildConfig.VERSION_NAME
        networkType!!.text = BuildConfig.NetType
        walletFile!!.text = filesDir.toString() + "/wallet/" + BuildConfig.NetType + "/walleb.db"

        startUIUpdate()
    }

    // Update peer count on the UI.
    private fun updatePeerCount() {
        if (!walletData!!.synced && !walletData!!.wallet.isSyncing) {
            peersConnected!!.setText(R.string.not_synced)
            return
        }
        // Update the peer count only if wallet is not syncing else show syncing text.
        if (!walletData!!.wallet.isSyncing) {
            if (walletData!!.peers == 1) {
                peersConnected!!.text = 1.toString() + " Peer"
            } else {
                peersConnected!!.text = walletData!!.peers.toString() + " Peers"
            }
        } else {
            if (walletData!!.peers == 1) {
                peersConnected!!.setText(R.string.syncing_with_one_peer)
            } else {
                peersConnected!!.text = getString(R.string.synced_with_multiple_peer, walletData!!.peers)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (uiUpdateThread != null) {
            // Interrupt the thread driving Ui values
            uiUpdateThread!!.interrupt()
        }
    }


    // Return the size of a dir in bytes
    private fun dirSize(dir: File): Long {

        if (dir.exists()) {
            var result: Long = 0
            val fileList = dir.listFiles()
            for (i in fileList.indices) {
                // Recursive call if it's a directory
                if (fileList[i].isDirectory) {
                    result += dirSize(fileList[i])
                } else {
                    // Sum the file size in bytes
                    result += fileList[i].length()
                }

            }
            return result // return the file size
        }
        return 0
    }

    // Updates the UI. Drives values from a different thread to avoid blocking UI thread.
    private fun startUIUpdate() {
        if (uiUpdateThread != null) {
            return
        }

        uiUpdateThread = object : Thread() {
            override fun run() {
                while (!this.isInterrupted) {
                    try {
                        val date = Date(walletData!!.wallet.bestBlockTimeStamp * 1000L)
                        val timeAgoText = DateUtils.getRelativeTimeSpanString(System.currentTimeMillis(), date.time, 0L, DateUtils.FORMAT_ABBREV_ALL).toString()
                                .replace("ago", "").replace("In", "").trim { it <= ' ' }
                        val startupTime = util!!.getLong(Constants.STARTUP_TIME, 0L)
                        val uptimeText = getTimeDiff(startupTime, System.currentTimeMillis())
                        runOnUiThread {
                            bestBlock!!.text = walletData!!.wallet.bestBlock.toString()
                            bestBlockTimestamp!!.text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").format(date)
                            bestBlockAge!!.text = timeAgoText
                            uptime!!.text = uptimeText
                            chainData!!.text = bytesToHuman(fileSize)
                            try {
                                accounts!!.text = Account.parse(walletData!!.wallet.getAccounts(if (util!!.getBoolean(Constants.SPEND_UNCONFIRMED_FUNDS)) 0 else Constants.REQUIRED_CONFIRMATIONS)).size.toString()
                                transactions!!.text = walletData!!.wallet.countTransactions(Dcrlibwallet.TxFilterAll).toString()
                            } catch (ex: Exception) {

                            }

                            updatePeerCount()
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                        return
                    }

                    try {
                        Thread.sleep(5000)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                        return
                    }

                }
            }
        }
        uiUpdateThread!!.start()

    }

    // Returns the time elapsed between startTime and currenTime in the format HH:mm:ss
    private fun getTimeDiff(startTime: Long, currentTime: Long): String {
        val mills = currentTime - startTime
        val hours = (mills / (1000 * 60 * 60)).toInt()
        val mins = (mills / (1000 * 60)).toInt() % 60
        val sec = (mills / 1000).toInt() % 60
        var hoursText = hours.toString()
        var minsText = mins.toString()
        var secText = sec.toString()
        if (sec < 10) {
            secText = "0$sec"
        }
        if (mins < 10) {
            minsText = "0$minsText"
        }
        if (hours < 10) {
            hoursText = "0$hoursText"
        }
        return "$hoursText:$minsText:$secText"
    }

    companion object {

        fun floatForm(d: Double): String {
            return DecimalFormat("#.##").format(d)
        }


        // Convert disk size in bytes to human format
        fun bytesToHuman(size: Long): String {
            val Kb = (1 * 1024).toLong()
            val Mb = Kb * 1024
            val Gb = Mb * 1024

            if (size < Kb) return floatForm(size.toDouble()) + " byte"
            if (size >= Kb && size < Mb) return floatForm(size.toDouble() / Kb) + " Kb"
            return if (size >= Mb && size < Gb) floatForm(size.toDouble() / Mb) + " Mb" else "???"

        }
    }

}
