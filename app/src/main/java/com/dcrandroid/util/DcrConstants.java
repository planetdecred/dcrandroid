package com.dcrandroid.util;

import mobilewallet.LibWallet;

/**
 * Created by collins on 2/24/18.
 */

public class DcrConstants {
    private static final DcrConstants ourInstance = new DcrConstants();
    public LibWallet wallet;
    public boolean synced = false;
    public int peers = 0;
    private DcrConstants() {

    }

    public static DcrConstants getInstance() {
        return ourInstance;
    }

}
