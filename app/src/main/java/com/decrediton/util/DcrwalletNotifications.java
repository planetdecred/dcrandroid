package com.decrediton.util;

/**
 * Created by collins on 1/30/18.
 */

public class DcrwalletNotifications {
    private static final DcrwalletNotifications ourInstance = new DcrwalletNotifications();

    public static DcrwalletNotifications getInstance() {
        return ourInstance;
    }

    private DcrwalletNotifications() {
    }


}
