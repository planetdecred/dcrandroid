/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.util;

import dcrlibwallet.LibWallet;

/**
 * Created by collins on 2/24/18.
 */

public class WalletData {
    private static final WalletData ourInstance = new WalletData();
    public LibWallet wallet;
    public boolean synced = false, syncing = true;
    public int peers = 0;

    public int syncStartPoint = -1, syncCurrentPoint = -1, syncEndPoint = -1;
    public double syncProgress = 0, accountDiscoveryStartTime, totalDiscoveryTime;
    public long fetchHeaderTime = -1, totalFetchTime = -1, rescanTime, syncRemainingTime, initialSyncEstimate = -1;
    public String syncStatus, syncVerbose;

    private WalletData() {

    }

    public static WalletData getInstance() {
        return ourInstance;
    }

}
