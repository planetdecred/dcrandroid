package com.dcrandroid.activities.more

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.dcrandroid.R
import com.dcrandroid.activities.BaseActivity
import com.dcrandroid.fragments.more.PoliteiaFragment

class PoliteiaActivity : BaseActivity() {
    private lateinit var currentFragment: Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_politeia)

        switchFragment()
    }

    private fun switchFragment() {
        currentFragment = PoliteiaFragment()
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.container, currentFragment)
                .commit()
    }
}