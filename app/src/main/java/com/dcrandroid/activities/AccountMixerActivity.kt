/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities

import android.content.DialogInterface
import android.os.Bundle
import android.widget.CompoundButton
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.dialog.InfoDialog
import com.dcrandroid.dialog.PasswordPromptDialog
import com.dcrandroid.dialog.PinPromptDialog
import com.dcrandroid.util.PassPromptTitle
import com.dcrandroid.util.PassPromptUtil
import com.dcrandroid.util.SnackBar
import com.dcrandroid.util.Utils
import dcrlibwallet.AccountMixerNotificationListener
import dcrlibwallet.Dcrlibwallet
import dcrlibwallet.Wallet
import kotlinx.android.synthetic.main.activity_account_mixer.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AccountMixerActivity: BaseActivity(), CompoundButton.OnCheckedChangeListener, AccountMixerNotificationListener {

    private lateinit var wallet: Wallet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_mixer)

        val walletID = intent.getLongExtra(Constants.WALLET_ID, -1)
        wallet = multiWallet!!.walletWithID(walletID)

        go_back.setOnClickListener { finish() }

        wallet_name.text = wallet.name
        mixed_account_branch.text = Dcrlibwallet.MixedAccountBranch.toString()
        shuffle_server.text = Dcrlibwallet.ShuffleServer
        shuffle_port.text = Dcrlibwallet.ShufflePort

        if (wallet.readBoolConfigValueForKey(Dcrlibwallet.AccountMixerConfigSet, false)) {
            val mixedAccountNumber = wallet.readInt32ConfigValueForKey(Dcrlibwallet.AccountMixerMixedAccount, -1)
            val changeAccountNumber = wallet.readInt32ConfigValueForKey(Dcrlibwallet.AccountMixerUnmixedAccount, -1)

            mixed_account_label.text = wallet.accountName(mixedAccountNumber)
            unmixed_account_label.text = wallet.accountName(changeAccountNumber)

//            SwitchPreference(this@AccountMixerActivity, Dcrlibwallet.walletUniqueConfigKey(wallet.id, Dcrlibwallet.AccountMixerMixTxChange), mix_tx_change)

            multiWallet?.setAccountMixerNotification(this@AccountMixerActivity)
        }
    }

    private fun setupAccountMixerCard() = GlobalScope.launch(Dispatchers.Main) {


    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {

    }

    private fun showWarningBeforeStarting() {

        if (multiWallet!!.isSyncing) {
            SnackBar.showError(this, R.string.wait_for_sync)
            return
        } else if (!multiWallet!!.isConnectedToDecredNetwork) {
            SnackBar.showError(this, R.string.not_connected)
            return
        }

        InfoDialog(this)
                .setMessage(getString(R.string.start_mixer_warning))
                .setPositiveButton(getString(R.string._continue), DialogInterface.OnClickListener { _, _ ->
                    startAccountMixer()
                })
                .setNegativeButton(getString(R.string.cancel), DialogInterface.OnClickListener { _, _ ->
                })
                .show()
    }

    private fun startAccountMixer() {

        val title = PassPromptTitle(R.string.unlock_to_start_mixing, R.string.unlock_to_start_mixing, R.string.unlock_to_start_mixing)
        PassPromptUtil(this@AccountMixerActivity, wallet.id, title, allowFingerprint = true) { dialog, passphrase ->

            if (passphrase == null) {
                return@PassPromptUtil true
            }

            try {
                multiWallet!!.startAccountMixer(wallet.id, passphrase)
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
                        // off switch
                    }
                } else{
                    GlobalScope.launch(Dispatchers.Main) {
                        // off switch

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
    }

    override fun onAccountMixerEnded(walletID: Long) {
        if(walletID == wallet.id){
            // off switch
            SnackBar.showText(this, R.string.mixer_has_stopped_running)
        }
    }

    override fun onAccountMixerStarted(walletID: Long) {
        // on switch
    }

}