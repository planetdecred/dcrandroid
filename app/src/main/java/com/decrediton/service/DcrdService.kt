package com.decrediton.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import com.decrediton.R
import dcrwallet.Dcrwallet

class DcrdService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        var notification : Notification = NotificationCompat.Builder(this).setContentTitle("Decred Wallet")
                .setContentText("Chain server is running")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notification.category = Notification.CATEGORY_SERVICE
        }
        startForeground(1, notification)
        Dcrwallet.runDrcd()
    }
}
