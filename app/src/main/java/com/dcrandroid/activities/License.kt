/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.dcrandroid.R

class License : AppCompatActivity() {
    val license = "ISC License" +
            "\n\n" +
            "Copyright (c) 2018-2019 The Decred developers" +
            "\n\n" +
            "Permission to use, copy, modify, and distribute this software for any" +
            " purpose with or without fee is hereby granted, provided that the above" +
            " copyright notice and this permission notice appear in all copies." +
            "\n\n" +
            "THE SOFTWARE IS PROVIDED \"AS IS\" AND THE AUTHOR DISCLAIMS ALL WARRANTIES" +
            " WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF" +
            " MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR" +
            " ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES" +
            " WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN" +
            " ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF" +
            " OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val decorView = window.decorView
            decorView.systemUiVisibility = WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }

        setContentView(R.layout.activity_license)
        findViewById<TextView>(R.id.license_text).text = license
    }
}