/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.dcrandroid.extensions.openedWalletsList
import com.dcrandroid.util.WalletData
import dcrlibwallet.AccountMixerNotificationListener
import dcrlibwallet.MultiWallet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@SuppressLint("Registered")
open class BaseActivity : AppCompatActivity(), AccountMixerNotificationListener {

    internal val walletData: WalletData = WalletData.instance
    internal val multiWallet: MultiWallet?
        get() = walletData.multiWallet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val decorView = window.decorView
            decorView.systemUiVisibility = WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        } else {
            window.navigationBarColor = ContextCompat.getColor(this, android.R.color.black)
        }

        checkMixerStatus()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val view = currentFocus
        if (view != null && (ev.action == MotionEvent.ACTION_UP || ev.action == MotionEvent.ACTION_MOVE) && view is EditText && !view.javaClass.name.startsWith("android.webkit.")) {
            view.clearFocus()
            val scrcoords = IntArray(2)
            view.getLocationOnScreen(scrcoords)
            val x = ev.rawX + view.left - scrcoords[0]
            val y = ev.rawY + view.top - scrcoords[1]
            if (x < view.left || x > view.right || y < view.top || y > view.bottom)
                (this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(this.window.decorView.applicationWindowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun checkMixerStatus() = GlobalScope.launch(Dispatchers.Main) {
        var activeMixers = 0
        for (wallet in multiWallet!!.openedWalletsList()) {
            if (wallet.isAccountMixerActive) {
                activeMixers++
            }
        }

        if (activeMixers > 0) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onResume() {
        super.onResume()
        multiWallet!!.removeAccountMixerNotificationListener(this.javaClass.name)
        multiWallet!!.addAccountMixerNotificationListener(this, this.javaClass.name)
        checkMixerStatus()
    }

    override fun onPause() {
        super.onPause()
        multiWallet!!.removeAccountMixerNotificationListener(this.javaClass.name)
    }

    override fun onAccountMixerEnded(walletID: Long) {
        checkMixerStatus()
    }

    override fun onAccountMixerStarted(walletID: Long) {
        checkMixerStatus()
    }
}