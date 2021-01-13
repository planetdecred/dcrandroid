/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities

import android.content.DialogInterface
import android.os.Bundle
import android.text.Html
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.dialog.InfoDialog
import com.dcrandroid.dialog.PasswordPromptDialog
import com.dcrandroid.dialog.PinPromptDialog
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.show
import com.dcrandroid.util.*
import dcrlibwallet.AccountMixerNotificationListener
import dcrlibwallet.Dcrlibwallet
import dcrlibwallet.TxAndBlockNotificationListener
import dcrlibwallet.Wallet
import kotlinx.android.synthetic.main.activity_account_mixer.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AccountMixerActivity: BaseActivity(), AccountMixerNotificationListener, TxAndBlockNotificationListener {

    private lateinit var wallet: Wallet
    private var mixedAccountNumber: Int = -1
    private var unmixedAccountNumber: Int = -1

    override fun onResume() {
        super.onResume()
        multiWallet!!.addTxAndBlockNotificationListener(this, this.javaClass.name)
        setMixerStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_mixer)

        val walletID = intent.getLongExtra(Constants.WALLET_ID, -1)
        wallet = multiWallet!!.walletWithID(walletID)

        wallet_name.text = wallet.name
        mixed_account_branch.text = Dcrlibwallet.MixedAccountBranch.toString()
        shuffle_server.text = Dcrlibwallet.ShuffleServer
        shuffle_port.text = Dcrlibwallet.ShufflePort

        mixedAccountNumber = wallet.readInt32ConfigValueForKey(Dcrlibwallet.AccountMixerMixedAccount, -1)
        unmixedAccountNumber = wallet.readInt32ConfigValueForKey(Dcrlibwallet.AccountMixerUnmixedAccount, -1)

        mixed_account_label.text = wallet.accountName(mixedAccountNumber)
        unmixed_account_label.text = wallet.accountName(unmixedAccountNumber)

//      SwitchPreference(this@AccountMixerActivity, Dcrlibwallet.walletUniqueConfigKey(wallet.id, Dcrlibwallet.AccountMixerMixTxChange), mix_tx_change)

        multiWallet?.setAccountMixerNotification(this@AccountMixerActivity)

        if(wallet.isAccountMixerActive){
            mixer_toggle_switch.isChecked = true
        }

        mixer_toggle_switch.setOnClickListener {
            mixer_toggle_switch.isChecked = wallet.isAccountMixerActive

            if(wallet.isAccountMixerActive){
                stopAccountMixer()
            }else{
                showWarningAndStartMixer()
            }
        }

        iv_info.setOnClickListener {
            InfoDialog(this)
                    .setDialogTitle(getString(R.string.mixer_help_title))
                    .setMessage(Html.fromHtml(getString(R.string.mixer_help_desc)))
                    .setPositiveButton(getString(R.string.got_it), null)
                    .show()
        }

        go_back.setOnClickListener { finish() }

        setMixerStatus()
    }

    override fun onPause() {
        super.onPause()
        multiWallet!!.removeTxAndBlockNotificationListener(this.javaClass.name)
    }

    private fun setMixerStatus() = GlobalScope.launch(Dispatchers.Main) {
        if(wallet.isAccountMixerActive){
            tv_mixer_status.setText(R.string.keep_app_opened)
            tv_mixer_status.setTextColor(resources.getColor(R.color.blueGraySecondTextColor))

            iv_mixer_status.setImageResource(R.drawable.ic_alert)
            iv_mixer_status.show()

            mixing_arrow.show()
        }else {

            if (multiWallet!!.readyToMix(wallet.id)) {
                tv_mixer_status.setText(R.string.ready_to_mix)
                tv_mixer_status.setTextColor(resources.getColor(R.color.blueGraySecondTextColor))
                iv_mixer_status.hide()
            } else {
                tv_mixer_status.setText(R.string.no_mixable_output)
                tv_mixer_status.setTextColor(resources.getColor(R.color.colorError))
                iv_mixer_status.hide()
            }

            mixing_arrow.hide()
        }

        unmixed_balance.text = getString(R.string.x_dcr, CoinFormat.formatDecred(wallet.getAccountBalance(unmixedAccountNumber).total))
        mixed_balance.text = getString(R.string.x_dcr, CoinFormat.formatDecred(wallet.getAccountBalance(mixedAccountNumber).total))
    }

    private fun showWarningAndStartMixer() {

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
                .setNegativeButton(getString(R.string.cancel))
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
            SnackBar.showText(this, R.string.mixer_has_stopped_running)
            setMixerStatus()
            GlobalScope.launch(Dispatchers.Main) {
                mixer_toggle_switch.isChecked = false
            }
        }
    }

    override fun onAccountMixerStarted(walletID: Long) {
        SnackBar.showText(this, R.string.mixer_is_running)
        setMixerStatus()
        GlobalScope.launch(Dispatchers.Main) {
            mixer_toggle_switch.isChecked = true
        }
    }

    override fun onBlockAttached(walletID: Long, blockHeight: Int) {
        setMixerStatus()
    }

    override fun onTransactionConfirmed(walletID: Long, hash: String?, blockHeight: Int) {

    }

    override fun onTransaction(transaction: String?) {

    }

}