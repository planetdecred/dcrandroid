package com.dcrandroid.activities.more

import android.os.Bundle
import com.dcrandroid.R
import com.dcrandroid.activities.BaseActivity
import kotlinx.android.synthetic.main.activity_help.*

class PoliteiaActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_politeia)

        go_back.setOnClickListener {
            finish()
        }
    }
}