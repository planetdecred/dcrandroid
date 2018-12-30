package com.dcrandroid.util;

import dcrlibwallet.LibWallet;

/**
 * Created by collins on 2/24/18.
 */

public class DcrConstants {
    private static final DcrConstants ourInstance = new DcrConstants();
    public LibWallet wallet;
    public boolean synced = false;
    public int peers = 0;

    public int syncStartPoint = -1, syncCurrentPoint = -1, syncEndPoint = -1;
    public long syncStartTime;

    private DcrConstants() {

    }

    public static DcrConstants getInstance() {
        return ourInstance;
    }

}
