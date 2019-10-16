/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities.more

import android.os.Bundle
import android.view.ViewTreeObserver
import com.dcrandroid.R
import com.dcrandroid.activities.BaseActivity
import com.dcrandroid.data.Constants
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.show
import com.dcrandroid.preference.EditTextPreference
import com.dcrandroid.preference.ListPreference
import com.dcrandroid.preference.SwitchPreference
import com.dcrandroid.util.BiometricUtils
import dcrlibwallet.Dcrlibwallet
import kotlinx.android.synthetic.main.settings_activity.*
import kotlinx.android.synthetic.main.settings_activity.view.*

const val IP_ADDRESS_REGEX = "^(?:(?:1\\d?\\d|[1-9]?\\d|2[0-4]\\d|25[0-5])\\.){3}(?:1\\d?\\d|[1-9]?\\d|2[0-4]\\d|25[0-‌​5])(?:[:]\\d+)?$"

class SettingsActivity: BaseActivity(), ViewTreeObserver.OnScrollChangedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        SwitchPreference(this, Dcrlibwallet.SpendUnconfirmedConfigKey, spend_unconfirmed_funds)

        if(BiometricUtils.isFingerprintEnrolled(this)){
            biometric_authentication.show()
            SwitchPreference(this, Dcrlibwallet.UseBiometricAuthConfigKey, biometric_authentication, biometricCheckChange)
        }

        SwitchPreference(this, Dcrlibwallet.BeepNewBlocksConfigKey, beep_new_blocks)
        SwitchPreference(this, Dcrlibwallet.SyncOnCellularConfigKey, wifi_sync)


        ListPreference(this, Dcrlibwallet.IncomingTxNotificationsConfigKey, Constants.DEFAULT_TX_NOTIFICATION,
                R.array.notification_options, incoming_transactions)

        setCurrencyConversionSummary(multiWallet.readInt32ConfigValueForKey(Dcrlibwallet.CurrencyConversionConfigKey, Constants.DEFAULT_CURRENCY_CONVERSION))
        ListPreference(this, Dcrlibwallet.CurrencyConversionConfigKey, Constants.DEFAULT_CURRENCY_CONVERSION,
                R.array.currency_conversion, currency_conversion){
            setCurrencyConversionSummary(it)
        }

        setPeerIP(multiWallet.readStringConfigValueForKey(Dcrlibwallet.SpvPersistentPeerAddressesConfigKey))
        EditTextPreference(this, Dcrlibwallet.SpvPersistentPeerAddressesConfigKey, R.string.peer_ip_dialog_title,
                R.string.peer_ip_pref_hint, R.string.invalid_peer_ip, spv_peer_ip, validateIPAddress){
            setPeerIP(it)
        }

        EditTextPreference(this, Dcrlibwallet.UserAgentConfigKey, R.string.user_agent_dialog_title, R.string.user_agent, null, user_agent)

        go_back.setOnClickListener {
            finish()
        }

        settings_scroll_view.viewTreeObserver.addOnScrollChangedListener(this)
    }

    private val validateIPAddress:(String) -> Boolean = {
        it.isBlank() || it.matches(Regex(IP_ADDRESS_REGEX))
    }


    val biometricCheckChange: (checked: Boolean) -> Boolean = {

        false
    }

    private fun setCurrencyConversionSummary(index: Int){
        val preferenceSummary = resources.getStringArray(R.array.currency_conversion)[index]
        currency_conversion.pref_subtitle.text = preferenceSummary
    }

    private fun setPeerIP(ip: String){
        if(ip.isBlank()){
            spv_peer_ip.pref_subtitle.hide()
        }else{
            spv_peer_ip.pref_subtitle.show()
            spv_peer_ip.pref_subtitle.text = ip
        }
    }

    override fun onScrollChanged() {
        app_bar.elevation = if(settings_scroll_view.scrollY > 0){
            resources.getDimension(R.dimen.app_bar_elevation)
        }else{
            0f
        }
    }
}