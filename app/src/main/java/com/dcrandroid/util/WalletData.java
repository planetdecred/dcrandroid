package com.dcrandroid.util;

import dcrlibwallet.LibWallet;

/**
 * Created by collins on 2/24/18.
 */

public class WalletData {
    private static final WalletData ourInstance = new WalletData();
    public LibWallet wallet;
    public boolean synced = false, syncing = false;
    public int peers = 0;

    public int syncStartPoint = -1, syncCurrentPoint = -1, syncEndPoint = -1;
    public double syncProgress = 0, accountDiscoveryTime;
    public long fetchHeaderTime = -1, totalFetchTime = -1, rescanTime, syncRemainingTime, initialSyncEstimate = -1, syncStartTime;
    public String syncStatus;

    private WalletData() {

    }

    public static WalletData getInstance() {
        return ourInstance;
    }

}
