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
import com.dcrandroid.fragments.PasswordFragment
import com.dcrandroid.fragments.PinFragment
import kotlinx.android.synthetic.main.activity_enter_passphrase.*

class EncryptWallet : BaseActivity(), View.OnClickListener {

    private var seed: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_enter_passphrase)

        seed = intent.extras.getString(Constants.SEED)

        val passwordFragment = PasswordFragment()
        passwordFragment.seed = seed
        supportFragmentManager.beginTransaction().replace(R.id.container, passwordFragment)
                .commit()

        layout_password.setOnClickListener(this)
        layout_pin.setOnClickListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        seed = null
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.layout_password -> {

                password_bottom_border.visibility = View.VISIBLE
                label_password.setTextColor(Color.parseColor("#4e5f70"))

                pin_bottom_border.visibility = View.INVISIBLE
                label_pin.setTextColor(Color.parseColor("#a4abb1"))

                val passwordFragment = PasswordFragment()
                passwordFragment.seed = seed
                supportFragmentManager.beginTransaction().replace(R.id.container, passwordFragment)
                        .commit()
            }
            R.id.layout_pin -> {

                password_bottom_border.visibility = View.INVISIBLE
                label_password.setTextColor(Color.parseColor("#a4abb1"))

                pin_bottom_border.visibility = View.VISIBLE
                label_pin.setTextColor(Color.parseColor("#4e5f70"))

                val pinFragment = PinFragment()
                pinFragment.seed = seed
                supportFragmentManager.beginTransaction().replace(R.id.container, pinFragment)
                        .commit()
            }
        }
    }
}