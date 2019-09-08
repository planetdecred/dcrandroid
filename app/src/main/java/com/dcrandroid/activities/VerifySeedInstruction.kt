/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import kotlinx.android.synthetic.main.verify_seed_instruction.*

class VerifySeedInstruction: BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.verify_seed_instruction)

        go_back.setOnClickListener {
            finish()
        }
    }

    fun verifyChecks(v: View){
        btn_verify.isEnabled = seed_check_1.isChecked && seed_check_2.isChecked &&
                seed_check_3.isChecked && seed_check_4.isChecked && seed_check_5.isChecked
    }

    fun proceedToNextPhase(v: View){
        val i = Intent(this, SaveSeedActivity::class.java)
        val walletID = intent.getLongExtra(Constants.WALLET_ID, -1)
        i.putExtra(Constants.WALLET_ID, walletID)
        i.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
        startActivity(i)
    }
}