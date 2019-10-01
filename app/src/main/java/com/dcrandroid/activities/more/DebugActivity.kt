/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities.more

import android.content.Intent
import android.os.Bundle
import com.dcrandroid.R
import com.dcrandroid.activities.LogViewer

class DebugActivity: ListActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        items = arrayOf(
                ListItem(R.string.check_wallets_log),
                ListItem(R.string.logging_level, "Critical")
        )

        super.onCreate(savedInstanceState)
        setTitle(R.string.debug)

        adapter.itemTapped = {
            when(it){
                0 -> {
                    startActivity(Intent(this, LogViewer::class.java))
                }
            }
        }
    }
}