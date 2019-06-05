/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities

import android.graphics.Color
import android.os.Bundle
import android.view.View
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.fragments.ChangePasswordFragment
import com.dcrandroid.fragments.ChangePinFragment
import com.dcrandroid.util.PreferenceUtil
import kotlinx.android.synthetic.main.activity_enter_passphrase.*

class ChangePassphrase : BaseActivity(), View.OnClickListener {

    private var oldPassPhrase: String? = null
    private var isSpendingPassword: Boolean? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_enter_passphrase)

        oldPassPhrase = intent.getStringExtra(Constants.PASSPHRASE)
        if (oldPassPhrase == null) {
            oldPassPhrase = ""
        }

        isSpendingPassword = intent.getBooleanExtra(Constants.SPENDING_PASSWORD, true)

        val util = PreferenceUtil(this)

        if ((util.get(Constants.SPENDING_PASSPHRASE_TYPE) == Constants.PIN && isSpendingPassword!!)
                || (util.get(Constants.STARTUP_PASSPHRASE_TYPE) == Constants.PIN && !isSpendingPassword!!)) {
            layout_pin.setBackgroundColor(Color.parseColor("#F3F5F6"))
            layout_password.setBackgroundColor(android.R.attr.selectableItemBackground)
            val pinFragment = ChangePinFragment()
            pinFragment.oldPassphrase = oldPassPhrase
            pinFragment.isSpendingPassword = isSpendingPassword
            supportFragmentManager.beginTransaction().replace(R.id.container, pinFragment)
                    .commit()
        } else {
            val passwordFragment = ChangePasswordFragment()
            passwordFragment.oldPassphrase = oldPassPhrase
            passwordFragment.isSpendingPassword = isSpendingPassword
            supportFragmentManager.beginTransaction().replace(R.id.container, passwordFragment)
                    .commit()
        }

        layout_password.setOnClickListener(this)
        layout_pin.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.layout_password -> {
                layout_password.setBackgroundColor(Color.parseColor("#F3F5F6"))
                layout_pin.setBackgroundColor(android.R.attr.selectableItemBackground)
                val passwordFragment = ChangePasswordFragment()
                passwordFragment.oldPassphrase = oldPassPhrase
                passwordFragment.isSpendingPassword = isSpendingPassword
                supportFragmentManager.beginTransaction().replace(R.id.container, passwordFragment)
                        .commit()
            }
            R.id.layout_pin -> {
                layout_pin.setBackgroundColor(Color.parseColor("#F3F5F6"))
                layout_password.setBackgroundColor(android.R.attr.selectableItemBackground)
                val pinFragment = ChangePinFragment()
                pinFragment.oldPassphrase = oldPassPhrase
                pinFragment.isSpendingPassword = isSpendingPassword
                supportFragmentManager.beginTransaction().replace(R.id.container, pinFragment)
                        .commit()
            }
        }
    }
}