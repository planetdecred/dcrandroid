/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.util

import androidx.fragment.app.FragmentActivity
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.fragments.PasswordPinDialogFragment
import dcrlibwallet.Dcrlibwallet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChangePassUtil(private val fragmentActivity: FragmentActivity, val walletID: Long?) : PasswordPinDialogFragment.PasswordPinListener {

    private lateinit var passwordPinDialogFragment: PasswordPinDialogFragment
    private var oldPassphrase = ""

    fun begin() {
        passwordPinDialogFragment = PasswordPinDialogFragment(R.string.change, walletID != null, true, this)

        val title = PassPromptTitle(R.string.confirm_to_change, R.string.confirm_to_change, R.string.confirm_to_change)
        var passPromptUtil: PassPromptUtil? = null
        passPromptUtil = PassPromptUtil(fragmentActivity, walletID, title, false) { _, pass ->
            if (pass != null) {
                oldPassphrase = pass

                passwordPinDialogFragment.tabIndex = if (passPromptUtil!!.passType == Dcrlibwallet.PassphraseTypePass) {
                    0
                } else 1

                passwordPinDialogFragment.show(fragmentActivity)

            }

            return@PassPromptUtil true
        }

        passPromptUtil.show()
    }

    override fun onEnterPasswordOrPin(newPassphrase: String, passphraseType: Int) {
        GlobalScope.launch(Dispatchers.IO) {
            val multiWallet = WalletData.multiWallet!!

            if (walletID == null) {
                try {
                    multiWallet.changePublicPassphrase(oldPassphrase.toByteArray(), newPassphrase.toByteArray())
                    multiWallet.setInt32ConfigValueForKey(Dcrlibwallet.StartupSecurityTypeConfigKey, passphraseType)

                    // saving after a successful change to avoid saving a wrong oldPassphrase
                    BiometricUtils.saveToKeystore(fragmentActivity, newPassphrase, Constants.STARTUP_PASSPHRASE)

                    SnackBar.showText(fragmentActivity, R.string.startup_security_changed)
                } catch (e: Exception) {
                    e.printStackTrace()

                    if (e.message == Dcrlibwallet.ErrInvalidPassphrase) {
                        val passType = multiWallet.readInt32ConfigValueForKey(Dcrlibwallet.StartupSecurityTypeConfigKey, Dcrlibwallet.PassphraseTypePass)
                        showError(passType)
                    }
                }
            } else {
                try {
                    multiWallet.changePrivatePassphraseForWallet(walletID, oldPassphrase.toByteArray(), newPassphrase.toByteArray(), passphraseType)
                    SnackBar.showText(fragmentActivity, R.string.spending_passphrase_changed)
                } catch (e: Exception) {
                    if (e.message == Dcrlibwallet.ErrInvalidPassphrase) {
                        val wallet = multiWallet.walletWithID(walletID)
                        showError(wallet.privatePassphraseType)
                    }
                }

            }

            withContext(Dispatchers.Main) {
                passwordPinDialogFragment.dismiss()
            }

        }
    }

    private fun showError(passType: Int) {
        val err = if (passType == Dcrlibwallet.PassphraseTypePass) {
            R.string.invalid_password
        } else {
            R.string.invalid_pin
        }

        SnackBar.showError(fragmentActivity, err)
    }
}