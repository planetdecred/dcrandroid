/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.fragments.more

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.annotation.StringRes
import androidx.recyclerview.widget.LinearLayoutManager
import com.dcrandroid.R
import com.dcrandroid.activities.more.*
import com.dcrandroid.fragments.BaseFragment
import kotlinx.android.synthetic.main.fragment_more.*
import kotlinx.android.synthetic.main.toolbar_layout.*

class MoreFragment : BaseFragment() {

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_more, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setToolbarTitle(R.string.more, false)

        val items = arrayOf(
                ListItem(
                        R.string.settings,
                        R.drawable.ic_settings,
                        Intent(context, SettingsActivity::class.java)
                ),
                ListItem(
                        R.string.security_tools,
                        R.drawable.ic_security,
                        Intent(context, SecurityTools::class.java)
                ),
                ListItem(
                        R.string.politeia,
                        R.drawable.ic_politeia,
                        Intent(context, PoliteiaActivity::class.java)
                ),
                ListItem(
                        R.string.help,
                        R.drawable.ic_question_mark,
                        Intent(context, HelpActivity::class.java)
                ),
                ListItem(
                        R.string.about,
                        R.drawable.ic_info1,
                        Intent(context, AboutActivity::class.java)
                ),
                ListItem(
                        R.string.debug,
                        R.drawable.ic_debug,
                        Intent(context, DebugActivity::class.java)
                )
        )

        val adapter = ListAdapter(requireContext(), items)
        more_recycler_view.layoutManager = LinearLayoutManager(context)
        more_recycler_view.adapter = adapter

        setupLogoAnim()
    }

    fun setToolbarTitle(title: CharSequence, showShadow: Boolean) {
        toolbar_title.text = title
        app_bar.elevation = if (showShadow) {
            resources.getDimension(R.dimen.app_bar_elevation)
        } else {
            0f
        }
    }

    fun setToolbarTitle(@StringRes title: Int, showShadow: Boolean) {
        if (context != null) {
            setToolbarTitle(requireContext().getString(title), showShadow)
        }
    }

    fun setToolbarSubTitle(subtitle: CharSequence) {
        if (subtitle == "") {
            toolbar_subtitle.visibility = View.GONE
        } else {
            toolbar_subtitle.visibility = View.VISIBLE
            toolbar_subtitle.text = subtitle
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun MoreFragment.setupLogoAnim() {
        val runnable = Runnable {
            val anim = AnimationUtils.loadAnimation(context, R.anim.logo_anim)
            home_logo.startAnimation(anim)
        }

        val handler = Handler()
        toolbar_title.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> handler.postDelayed(runnable, 10000)
                MotionEvent.ACTION_UP -> handler.removeCallbacks(runnable)
            }
            true
        }
    }
}