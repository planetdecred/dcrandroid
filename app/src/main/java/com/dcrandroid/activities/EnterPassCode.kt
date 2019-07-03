/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.util.KeyPad
import kotlinx.android.synthetic.main.passcode.*

class EnterPassCode : BaseActivity(), KeyPad.KeyPadListener {

    private var keyPad: KeyPad? = null
    private var isChange: Boolean? = null
    private var isSpendingPassword: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.passcode)

        pin_strength.visibility = View.GONE
        tv_pin_strength.visibility = View.GONE

        isChange = intent.getBooleanExtra(Constants.CHANGE, false)
        isSpendingPassword = intent.getBooleanExtra(Constants.SPENDING_PASSWORD, true)

        if (isChange!!) {
            keypad_instruction.setText(R.string.enter_current_pin)
        } else {
            if (isSpendingPassword!!) {
                keypad_instruction.setText(R.string.enter_spending_pin)
            } else {
                keypad_instruction.setText(R.string.enter_startup_pin)
            }
        }

        keyPad = KeyPad(keypad, keypad_pin_view)
        keyPad!!.setKeyListener(this)
    }

    override fun onPassCodeCompleted(passCode: String) {
        if (isChange!!) {
            val intent = Intent(this, ChangePassphrase::class.java)
            intent.putExtra(Constants.PASSPHRASE, passCode)
            intent.putExtra(Constants.SPENDING_PASSWORD, isSpendingPassword)
            startActivity(intent)
            finish()
        } else {
            val data = Intent()
            data.putExtra(Constants.PASSPHRASE, passCode)
            setResult(Activity.RESULT_OK, data)
            finish()
        }
    }

    override fun onBackPressed() {
        if (!intent.getBooleanExtra(Constants.NO_RETURN, false)) {
            super.onBackPressed()
        }
    }

    override fun onPinEnter(pin: String?, passCode: String) {}
}