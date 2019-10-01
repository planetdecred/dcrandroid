/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities.more

import android.content.Intent
import android.os.Bundle
import com.dcrandroid.BuildConfig
import com.dcrandroid.R
import com.dcrandroid.activities.License

class AboutActivity: ListActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        items = arrayOf(
                ListItem(R.string.version, BuildConfig.VERSION_NAME),
                ListItem(R.string.license)
        )

        super.onCreate(savedInstanceState)
        setTitle(R.string.about)

        adapter.itemTapped = {
            when (it) {
                0 -> {
                    // show stakey after 8 taps
                }
                1 -> {
                    startActivity(Intent(this, License::class.java))
                }
            }
        }

    }
}