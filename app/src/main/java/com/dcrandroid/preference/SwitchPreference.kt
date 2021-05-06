/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.preference

import android.content.Context
import android.view.View
import android.widget.CompoundButton
import kotlinx.android.synthetic.main.settings_activity.view.*

class SwitchPreference(context: Context, val key: String, val view: View, val checkChange: ((checked: Boolean) -> Boolean)? = null)
    : Preference(context, key, view), View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    init {
        view.pref_switch.isChecked = multiWallet!!.readBoolConfigValueForKey(key, false)
        view.setOnClickListener(this)
        view.pref_switch.setOnCheckedChangeListener(this)
    }

    override fun onClick(v: View?) {
        view.pref_switch.isChecked = !view.pref_switch.isChecked
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        var newValue = isChecked

        if (checkChange != null) {
            newValue = checkChange.let { it(isChecked) }
        }

        setValue(newValue)
    }

    fun setValue(newValue: Boolean) {
        multiWallet!!.setBoolConfigValueForKey(key, newValue)
        setChecked(newValue)
    }

    fun setChecked(checked: Boolean) {
        view.pref_switch.setOnCheckedChangeListener(null)
        view.pref_switch.isChecked = checked
        view.pref_switch.setOnCheckedChangeListener(this)
    }


}