package com.dcrandroid.util;

/**
 * Created by collins on 2/24/18.
 */

public class DcrNotifications {
    private static final DcrNotifications ourInstance = new DcrNotifications();

    public static DcrNotifications getInstance() {
        return ourInstance;
    }

    private DcrNotifications() {
    }
}
