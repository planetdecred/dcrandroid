/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities

import android.app.Activity
import android.content.DialogInterface
import android.os.Bundle
import android.widget.CompoundButton
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.dialog.InfoDialog
import com.dcrandroid.dialog.PasswordPromptDialog
import com.dcrandroid.dialog.PinPromptDialog
import com.dcrandroid.extensions.findCSPPAccounts
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.show
import com.dcrandroid.preference.SwitchPreference
import com.dcrandroid.util.PassPromptTitle
import com.dcrandroid.util.PassPromptUtil
import com.dcrandroid.util.SnackBar
import com.dcrandroid.util.Utils
import com.dcrandroid.view.util.InputHelper
import dcrlibwallet.AccountMixerNotificationListener
import dcrlibwallet.Dcrlibwallet
import dcrlibwallet.Wallet
import kotlinx.android.synthetic.main.activity_account_mixer.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccountMixerActivity: BaseActivity(), CompoundButton.OnCheckedChangeListener, AccountMixerNotificationListener {

    private lateinit var wallet: Wallet
    private lateinit var mixedAccountInputHelper: InputHelper
    private lateinit var unmixedAccountInputHelper: InputHelper

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

    private fun setupCreateAccountCard() {
        start_account_mixer_card.hide()
        create_accounts_card.show()

        val validateInput: (String) -> Boolean = {
            it.isNotBlank()
        }

        mixedAccountInputHelper = InputHelper(this, mixed_account_name_input, validateInput).apply {
            hidePasteButton()
            hideQrScanner()
            setHint(R.string.mixed_account)
        }

        unmixedAccountInputHelper = InputHelper(this, change_account_name_input, validateInput).apply {
            hidePasteButton()
            hideQrScanner()
            setHint(R.string.unmixed_account)
        }

        val textChanged = {
            tv_continue.isEnabled = mixedAccountInputHelper.validatedInput != null && unmixedAccountInputHelper.validatedInput != null
        }

        mixedAccountInputHelper.textChanged = textChanged
        unmixedAccountInputHelper.textChanged = textChanged

        val usedCSPPAccounts = wallet.findCSPPAccounts()

        if (usedCSPPAccounts != null) {
            val mixedAccountName = wallet.accountName(usedCSPPAccounts[0])
            mixedAccountInputHelper.editText.setText(mixedAccountName)

            val unmixedAccountName = wallet.accountName(usedCSPPAccounts[1])
            unmixedAccountInputHelper.editText.setText(unmixedAccountName)
        } else {
            mixedAccountInputHelper.editText.setText(R.string.mixed)
            unmixedAccountInputHelper.editText.setText(R.string.unmixed)
        }

        tv_continue.setOnClickListener {

            if (mixedAccountInputHelper.validatedInput!! == unmixedAccountInputHelper.validatedInput!!) {
                SnackBar.showError(this, R.string.same_mixing_accounts_error)
                return@setOnClickListener
            }

            val mixedAccountExists = wallet.hasAccount(mixedAccountInputHelper.validatedInput)
            val unmixedAccountExists = wallet.hasAccount(unmixedAccountInputHelper.validatedInput)

            var notificationMessage: Int? = null
            if (!mixedAccountExists && !unmixedAccountExists) {
                notificationMessage = R.string.new_accounts_mixed_unmixed
            } else if (!mixedAccountExists) {
                notificationMessage = R.string.new_account_mixed
            } else if (!unmixedAccountExists) {
                notificationMessage = R.string.new_account_umixed
            }

            if (notificationMessage != null) {
                InfoDialog(this)
                        .setMessage(getString(notificationMessage))
                        .setNegativeButton(getString(R.string.cancel), null)
                        .setPositiveButton(getString(R.string._continue), DialogInterface.OnClickListener { _, _ ->
                            createMixerAccount(mixedAccountExists, unmixedAccountExists)
                        })
                        .show()
            } else {
                setMixerAccounts(wallet.accountNumber(mixedAccountInputHelper.validatedInput!!.trim()),
                        wallet.accountNumber(unmixedAccountInputHelper.validatedInput!!.trim()))
            }
        }
    }

    private fun createMixerAccount(mixedAccountExists: Boolean, unmixedAccountExists: Boolean) {

        val setEnabled: (Boolean) -> Unit = { enabled ->
            if (enabled) {
                tv_continue.show()
                progress_bar.hide()
            } else {
                tv_continue.hide()
                progress_bar.show()
            }

            mixedAccountInputHelper.setEnabled(enabled)
            unmixedAccountInputHelper.setEnabled(enabled)
        }

        setEnabled(false)
        val title = PassPromptTitle(R.string.confirm_to_create_accounts, R.string.confirm_to_create_accounts, R.string.confirm_to_create_accounts)
        PassPromptUtil(this, wallet.id, title, allowFingerprint = true) { dialog, passphrase ->

            if (passphrase == null) {
                setEnabled(true)
                return@PassPromptUtil true
            }

            GlobalScope.launch(Dispatchers.Default) {
                try {

                    val mixedAccountNumber = if (mixedAccountExists) {
                        wallet.accountNumber(mixedAccountInputHelper.validatedInput!!.trim())
                    } else {
                        wallet.nextAccount(mixedAccountInputHelper.validatedInput!!.trim(), passphrase.toByteArray())
                    }

                    val unmixedAccountNumber = if (unmixedAccountExists) {
                        wallet.accountNumber(unmixedAccountInputHelper.validatedInput!!.trim())
                    } else {
                        wallet.nextAccount(unmixedAccountInputHelper.validatedInput!!.trim(), passphrase.toByteArray())
                    }

                    withContext(Dispatchers.Main) {
                        dialog?.dismiss()
                    }

                    setMixerAccounts(mixedAccountNumber, unmixedAccountNumber)
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

    private fun setMixerAccounts(mixedAccountNumber: Int, unmixedAccountNumber: Int) = GlobalScope.launch(Dispatchers.Main) {
        wallet.setAccountMixerConfig(mixedAccountNumber, unmixedAccountNumber)
        create_accounts_card.hide()
        setResult(Activity.RESULT_OK)
        setupAccountMixerCard()
    }

    private fun setupAccountMixerCard() = GlobalScope.launch(Dispatchers.Main) {
        start_account_mixer_card.show()

        val mixedAccountNumber = wallet.readInt32ConfigValueForKey(Dcrlibwallet.AccountMixerMixedAccount, -1)
        val changeAccountNumber = wallet.readInt32ConfigValueForKey(Dcrlibwallet.AccountMixerChangeAccount, -1)

        mixed_account_label.text = wallet.accountName(mixedAccountNumber)
        change_account_label.text = wallet.accountName(changeAccountNumber)

        SwitchPreference(this@AccountMixerActivity, Dcrlibwallet.walletUniqueConfigKey(wallet.id, Dcrlibwallet.AccountMixerMixTxChange), mix_tx_change)

        start_account_mixer.setOnClickListener {
            switch_start_account_mixer.isChecked = !switch_start_account_mixer.isChecked
        }

        mixer_not_running.text = when {
            wallet.isAccountMixerActive -> getString(R.string.running)
            else -> getString(R.string.not_running)
        }
        switch_start_account_mixer.isChecked = wallet.isAccountMixerActive
        switch_start_account_mixer.setOnCheckedChangeListener(this@AccountMixerActivity)

        multiWallet?.setAccountMixerNotification(this@AccountMixerActivity)
    }

    private fun setChecked(checked: Boolean) = GlobalScope.launch(Dispatchers.Main){
        switch_start_account_mixer.setOnCheckedChangeListener(null)
        switch_start_account_mixer.isChecked = checked
        switch_start_account_mixer.setOnCheckedChangeListener(this@AccountMixerActivity)

        mixer_not_running.text = when {
            wallet.isAccountMixerActive -> getString(R.string.running)
            else -> getString(R.string.not_running)
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        setChecked(!isChecked)

        changeSwitchState(false)
            if(isChecked){
                showWarningBeforeStarting()
            } else {
                stopAccountMixer()
            }
    }

    private fun changeSwitchState(enabled: Boolean) = GlobalScope.launch(Dispatchers.Main) {
        start_account_mixer.isEnabled = enabled
        switch_start_account_mixer.isEnabled = enabled
    }

    private fun showWarningBeforeStarting() {

        if (multiWallet!!.isSyncing) {
            changeSwitchState(true)
            SnackBar.showError(this, R.string.wait_for_sync)
            return
        } else if (!multiWallet!!.isConnectedToDecredNetwork) {
            changeSwitchState(true)
            SnackBar.showError(this, R.string.not_connected)
            return
        }

        InfoDialog(this)
                .setMessage(getString(R.string.start_mixer_warning))
                .setPositiveButton(getString(R.string._continue), DialogInterface.OnClickListener { _, _ ->
                    startAccountMixer()
                })
                .setNegativeButton(getString(R.string.cancel), DialogInterface.OnClickListener { _, _ ->
                    changeSwitchState(true)
                })
                .show()
    }

    private fun startAccountMixer() {

        val title = PassPromptTitle(R.string.unlock_to_start_mixing, R.string.unlock_to_start_mixing, R.string.unlock_to_start_mixing)
        PassPromptUtil(this@AccountMixerActivity, wallet.id, title, allowFingerprint = true) { dialog, passphrase ->

            if (passphrase == null) {
                changeSwitchState(true)
                return@PassPromptUtil true
            }

            try {
                multiWallet!!.startAccountMixer(wallet.id, passphrase)
                setChecked(true)
                changeSwitchState(true)
                GlobalScope.launch(Dispatchers.Main) {
                    dialog?.dismiss()
                }

                SnackBar.showText(this, R.string.mixer_is_running)

            }catch (e: Exception){
                if (e.message == Dcrlibwallet.ErrInvalidPassphrase) {
                    if (dialog is PinPromptDialog) {
                        dialog.setProcessing(false)
                        dialog.showError()
                    } else if (dialog is PasswordPromptDialog) {
                        dialog.setProcessing(false)
                        dialog.showError()
                    }
                } else if(e.message == Dcrlibwallet.ErrNoMixableOutput){
                    SnackBar.showError(this, R.string.no_mixable_output)
                    GlobalScope.launch(Dispatchers.Main) {
                        dialog?.dismiss()
                        changeSwitchState(true)
                    }
                } else{
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
        multiWallet?.stopAccountMixer(wallet.id)
        setChecked(false)
        changeSwitchState(true)
    }

    override fun onAccountMixerEnded(walletID: Long) {
        if(walletID == wallet.id){
            setChecked(false)
            changeSwitchState(true)
            SnackBar.showText(this, R.string.mixer_has_stopped_running)
        }
    }

    override fun onAccountMixerStarted(walletID: Long) {
    }

}