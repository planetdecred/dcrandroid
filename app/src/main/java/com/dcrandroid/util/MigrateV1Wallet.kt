/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.util

import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import com.dcrandroid.BuildConfig
import com.dcrandroid.R
import com.dcrandroid.dialog.FullScreenBottomSheetDialog
import com.dcrandroid.dialog.InfoDialog
import com.dcrandroid.dialog.PasswordPromptDialog
import com.dcrandroid.dialog.PinPromptDialog
import dcrlibwallet.Dcrlibwallet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

const val walletsDirName = "wallets"
const val v1WalletDirName = "wallet"

class MigrateV1Wallet(val activity: AppCompatActivity, val v1WalletPath: String, val migrationComplete: () -> Unit) {

    private val multiWallet = WalletData.multiWallet

    private var preferenceUtil = V1PreferenceUtil.with(activity)

    lateinit var publicPassType: String

    fun beginV1WalletMigration() {
        publicPassType = preferenceUtil.getString(KEY_STARTUP_PASSPHRASE_TYPE, KEY_PASSWORD)

        if (preferenceUtil.getBoolean(KEY_ENCRYPT, false)) {
            requestV1WalletPublicPassphrase()
        } else {
            completeV1WalletMigration(KEY_INSECURE_PUB_PASSPHRASE)
        }
    }

    private fun requestV1WalletPublicPassphrase() {

        val passEntered = { _: FullScreenBottomSheetDialog, passphrase: String? ->
            if (passphrase != null) {
                completeV1WalletMigration(passphrase)
            } else {
                activity.finish()
            }

            true
        }

        if (publicPassType == KEY_PASSWORD) {
            val passwordPromptDialog = PasswordPromptDialog(R.string.startup_password_prompt_title, false, passEntered)
            passwordPromptDialog.isCancelable = false
            passwordPromptDialog.show(activity)
        } else {
            val pinPromptDialog = PinPromptDialog(R.string.startup_pin_prompt_title, false, passEntered)
            pinPromptDialog.isCancelable = false
            pinPromptDialog.show(activity)
        }
    }

    private fun completeV1WalletMigration(passphrase: String) = GlobalScope.launch(Dispatchers.IO) {
        val privatePassphraseType = when (preferenceUtil.getString(KEY_SPENDING_PASSPHRASE_TYPE, KEY_PASSWORD)) {
            KEY_PASSWORD -> Dcrlibwallet.PassphraseTypePass
            else -> Dcrlibwallet.PassphraseTypePin
        }

        val peerIP = preferenceUtil.getString(KEY_PEER_IP, "")

        try {
            multiWallet!!.linkExistingWallet(v1WalletPath, passphrase, privatePassphraseType)
            multiWallet.setStringConfigValueForKey(Dcrlibwallet.SpvPersistentPeerAddressesConfigKey, peerIP)

            val transactionsFolder = File(activity.filesDir, BuildConfig.NetType)
            if (transactionsFolder.exists()) {
                Utils.deleteDir(transactionsFolder)
            }

            val oldWalletFolder = File(activity.filesDir, v1WalletDirName)
            if (oldWalletFolder.exists()) {
                Utils.deleteDir(oldWalletFolder)
            }

            migrationComplete()
        } catch (e: Exception) {
            e.printStackTrace()
            if (e.message == Dcrlibwallet.ErrInvalidPassphrase) {
                val errMessage = if (publicPassType == KEY_PASSWORD) {
                    R.string.invalid_password
                } else {
                    R.string.invalid_pin
                }

                SnackBar.showError(activity, errMessage)

                requestV1WalletPublicPassphrase() // ask for passphrase again
            } else {
                withContext(Dispatchers.Main) {
                    InfoDialog(activity)
                            .setMessage(e.message)
                            .setPositiveButton(activity.getString(R.string.exit_cap), DialogInterface.OnClickListener { _, _ ->
                                activity.finish()
                            }).show()
                }
            }
        }
    }
}