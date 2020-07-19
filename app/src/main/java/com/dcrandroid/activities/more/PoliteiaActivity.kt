package com.dcrandroid.activities.more

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.dcrandroid.R
import com.dcrandroid.activities.BaseActivity
import com.dcrandroid.activities.PoliteiaDetailsActivity
import kotlinx.android.synthetic.main.activity_politeia.*

class PoliteiaActivity : BaseActivity() {

    lateinit var proposalItem : View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_politeia)

        proposalItem = findViewById(R.id.proposal_item)
        proposalItem.setOnClickListener{
            startActivity(Intent(this@PoliteiaActivity, PoliteiaDetailsActivity::class.java))
            Toast.makeText(this, "proposal item clicked", Toast.LENGTH_SHORT).show()
        }

        go_back.setOnClickListener {
            finish()
        }
    }
}