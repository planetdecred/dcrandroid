/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.fragments.more

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.dcrandroid.R
import com.dcrandroid.activities.more.AboutActivity
import com.dcrandroid.activities.more.HelpActivity
import com.dcrandroid.activities.more.DebugActivity
import com.dcrandroid.activities.more.SettingsActivity
import com.dcrandroid.fragments.BaseFragment
import kotlinx.android.synthetic.main.fragment_more.*

class MoreFragment : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_more, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setToolbarTitle(R.string.more, false)

        val items= arrayOf(
                ListItem(R.string.settings, R.drawable.ic_settings, Intent(context, SettingsActivity::class.java)),
                ListItem(R.string.help, R.drawable.ic_question_mark, Intent(context, HelpActivity::class.java)),
                ListItem(R.string.about, R.drawable.ic_info1, Intent(context, AboutActivity::class.java)),
                ListItem(R.string.debug, R.drawable.ic_debug, Intent(context, DebugActivity::class.java)))

        val adapter = ListAdapter(context!!, items)
        more_recycler_view.layoutManager = LinearLayoutManager(context)
        more_recycler_view.adapter = adapter
    }

}