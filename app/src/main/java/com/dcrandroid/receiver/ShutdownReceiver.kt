package com.dcrandroid.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ShutdownReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        System.exit(0)
    }
}
