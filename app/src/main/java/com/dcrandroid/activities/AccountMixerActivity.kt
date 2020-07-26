/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities

import android.os.Bundle
import android.widget.CompoundButton
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.dialog.PasswordPromptDialog
import com.dcrandroid.dialog.PinPromptDialog
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.show
import com.dcrandroid.util.PassPromptTitle
import com.dcrandroid.util.PassPromptUtil
import com.dcrandroid.util.Utils
import com.dcrandroid.view.util.InputHelper
import dcrlibwallet.Dcrlibwallet
import dcrlibwallet.Wallet
import kotlinx.android.synthetic.main.activity_account_mixer.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccountMixerActivity: BaseActivity(), CompoundButton.OnCheckedChangeListener {

    lateinit var wallet: Wallet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_mixer)

        val walletID = intent.getLongExtra(Constants.WALLET_ID, -1)
        wallet = multiWallet!!.walletWithID(walletID)

        go_back.setOnClickListener { finish() }

        if(wallet.readBoolConfigValueForKey(Dcrlibwallet.AccountMixerConfigSet, false)){
            setupAccountMixerCard()
        }else{
            setupCreateAccountCard()
        }
    }

    private fun setupCreateAccountCard(){
        start_account_mixer_card.hide()
        create_accounts_card.show()

        val validateInput: (String) -> Boolean = {
            it.isNotBlank() && !wallet.hasAccount(it)
        }

        val mixedAccountInputHelper = InputHelper(this, mixed_account_name_input, validateInput).apply {
            validationMessage = R.string.account_exists
            hidePasteButton()
            hideQrScanner()
            setHint(R.string.mixed_account)
        }

        val changeAccountInputHelper = InputHelper(this, change_account_name_input, validateInput).apply {
            validationMessage = R.string.account_exists
            hidePasteButton()
            hideQrScanner()
            setHint(R.string.change_account)
        }

        val textChanged = {
            tv_create.isEnabled = mixedAccountInputHelper.validatedInput != null && changeAccountInputHelper.validatedInput != null
        }

        mixedAccountInputHelper.textChanged = textChanged
        changeAccountInputHelper.textChanged = textChanged

        val setEnabled: (Boolean) -> Unit = {enabled ->
            if (enabled) {
                tv_create.show()
                progress_bar.hide()
            } else {
                tv_create.hide()
                progress_bar.show()
            }

            mixedAccountInputHelper.setEnabled(enabled)
            changeAccountInputHelper.setEnabled(enabled)
        }

        tv_create.setOnClickListener {

            setEnabled(false)

            val title = PassPromptTitle(R.string.confirm_to_create_accounts, R.string.confirm_to_create_accounts, R.string.confirm_to_create_accounts)
            PassPromptUtil(this, wallet.id, title, allowFingerprint = true) { dialog, passphrase ->

                if(passphrase == null){
                    setEnabled(true)
                    return@PassPromptUtil true
                }

                GlobalScope.launch(Dispatchers.Default) {
                    try {
                        val mixedAccountNumber = wallet.nextAccount(mixedAccountInputHelper.validatedInput!!.trim(), passphrase.toByteArray())
                        val changeAccountNumber = wallet.nextAccount(changeAccountInputHelper.validatedInput!!.trim(), passphrase.toByteArray())

                        wallet.setAccountMixerConfig(mixedAccountNumber, changeAccountNumber)

                        withContext(Dispatchers.Main){
                            dialog?.dismiss()
                            create_accounts_card.hide()
                            setupAccountMixerCard()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()

                        if (e.message == Dcrlibwallet.ErrInvalidPassphrase) {
                            if (dialog is PinPromptDialog) {
                                dialog.setProcessing(false)
                                dialog.showError()
                            } else if (dialog is PasswordPromptDialog) {
                                dialog.setProcessing(false)
                                dialog.showError()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                setEnabled(true)

                                val op = this.javaClass.name + "setupCreateAccountCard"
                                dialog?.dismiss()
                                Utils.showErrorDialog(this@AccountMixerActivity, op + ": " + e.message)
                                Dcrlibwallet.logT(op, e.message)
                            }
                        }
                    }
                }
                false
            }.show()
        }
    }

    private fun setupAccountMixerCard() = GlobalScope.launch(Dispatchers.Main){
        start_account_mixer_card.show()

        val mixedAccountNumber = wallet.readInt32ConfigValueForKey(Dcrlibwallet.AccountMixerMixedAccount, -1)
        val changeAccountNumber = wallet.readInt32ConfigValueForKey(Dcrlibwallet.AccountMixerChangeAccount, -1)

        mixed_account_label.text = wallet.accountName(mixedAccountNumber)
        change_account_label.text = wallet.accountName(changeAccountNumber)

        start_account_mixer.setOnClickListener {
            switch_start_account_mixer.isChecked = !switch_start_account_mixer.isChecked
        }

        switch_start_account_mixer.isChecked = wallet.isAccountMixerActive
        switch_start_account_mixer.setOnCheckedChangeListener(this@AccountMixerActivity)
    }

    private fun setChecked(checked: Boolean) = GlobalScope.launch(Dispatchers.Main){
        switch_start_account_mixer.setOnCheckedChangeListener(null)
        switch_start_account_mixer.isChecked = checked
        switch_start_account_mixer.setOnCheckedChangeListener(this@AccountMixerActivity)
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        setChecked(!isChecked)

        changeSwitchState(false)
            if(isChecked){
                startAccountMixer()
            }else{
                stopAccountMixer()
            }
    }

    private fun changeSwitchState(enabled: Boolean) = GlobalScope.launch(Dispatchers.Main){
        start_account_mixer.isEnabled = enabled
        switch_start_account_mixer.isEnabled = enabled
    }

    private fun startAccountMixer()  {
        val title = PassPromptTitle(R.string.confirm_to_create_accounts, R.string.confirm_to_create_accounts, R.string.confirm_to_create_accounts)
        PassPromptUtil(this@AccountMixerActivity, wallet.id, title, allowFingerprint = true) { dialog, passphrase ->

            if (passphrase == null) {
                changeSwitchState(true)
                return@PassPromptUtil true
            }

            try{
                wallet.startAccountMixer(passphrase)
                setChecked(true)
                changeSwitchState(true)
                GlobalScope.launch(Dispatchers.Main) {
                    dialog?.dismiss()
                }

            }catch (e: Exception){
                if (e.message == Dcrlibwallet.ErrInvalidPassphrase) {
                    if (dialog is PinPromptDialog) {
                        dialog.setProcessing(false)
                        dialog.showError()
                    } else if (dialog is PasswordPromptDialog) {
                        dialog.setProcessing(false)
                        dialog.showError()
                    }
                } else {
                    GlobalScope.launch(Dispatchers.Main) {
                        changeSwitchState(true)

                        val op = this.javaClass.name + "startAccountMixer"
                        dialog?.dismiss()
                        Utils.showErrorDialog(this@AccountMixerActivity, op + ": " + e.message)
                        Dcrlibwallet.logT(op, e.message)
                    }
                }
            }

            false
        }.show()
    }

    private fun stopAccountMixer() = GlobalScope.launch(Dispatchers.Default){
        wallet.stopAccountMixer()
        setChecked(false)
        changeSwitchState(true)
    }

}