package com.dcrandroid.activities

import android.os.Bundle
import com.dcrandroid.R
import kotlinx.android.synthetic.main.activity_politeia_details.*

class PoliteiaDetailsActivity  : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_politeia_details)

        go_back.setOnClickListener {
            finish()
        }
    }
}