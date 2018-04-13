package com.dcrandroid.util;

import mobilewallet.BlockNotificationError;
import mobilewallet.LibWallet;
/**
 * Created by collins on 2/24/18.
 */

public class DcrConstants implements BlockNotificationError {
    private static final DcrConstants ourInstance = new DcrConstants();
    public static DcrConstants getInstance() {
        return ourInstance;
    }
    public LibWallet wallet;
    public BlockNotificationError notificationError;
    public BlockNotificationProxy notificationProxy;
    private DcrConstants() {
        this.notificationError = this;
    }

    @Override
    public void onBlockNotificationError(Exception e) {
        if(notificationProxy != null){
            notificationProxy.onBlockNotificationError(e);
        }
    }
}
