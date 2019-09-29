/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities.security

import android.os.Bundle
import com.dcrandroid.R
import com.dcrandroid.dialog.InfoDialog
import com.dcrandroid.extensions.show
import com.dcrandroid.fragments.more.ListActivity
import com.dcrandroid.fragments.more.ListItem
import kotlinx.android.synthetic.main.security_tools.*

class SecurityTools: ListActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        items = arrayOf(
                ListItem(R.string.validate_addresses, R.drawable.ic_settings),
                ListItem(R.string.sign_message, R.drawable.ic_security),
                ListItem(R.string.verify_signature, R.drawable.ic_question_mark)
        )

        super.onCreate(savedInstanceState)
        title = "Security tddddools"
        iv_info.show()
    }

    override fun showInfo() {
        InfoDialog(this)
                .setDialogTitle(getString(R.string.security_tools))
                .setMessage(getString(R.string.security_tools_info))
                .setPositiveButton(getString(R.string.got_it))
                .show()
    }
}