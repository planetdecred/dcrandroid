/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities.more

import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.dcrandroid.R
import com.dcrandroid.activities.BaseActivity
import com.dcrandroid.activities.security.ValidateAddress
import com.dcrandroid.activities.security.VerifyMessage
import com.dcrandroid.dialog.InfoDialog
import com.dcrandroid.fragments.more.ListAdapter
import com.dcrandroid.fragments.more.ListItem
import kotlinx.android.synthetic.main.activity_security_tools.*

class SecurityTools : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_security_tools)

        iv_info.setOnClickListener {
            InfoDialog(this)
                .setDialogTitle(getString(R.string.security_tools))
                .setMessage(getString(R.string.security_tools_message))
                .setPositiveButton(getString(R.string.got_it), null)
                .show()
        }

        go_back.setOnClickListener {
            finish()
        }

        val items = arrayOf(
            ListItem(
                R.string.verify_message,
                R.drawable.ic_verify_message,
                Intent(this, VerifyMessage::class.java)
            ),
            ListItem(
                R.string.validate_addresses,
                R.drawable.ic_location_pin,
                Intent(this, ValidateAddress::class.java)
            )
        )

        val adapter = ListAdapter(this, items)
        security_tools_recycler_view.layoutManager = LinearLayoutManager(this)
        security_tools_recycler_view.adapter = adapter
    }


}