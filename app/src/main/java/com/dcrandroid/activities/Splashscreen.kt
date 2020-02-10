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

import androidx.core.app.ActivityCompat
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat

import com.dcrandroid.BuildConfig
import com.dcrandroid.HomeActivity
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.dialog.InfoDialog
import com.dcrandroid.extensions.show
import com.dcrandroid.util.*

import dcrlibwallet.Dcrlibwallet
import dcrlibwallet.MultiWallet
import kotlinx.android.synthetic.main.splash_page.*
import kotlinx.coroutines.*
import kotlin.system.exitProcess

/**
 * Created by Macsleven on 24/12/2017.
 */

class SplashScreen : BaseActivity() {

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

        setContentView(R.layout.splash_page)

        if (BuildConfig.IS_TESTNET) {
            tv_testnet.show()
        }

        splashscreen_dcr_symbol.post {
            val anim = AnimatedVectorDrawableCompat.create(applicationContext, R.drawable.avd_anim)
            anim!!.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable?) {
                    super.onAnimationEnd(drawable)
                    splashscreen_dcr_symbol.postDelayed({ anim.start() }, 750)
                }
            })
            splashscreen_dcr_symbol.setImageDrawable(anim)
            anim.start()
        }

        startup()
    }

    private fun setText(str: String) = GlobalScope.launch(Dispatchers.Main) {
        loading_status.text = str
    }

    private fun startup() {

        try {
            if (multiWallet != null) {
                multiWallet!!.shutdown()
            }

            walletData.multiWallet = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val homeDir = "$filesDir/$walletsDirName"
        walletData.multiWallet = MultiWallet(homeDir, Constants.BADGER_DB, BuildConfig.NetType)

        val logLevels = resources.getStringArray(R.array.logging_levels)
        val logLevelIndex = multiWallet!!.readInt32ConfigValueForKey(Dcrlibwallet.LogLevelConfigKey, Constants.DEF_LOG_LEVEL)
        Dcrlibwallet.setLogLevels(logLevels[logLevelIndex])

        if (multiWallet!!.loadedWalletsCount() == 0) {

            val v1WalletPath = "$filesDir/$v1WalletDirName/${BuildConfig.NetType}"
            val v1WalletExists = Dcrlibwallet.walletExistsAt(v1WalletPath)

            if (v1WalletExists) {

                setText(getString(R.string.migrating_wallet))

                MigrateV1Wallet(this, v1WalletPath) {
                    proceedToHomeActivity()
                }.beginV1WalletMigration()

            } else {
                launchSetupWalletActivity()
            }

        } else {
            checkStartupPass()
        }
    }

    private fun launchSetupWalletActivity() = GlobalScope.launch(Dispatchers.Default) {
        delay(3000)
        val i = Intent(this@SplashScreen, SetupWalletActivity::class.java)
        startActivity(i)
        finish()
    }

    private fun checkStartupPass() {
        if (multiWallet!!.isStartupSecuritySet) {
            requestStartupPass()
        } else {
            openWallet("")
        }
    }

    private fun requestStartupPass() {
        val title = PassPromptTitle(R.string.startup_password_prompt_title, R.string.startup_pin_prompt_title, R.string.startup_fingerprint_prompt_title)
        PassPromptUtil(this, null, title, allowFingerprint = true) { _, pass ->
            if (pass != null) {
                openWallet(pass)
            } else {
                endProcess()
            }

            true
        }.show()
    }

    private fun openWallet(publicPass: String) = GlobalScope.launch(Dispatchers.IO) {
        try {
            if (multiWallet!!.loadedWalletsCount() > 1) {
                setText(getString(R.string.opening_wallets))
            } else {
                setText(getString(R.string.opening_wallet))
            }

            multiWallet!!.openWallets(publicPass.toByteArray())

            proceedToHomeActivity()

        } catch (e: Exception) {
            e.printStackTrace()

            withContext(Dispatchers.Main) {
                val infoDialog = InfoDialog(this@SplashScreen)
                        .setDialogTitle(getString(R.string.failed_to_open_wallet))
                        .setMessage(Utils.translateError(this@SplashScreen, e))
                        .setPositiveButton(getString(R.string.exit_cap), DialogInterface.OnClickListener { _, _ -> endProcess() })

                if (e.message == Dcrlibwallet.ErrInvalidPassphrase) {

                    if (multiWallet!!.startupSecurityType() == Dcrlibwallet.PassphraseTypePin) {
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

    private fun proceedToHomeActivity() {
        val i = Intent(this@SplashScreen, HomeActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(i)

        //Finish all the activities before this
        ActivityCompat.finishAffinity(this@SplashScreen)
    }

    private fun endProcess() {
        multiWallet?.shutdown()
        finish()
        exitProcess(1)
    }
}