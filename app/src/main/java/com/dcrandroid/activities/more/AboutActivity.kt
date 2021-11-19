/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities.more

import android.content.Intent
import android.os.Bundle
import com.dcrandroid.BuildConfig
import com.dcrandroid.R
import com.dcrandroid.activities.License
import com.dcrandroid.dialog.StakeyDialog

class AboutActivity : ListActivity() {

    private val tapThreshold = 3000L // 3 seconds

    var versionTaps: Int = 0
    var lastVersionTap = 0L

    override fun onCreate(savedInstanceState: Bundle?) {

        items = arrayOf(
            ListItem(R.string.version, BuildConfig.VERSION_NAME),
            ListItem(R.string.build_date, BuildConfig.BuildDate),
            ListItem(R.string.net, BuildConfig.NetType),
            ListItem(R.string.license)
        )

        super.onCreate(savedInstanceState)
        setTitle(R.string.about)

        adapter.itemTapped = {
            when (it) {
                0 -> {
                    val currentTime = System.currentTimeMillis()
                    val timeDifference = currentTime - lastVersionTap

                    if (timeDifference > tapThreshold) {
                        versionTaps = 1
                        lastVersionTap = currentTime
                    } else {
                        versionTaps++

                        if (versionTaps >= 8) {
                            versionTaps = 0
                            lastVersionTap = 0

                            StakeyDialog(this).show()
                        }
                    }
                }
                3 -> {
                    startActivity(Intent(this, License::class.java))
                }
            }
        }

    }
}