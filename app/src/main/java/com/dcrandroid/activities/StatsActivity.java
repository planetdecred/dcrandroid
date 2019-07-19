/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities;

import android.os.Bundle;
import android.text.format.DateUtils;
import android.widget.TextView;

import com.dcrandroid.BuildConfig;
import com.dcrandroid.R;
import com.dcrandroid.data.Account;
import com.dcrandroid.data.Constants;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.WalletData;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import androidx.appcompat.app.AppCompatActivity;

import dcrlibwallet.Dcrlibwallet;

public class StatsActivity extends AppCompatActivity {

    private TextView chainData;          // Wallet data
    private TextView peersConnected;     // Peers connected to
    private TextView buildName;          // BuildConfigs versionName
    private TextView networkType;        // Nettype mainly testnet3 and mainnet
    private TextView bestBlock;          // Chains best block
    private TextView bestBlockTimestamp; // Best block's time stamp
    private TextView bestBlockAge;       // Time diff between bestBlockTimestamp and now
    private TextView uptime;             // Application uptime
    private TextView transactions;       // Number of transactions executed
    private TextView accounts;           // Number of accounts in the wallet.
    private TextView walletFile;         // Location of wallet file
    private WalletData walletData;
    private PreferenceUtil util;
    private Thread uiUpdateThread;
    private final String TAG  = getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);
        util = new PreferenceUtil(this);
        walletData = WalletData.getInstance(); // walletData singleton instance.

        chainData = findViewById(R.id.stats_chain_data_value);
        peersConnected = findViewById(R.id.stats_peers_connected_value);
        buildName = findViewById(R.id.stats_build_value);
        networkType = findViewById(R.id.stats_network_value);
        bestBlock = findViewById(R.id.stats_best_block_value);
        bestBlockTimestamp = findViewById(R.id.stats_block_timestamp_value);
        bestBlockAge = findViewById(R.id.stats_block_age_value);
        uptime = findViewById(R.id.stats_uptime_value);
        transactions = findViewById(R.id.stats_transactions_value);
        accounts = findViewById(R.id.stats_accounts_value);
        walletFile = findViewById(R.id.stats_wallet_file_value);

        // Set static values
        buildName.setText(BuildConfig.VERSION_NAME);
        networkType.setText(BuildConfig.NetType);
        walletFile.setText(getFilesDir() + "/wallet/" + BuildConfig.NetType + "/walleb.db");

        startUIUpdate();
    }

    // Update peer count on the UI.
    private void updatePeerCount() {
        if (!walletData.synced && !walletData.wallet.isSyncing()) {
            peersConnected.setText(R.string.not_synced);
            return;
        }
        // Update the peer count only if wallet is not syncing else show syncing text.
        if (!walletData.wallet.isSyncing()) {
            if (walletData.peers == 1) {
                peersConnected.setText(1 + " Peer");
            } else {
                peersConnected.setText(String.valueOf(walletData.peers) + " Peers");
            }
        } else {
            if (walletData.peers == 1) {
                peersConnected.setText(R.string.syncing_with_one_peer);
            } else {
                peersConnected.setText(getString(R.string.synced_with_multiple_peer, walletData.peers));
            }
        }
    }

    @Override
    protected void onStop(){
        super.onStop();
        if(uiUpdateThread != null){
            // Interrupt the thread driving Ui values
            uiUpdateThread.interrupt();
        }
    }

    public static String floatForm (double d)
    {
        return new DecimalFormat("#.##").format(d);
    }


    // Convert disk size in bytes to human format
    public static String bytesToHuman (long size)
    {
        long Kb = 1  * 1024;
        long Mb = Kb * 1024;
        long Gb = Mb * 1024;
        long Tb = Gb * 1024;
        long Pb = Tb * 1024;
        long Eb = Pb * 1024;

        if (size <  Kb)                 return floatForm(        size     ) + " byte";
        if (size >= Kb && size < Mb)    return floatForm((double)size / Kb) + " Kb";
        if (size >= Mb && size < Gb)    return floatForm((double)size / Mb) + " Mb";
        if (size >= Gb && size < Tb)    return floatForm((double)size / Gb) + " Gb";
        if (size >= Tb && size < Pb)    return floatForm((double)size / Tb) + " Tb";
        if (size >= Pb && size < Eb)    return floatForm((double)size / Pb) + " Pb";
        if (size >= Eb)                 return floatForm((double)size / Eb) + " Eb";

        return "???";
    }


    private long getFileSize(){
        String homeDir = getFilesDir() + "/wallet";
        String walletDB;

        // Check if we are running testnet
        if (BuildConfig.IS_TESTNET) {
            walletDB = homeDir + "/testnet3/wallet.db";
        } else {
            walletDB = homeDir + "/mainnet/wallet.db";
        }
        return dirSize(new File(walletDB));
    }


    // Return the size of a dir in bytes
    private long dirSize(File dir) {

        if (dir.exists()) {
            long result = 0;
            File[] fileList = dir.listFiles();
            for(int i = 0; i < fileList.length; i++) {
                // Recursive call if it's a directory
                if(fileList[i].isDirectory()) {
                    result += dirSize(fileList[i]);
                } else {
                    // Sum the file size in bytes
                    result += fileList[i].length();
                }

            }
            return result; // return the file size
        }
        return 0;
    }

    // Updates the UI. Drives values from a different thread to avoid blocking UI thread.
    private void startUIUpdate(){
        if (uiUpdateThread != null) {
            return;
        }

        uiUpdateThread = new Thread() {
            public void run() {
                while (!this.isInterrupted()) {
                    try {
                        final Date date = new Date(walletData.wallet.getBestBlockTimeStamp() * 1000L);
                        final String timeAgoText = DateUtils.getRelativeTimeSpanString(System.currentTimeMillis(), date.getTime(), 0L, DateUtils.FORMAT_ABBREV_ALL).toString()
                                .replace("ago", "").replace("In", "").trim();
                        Long startupTime = util.getLong(Constants.STARTUP_TIME, 0L);
                        final String uptimeText = getTimeDiff(startupTime, System.currentTimeMillis());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                bestBlock.setText(String.valueOf(walletData.wallet.getBestBlock()));
                                bestBlockTimestamp.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").format(date));
                                bestBlockAge.setText(timeAgoText);
                                uptime.setText(uptimeText);
                                chainData.setText(String.valueOf(bytesToHuman(getFileSize())));
                                try{
                                    accounts.setText(String.valueOf(Account.parse(walletData.wallet.getAccounts(util.getBoolean(Constants.SPEND_UNCONFIRMED_FUNDS) ? 0 : Constants.REQUIRED_CONFIRMATIONS)).size()));
                                    transactions.setText(String.valueOf(walletData.wallet.countTransactions(Dcrlibwallet.TxFilterAll)));
                                }catch (Exception ex){

                                }
                                updatePeerCount();
                            }
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            }
        };
        uiUpdateThread.start();

    }

    // Returns the time elapsed between startTime and currenTime in the format HH:mm:ss
    private String getTimeDiff(long startTime, long currentTime){
        long mills = currentTime - startTime;
        int hours = (int) (mills/(1000 * 60 * 60));
        int mins = (int) (mills/(1000*60)) % 60;
        int sec = (int) (mills/1000) % 60;
        String hoursText = String.valueOf(hours);
        String minsText = String.valueOf(mins);
        String secText =  String.valueOf(sec);
        if(sec < 10){
            secText = "0"+ sec;
        }
        if(mins < 10){
            minsText = "0" + minsText;
        }
        if(hours < 10){
            hoursText = "0" + hoursText;
        }
        return hoursText + ":" + minsText + ":" + secText;
    }

}
