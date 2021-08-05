/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities.privacy

import android.content.DialogInterface
import android.os.Bundle
import android.text.Html
import android.view.WindowManager
import com.dcrandroid.BuildConfig
import com.dcrandroid.R
import com.dcrandroid.activities.BaseActivity
import com.dcrandroid.data.Constants
import com.dcrandroid.dialog.InfoDialog
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
import kotlinx.coroutines.withContext

class AccountMixerActivity : BaseActivity(), AccountMixerNotificationListener,
    TxAndBlockNotificationListener {

    private lateinit var wallet: Wallet
    private var mixedAccountNumber: Int = -1
    private var unmixedAccountNumber: Int = -1

    override fun onResume() {
        super.onResume()
        multiWallet!!.removeTxAndBlockNotificationListener(this.javaClass.name)
        multiWallet!!.addTxAndBlockNotificationListener(this, this.javaClass.name)
        multiWallet!!.removeAccountMixerNotificationListener(this.javaClass.name)
        multiWallet!!.addAccountMixerNotificationListener(this, this.javaClass.name)
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
        shuffle_port.text = if (BuildConfig.IS_TESTNET) Dcrlibwallet.TestnetShufflePort
        else Dcrlibwallet.MainnetShufflePort

        mix_tx_change_switch.isChecked =
            wallet.readBoolConfigValueForKey(Dcrlibwallet.AccountMixerMixTxChange, false)
        setMixTxChangeSummary()

        mixedAccountNumber =
            wallet.readInt32ConfigValueForKey(Dcrlibwallet.AccountMixerMixedAccount, -1)
        unmixedAccountNumber =
            wallet.readInt32ConfigValueForKey(Dcrlibwallet.AccountMixerUnmixedAccount, -1)

        mixed_account_label.text = wallet.accountName(mixedAccountNumber)
        unmixed_account_label.text = wallet.accountName(unmixedAccountNumber)

        if (wallet.isAccountMixerActive) {
            mixer_toggle_switch.isChecked = true
        }

        mixer_toggle_switch.setOnClickListener {
            mixer_toggle_switch.isChecked = wallet.isAccountMixerActive

            if (wallet.isAccountMixerActive) {
                stopAccountMixer()
            } else {
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

        mix_tx_change.setOnClickListener {
            mix_tx_change_switch.toggle()
        }

        mix_tx_change_switch.setOnCheckedChangeListener { _, isChecked ->
            wallet.setBoolConfigValueForKey(Dcrlibwallet.AccountMixerMixTxChange, isChecked)
            setMixTxChangeSummary()
        }

        setMixerStatus()
    }

    private fun setMixTxChangeSummary() {
        if (mix_tx_change_switch.isChecked) {
            mix_tx_change_summary.setText(R.string.mix_tx_change_summary_enabled)
        } else {
            mix_tx_change_summary.setText(R.string.mix_tx_change_summary_disabled)
        }
    }

    override fun onPause() {
        super.onPause()
        multiWallet!!.removeAccountMixerNotificationListener(this.javaClass.name)
        multiWallet!!.removeTxAndBlockNotificationListener(this.javaClass.name)
    }

    private fun setMixerStatus() = GlobalScope.launch(Dispatchers.Main) {
        if (wallet.isAccountMixerActive) {
            tv_mixer_status.setText(R.string.keep_app_opened)
            tv_mixer_status.setTextColor(resources.getColor(R.color.blueGraySecondTextColor))

            iv_mixer_status.setImageResource(R.drawable.ic_alert)
            iv_mixer_status.show()

            mixing_arrow.show()
        } else {

            if (multiWallet!!.readyToMix(wallet.id)) {
                tv_mixer_status.setText(R.string.ready_to_mix)
                tv_mixer_status.setTextColor(resources.getColor(R.color.blueGraySecondTextColor))
                iv_mixer_status.hide()
                mixer_toggle_switch.isEnabled = true
            } else {
                tv_mixer_status.setText(R.string.no_mixable_output)
                tv_mixer_status.setTextColor(resources.getColor(R.color.colorError))
                iv_mixer_status.hide()
                mixer_toggle_switch.isEnabled = false
            }

            mixing_arrow.hide()
        }

        unmixed_balance.text =
            CoinFormat.formatAlpha(wallet.getAccountBalance(unmixedAccountNumber).spendable, getColor(R.color.text4))
        mixed_balance.text =
            CoinFormat.formatAlpha(wallet.getAccountBalance(mixedAccountNumber).spendable, getColor(R.color.text4))
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
            .setPositiveButton(
                getString(R.string._continue),
                DialogInterface.OnClickListener { _, _ ->
                    startAccountMixer()
                })
            .setNegativeButton(getString(R.string.cancel))
            .show()
    }

    private fun startAccountMixer() {

        val title = PassPromptTitle(
            R.string.unlock_to_start_mixing,
            R.string.unlock_to_start_mixing,
            R.string.unlock_to_start_mixing
        )
        PassPromptUtil(
            this@AccountMixerActivity,
            wallet.id,
            title,
            allowFingerprint = true
        ) { dialog, passphrase ->

            if (passphrase == null) {
                return@PassPromptUtil true
            }

            GlobalScope.launch(Dispatchers.Default) {
                try {
                    multiWallet!!.startAccountMixer(wallet.id, passphrase)
                    GlobalScope.launch(Dispatchers.Main) {
                        dialog?.dismiss()
                    }

                } catch (e: Exception) {
                    if (e.message == Dcrlibwallet.ErrNoMixableOutput) {
                        SnackBar.showError(this@AccountMixerActivity, R.string.no_mixable_output)
                        GlobalScope.launch(Dispatchers.Main) {
                            dialog?.dismiss()
                        }
                    } else {
                        PassPromptUtil.handleError(this@AccountMixerActivity, e, dialog)
                    }
                }
            }

            false
        }.show()
    }

    private fun stopAccountMixer() = GlobalScope.launch(Dispatchers.Default) {
        multiWallet?.stopAccountMixer(wallet.id)
        // Allow display to timeout after mixer is stopped
        withContext(Dispatchers.Main) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onAccountMixerEnded(walletID: Long) {
        if (walletID == wallet.id) {
            SnackBar.showText(this, R.string.mixer_has_stopped_running)
            setMixerStatus()
            GlobalScope.launch(Dispatchers.Main) {
                mixer_toggle_switch.isChecked = false
                // Allow display to timeout after mixer completes
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    override fun onAccountMixerStarted(walletID: Long) {
        SnackBar.showText(this, R.string.mixer_is_running)
        setMixerStatus()
        GlobalScope.launch(Dispatchers.Main) {
            mixer_toggle_switch.isChecked = true
            // Prevent display from timing out after mixer starts
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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