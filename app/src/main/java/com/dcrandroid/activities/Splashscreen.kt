/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities

import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView

import androidx.core.app.ActivityCompat
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat

import com.dcrandroid.BuildConfig
import com.dcrandroid.HomeActivity
import com.dcrandroid.R
import com.dcrandroid.activities.more.SettingsActivity
import com.dcrandroid.data.Constants
import com.dcrandroid.dialog.InfoDialog
import com.dcrandroid.extensions.show
import com.dcrandroid.util.PassPromptTitle
import com.dcrandroid.util.PassPromptUtil
import com.dcrandroid.util.PreferenceUtil
import com.dcrandroid.util.Utils

import dcrlibwallet.Dcrlibwallet
import dcrlibwallet.MultiWallet
import kotlin.system.exitProcess

/**
 * Created by Macsleven on 24/12/2017.
 */

const val DOUBLE_CLICK_TIME_DELTA: Long = 300 //milliseconds

class SplashScreen : BaseActivity() {

    private var imgAnim: ImageView? = null
    private var util: PreferenceUtil? = null
    private var tvLoading: TextView? = null
    private var loadThread: Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isTaskRoot) {
            val intent = intent
            val intentAction = intent.action
            if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && intentAction != null && intentAction == Intent.ACTION_MAIN) {
                finish()
                return
            }
        }

        util = PreferenceUtil(this)
        setContentView(R.layout.splash_page)

        if (BuildConfig.IS_TESTNET) {
            findViewById<TextView>(R.id.tv_testnet).show()
        }

        imgAnim = findViewById(R.id.splashscreen_icon)
        imgAnim!!.setOnClickListener(object : DoubleClickListener() {
            override fun onSingleClick(v: View) {

            }

            override fun onDoubleClick(v: View) {
                if (loadThread != null) {
                    loadThread!!.interrupt()
                }

                if (walletData.multiWallet != null) {
                    walletData.multiWallet!!.shutdown()
                }

                val intent = Intent(applicationContext, SettingsActivity::class.java)
                startActivityForResult(intent, 2)
            }
        })

        imgAnim!!.post {
            val anim = AnimatedVectorDrawableCompat.create(applicationContext, R.drawable.avd_anim)
            anim!!.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable?) {
                    super.onAnimationEnd(drawable)
                    imgAnim!!.postDelayed({ anim.start() }, 750)
                }
            })
            imgAnim!!.setImageDrawable(anim)
            anim.start()
        }

        tvLoading = findViewById(R.id.loading_status)
        startup()
    }

    private fun startup() {

        try {
            if (walletData.multiWallet != null) {
                walletData.multiWallet!!.shutdown()
            }

            walletData.multiWallet = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val homeDir = "$filesDir/wallets"
        walletData.multiWallet = MultiWallet(homeDir, Constants.BADGER_DB, BuildConfig.NetType)

        val logLevels = resources.getStringArray(R.array.logging_levels)
        val logLevelIndex = walletData.multiWallet!!.readInt32ConfigValueForKey(Dcrlibwallet.LogLevelConfigKey, Constants.DEF_LOG_LEVEL)
        Dcrlibwallet.setLogLevels(logLevels[logLevelIndex])

        if (walletData.multiWallet!!.loadedWalletsCount() == 0) {
            loadThread = object : Thread() {
                override fun run() {
                    try {
                        sleep(3000)
                        createWallet()
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }

                }
            }
            loadThread!!.start()
        } else {
            checkStartupPass()
        }
    }

    private fun setText(str: String) {
        runOnUiThread { tvLoading!!.text = str }
    }

    private fun createWallet() {
        val i = Intent(this@SplashScreen, SetupWalletActivity::class.java)
        startActivity(i)
        finish()
    }

    private fun checkStartupPass() {
        if (multiWallet!!.readBoolConfigValueForKey(Dcrlibwallet.IsStartupSecuritySetConfigKey, Constants.DEF_STARTUP_SECURITY_SET)) {
            requestStartupPass()
        } else {
            openWallet(Constants.INSECURE_PUB_PASSPHRASE)
        }
    }

    private fun requestStartupPass(){
        val title = PassPromptTitle(R.string.startup_password_prompt_title, R.string.startup_pin_prompt_title, R.string.startup_fingerprint_prompt_title)
        PassPromptUtil(this, null, title, allowFingerprint = true) { _, pass ->
            if(pass != null){
                openWallet(pass)
            }else{
                endProcess()
            }

            true
        }.show()
    }

    private fun openWallet(publicPass: String) {
        loadThread = object : Thread() {
            override fun run() {
                try {
                    if (walletData.multiWallet!!.loadedWalletsCount() > 1) {
                        setText(getString(R.string.opening_wallets))
                    } else {
                        setText(getString(R.string.opening_wallet))
                    }

                    walletData.multiWallet!!.openWallets(publicPass.toByteArray())

                    val i = Intent(this@SplashScreen, HomeActivity::class.java)
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(i)
                    //Finish all the activities before this
                    ActivityCompat.finishAffinity(this@SplashScreen)

                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread {
                        val infoDialog = InfoDialog(this@SplashScreen)
                                .setDialogTitle(getString(R.string.failed_to_open_wallet))
                                .setMessage(Utils.translateError(this@SplashScreen, e))
                                .setPositiveButton(getString(R.string.exit_cap), DialogInterface.OnClickListener { _, _ -> endProcess() })

                        if (e.message == Dcrlibwallet.ErrInvalidPassphrase) {
                            if (multiWallet!!.readInt32ConfigValueForKey(Dcrlibwallet.StartupSecurityTypeConfigKey, Dcrlibwallet.PassphraseTypePass)
                                    == Dcrlibwallet.PassphraseTypePin) {
                                infoDialog.setMessage(getString(R.string.invalid_pin))
                            }
                            infoDialog.setNegativeButton(getString(R.string.exit_cap), DialogInterface.OnClickListener { _, _ -> endProcess() })
                                    .setPositiveButton(getString(R.string.retry_caps), DialogInterface.OnClickListener { _, _ ->
                                        requestStartupPass()
                            })
                        }

                        infoDialog.setCancelable(false)
                        infoDialog.setCanceledOnTouchOutside(false)
                        infoDialog.show()
                    }
                }

            }
        }
        loadThread!!.start()
    }

    private fun endProcess(){
        multiWallet?.shutdown()
        finish()
        exitProcess(1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2) {
            startup()
        }
    }

    abstract inner class DoubleClickListener : View.OnClickListener {

        private var lastClickTime: Long = 0

        override fun onClick(v: View) {
            val clickTime = System.currentTimeMillis()
            if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
                onDoubleClick(v)
                lastClickTime = 0
            } else {
                onSingleClick(v)
            }
            lastClickTime = clickTime
        }

        abstract fun onSingleClick(v: View)

        abstract fun onDoubleClick(v: View)

    }
}