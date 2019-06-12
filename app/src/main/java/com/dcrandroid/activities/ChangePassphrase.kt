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
            displayPin()
        } else {
            displayPassword()
        }

        layout_password.setOnClickListener(this)
        layout_pin.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.layout_password -> {
                displayPassword()
            }
            R.id.layout_pin -> {
                displayPin()
            }
        }
    }

    private fun displayPassword() {

        password_bottom_border.visibility = View.VISIBLE
        label_password.setTextColor(Color.parseColor("#4e5f70"))

        pin_bottom_border.visibility = View.INVISIBLE
        label_pin.setTextColor(Color.parseColor("#a4abb1"))

        val passwordFragment = ChangePasswordFragment()
        passwordFragment.oldPassphrase = oldPassPhrase
        passwordFragment.isSpendingPassword = isSpendingPassword
        supportFragmentManager.beginTransaction().replace(R.id.container, passwordFragment)
                .commit()
    }

    private fun displayPin() {

        password_bottom_border.visibility = View.INVISIBLE
        label_password.setTextColor(Color.parseColor("#a4abb1"))

        pin_bottom_border.visibility = View.VISIBLE
        label_pin.setTextColor(Color.parseColor("#4e5f70"))

        val pinFragment = ChangePinFragment()
        pinFragment.oldPassphrase = oldPassPhrase
        pinFragment.isSpendingPassword = isSpendingPassword
        supportFragmentManager.beginTransaction().replace(R.id.container, pinFragment)
                .commit()
    }
}