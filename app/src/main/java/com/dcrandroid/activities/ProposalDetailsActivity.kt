package com.dcrandroid.activities

import android.os.Bundle
import com.dcrandroid.R
import kotlinx.android.synthetic.main.activity_proposal_details.*

class ProposalDetailsActivity  : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_proposal_details)

        go_back.setOnClickListener {
            finish()
        }
    }
}