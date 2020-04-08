/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities

import android.animation.*
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.dcrandroid.BuildConfig
import com.dcrandroid.HomeActivity
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.dialog.CreateWatchOnlyWallet
import com.dcrandroid.dialog.FullScreenBottomSheetDialog
import com.dcrandroid.dialog.InfoDialog
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.show
import com.dcrandroid.fragments.PasswordPinDialogFragment
import com.dcrandroid.util.*
import dcrlibwallet.Dcrlibwallet
import dcrlibwallet.MultiWallet
import kotlinx.android.synthetic.main.activity_setup_page.*
import kotlinx.android.synthetic.main.activity_splash_screen.*
import kotlinx.android.synthetic.main.activity_splash_screen.ll_create_wallet
import kotlinx.android.synthetic.main.activity_splash_screen.ll_restore_wallet
import kotlinx.coroutines.*
import kotlin.system.exitProcess

const val RESTORE_WALLET_REQUEST_CODE = 1

class SplashScreenActivity: BaseActivity() {

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

        setContentView(R.layout.activity_splash_screen)

        ll_create_wallet.setOnClickListener {
            PasswordPinDialogFragment(R.string.create, isSpending = true, isChange = false) { dialog, passphrase, passphraseType ->
                createWallet(dialog, passphrase, passphraseType)
            }.show(this)
        }

        ll_restore_wallet.setOnClickListener {
            val restoreIntent = Intent(this, RestoreWalletActivity::class.java)
            startActivityForResult(restoreIntent, RESTORE_WALLET_REQUEST_CODE)
        }

        if (BuildConfig.IS_TESTNET) {
            tv_testnet.show()
        }

        splashscreen_dcr_symbol.post {
            val symbolAnimation = AnimatedVectorDrawableCompat.create(applicationContext, R.drawable.avd_anim)
            symbolAnimation!!.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable?) {
                    super.onAnimationEnd(drawable)
                    splashscreen_dcr_symbol.postDelayed({ symbolAnimation.start() }, 750)
                }
            })
            splashscreen_dcr_symbol.setImageDrawable(symbolAnimation)
            symbolAnimation.start()
        }

        startup()
    }

    private fun createWallet(dialog: FullScreenBottomSheetDialog, spendingKey: String, type: Int) = GlobalScope.launch(Dispatchers.IO) {
        try {
            multiWallet!!.createNewWallet(spendingKey, type)
            withContext(Dispatchers.Main) {
                dialog.dismiss()
            }
            SnackBar.showText(this@SplashScreenActivity, R.string.wallet_created)
            proceedToHomeActivity()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RESTORE_WALLET_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            finish()
        }
    }

    private fun setLoadingStatus(@StringRes str: Int)=  GlobalScope.launch(Dispatchers.Main){
        loading_status.setText(str)
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

        if (multiWallet!!.loadedWalletsCount() == 0) {

            val v1WalletPath = "$filesDir/$v1WalletDirName/${BuildConfig.NetType}"
            val v1WalletExists = Dcrlibwallet.walletExistsAt(v1WalletPath)

            if (v1WalletExists) {

                setLoadingStatus(R.string.migrating_wallet)

                MigrateV1Wallet(this, v1WalletPath) {
                    proceedToHomeActivity()
                }.beginV1WalletMigration()

            } else {
                showSetupWallet()
            }

        } else {
            if (multiWallet!!.isStartupSecuritySet) {
                requestStartupPass()
            } else {
                openWallet("")
            }
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
                setLoadingStatus(R.string.opening_wallets)
            } else {
                setLoadingStatus(R.string.opening_wallet)
            }

            multiWallet!!.openWallets(publicPass.toByteArray())

            proceedToHomeActivity()

        } catch (e: Exception) {
            e.printStackTrace()

            withContext(Dispatchers.Main) {
                val infoDialog = InfoDialog(this@SplashScreenActivity)
                        .setDialogTitle(getString(R.string.failed_to_open_wallet))
                        .setMessage(Utils.translateError(this@SplashScreenActivity, e))
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

    private fun showSetupWallet() = GlobalScope.launch(Dispatchers.Main){

        delay(3000)
        loading_status.hide()

        splashscreen_dcr_symbol.setImageResource(R.drawable.avd_anim)

        welcome_text.alpha = 0f
        welcome_text.translationY = resources.getDimension(R.dimen.margin_padding_size_40)
        welcome_text.setText(R.string.welcome_screen_text)
        welcome_text.show()

        val pvhX = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f)
        val pvhAlpha = PropertyValuesHolder.ofFloat(View.ALPHA, 1f)
        val objectAnimator = ObjectAnimator.ofPropertyValuesHolder(welcome_text, pvhX, pvhAlpha)


        val bottomMargin = resources.getDimensionPixelOffset(R.dimen.margin_padding_size_180)
        val layoutParams = bottom_bar_layout.layoutParams as LinearLayout.LayoutParams
        layoutParams.bottomMargin = -bottomMargin
        bottom_bar_layout.show()

        val valueAnimator = ValueAnimator.ofInt(bottomMargin, 0)
        valueAnimator.addUpdateListener {
            layoutParams.bottomMargin = -(valueAnimator.animatedValue as Int)
            bottom_bar_layout.requestLayout()
        }

        val animatorSet = AnimatorSet()
        animatorSet.duration = 300
        animatorSet.play(objectAnimator).with(valueAnimator)
        animatorSet.start()
    }

    private fun proceedToHomeActivity() {
        val i = Intent(this@SplashScreenActivity, HomeActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(i)

        // Finish all the activities before this
        ActivityCompat.finishAffinity(this@SplashScreenActivity)
    }

    private fun endProcess() {
        multiWallet?.shutdown()
        finish()
        exitProcess(1)
    }
}