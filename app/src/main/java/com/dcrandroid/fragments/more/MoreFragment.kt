/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.fragments.more

import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import com.dcrandroid.activities.Settings
import com.dcrandroid.activities.security.SecurityTools
import com.dcrandroid.fragments.BaseFragment

class MoreFragment : BaseFragment() {
    lateinit var recyclerView: RecyclerView

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setToolbarTitle(R.string.more, false)

        var items= arrayOf(
                ListItem(R.string.settings, R.drawable.ic_settings, Intent(context, Settings::class.java)),
                ListItem(R.string.security, R.drawable.ic_security, Intent(context, SecurityTools::class.java)),
                ListItem(R.string.help, R.drawable.ic_question_mark),
                ListItem(R.string.about, R.drawable.ic_info1),
                ListItem(R.string.debug, R.drawable.ic_debug))


        val adapter = ListAdapter(context!!, items)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

    }

}