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
    private var passphrase = ""

    fun begin(){
        passwordPinDialogFragment = PasswordPinDialogFragment(R.string.change, walletID != null, true, this)

        val title = PassPromptTitle(R.string.confirm_to_change, R.string.confirm_to_change, R.string.confirm_to_change)
        var passPromptUtil: PassPromptUtil? = null
        passPromptUtil = PassPromptUtil(fragmentActivity, walletID, title, false){ _, pass ->
            if(pass != null){
                passphrase = pass

                passwordPinDialogFragment.tabIndex = if(passPromptUtil!!.passType == Dcrlibwallet.PassphraseTypePass){
                    0
                }else 1

                passwordPinDialogFragment.show(fragmentActivity)

            }

            return@PassPromptUtil true
        }

        passPromptUtil.show()
    }

    override fun onEnterPasswordOrPin(newPassphrase: String, passphraseType: Int) {
        GlobalScope.launch(Dispatchers.IO){
            val multiWallet = WalletData.multiWallet!!
            try{
                multiWallet.changeStartupPassphrase(passphrase.toByteArray(), newPassphrase.toByteArray())
                multiWallet.setInt32ConfigValueForKey(Dcrlibwallet.StartupSecurityTypeConfigKey, passphraseType)

                // saving after a successful change to avoid saving a wrong passphrase
                BiometricUtils.saveToKeystore(fragmentActivity, newPassphrase, Constants.STARTUP_PASSPHRASE)

                withContext(Dispatchers.Main){
                    passwordPinDialogFragment.dismiss()
                    SnackBar.showText(fragmentActivity, R.string.startup_security_changed)
                }
            }catch (e: Exception){
                e.printStackTrace()

                val passType = multiWallet.readInt32ConfigValueForKey(Dcrlibwallet.StartupSecurityTypeConfigKey, Dcrlibwallet.PassphraseTypePass)
                if(e.message == Dcrlibwallet.ErrInvalidPassphrase){
                    val err = if(passType == Dcrlibwallet.PassphraseTypePass){
                        R.string.invalid_password
                    }else{
                        R.string.invalid_pin
                    }


                    withContext(Dispatchers.Main){
                        passwordPinDialogFragment.dismiss()
                        SnackBar.showError(fragmentActivity, err)
                    }

                }
            }

        }
    }
}