/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities

import android.os.Bundle
import com.dcrandroid.R
import kotlinx.android.synthetic.main.activity_help.*
import android.content.Intent
import android.net.Uri


class HelpActivity: BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        go_back.setOnClickListener { finish() }

        see_docs.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.https_docs_decred_org)))
            startActivity(browserIntent)
        }
    }
}