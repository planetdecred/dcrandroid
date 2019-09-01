/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.fragments

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.dcrandroid.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.fragment_password_pin_dialog.*

interface DialogButtonListener {

    fun onClickOk(spendingKey: String)

    fun onClickCancel()
}

class PasswordPinDialogFragment : BottomSheetDialogFragment(), DialogButtonListener {

    private lateinit var spendingPasswordFragment: PassphrasePromptFragment
    private lateinit var spendingPinFragment: PassphrasePromptFragment
    private lateinit var fragmentList: List<Fragment>
    private lateinit var tabsTitleList: List<String>
    private lateinit var titleList: List<String>
    private var passwordPinListener: PasswordPinListener? = null
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        spendingPasswordFragment = PassphrasePromptFragment(this, true)
        spendingPinFragment = PassphrasePromptFragment(this, false)

        fragmentList = listOf(spendingPasswordFragment, spendingPinFragment)
        tabsTitleList = listOf(context!!.getString(R.string.password), context!!.getString(R.string.pin))
        titleList = listOf(context!!.getString(R.string.create_spending_pass), context!!.getString(R.string.create_spending_pin))
    }

    override fun getTheme(): Int = R.style.BottomSheetDialogStyle

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)

        val dialog: Dialog = BottomSheetDialog(requireContext(), theme)

        dialog.setOnShowListener {
            val bottomSheetDialog = dialog as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<FrameLayout>(R.id.design_bottom_sheet)
            bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet!!)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            bottomSheetBehavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, slideOffset: Float) {

                }

                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                    }
                }

            })
        }

        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_password_pin_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view_pager.adapter = ViewPagerAdapter(childFragmentManager, fragmentList, tabsTitleList)
        tab_layout.setupWithViewPager(view_pager)
        tab_layout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener{
            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab!!.position == 0) {
                    tv_title.text = titleList[0]
                } else {
                    tv_title.text = titleList[1]
                }
            }

        })
    }

    override fun onClickOk(spendingKey: String) {
        val normalColor = Color.parseColor("#c4cbd2")
        tab_layout.setSelectedTabIndicatorColor(normalColor)
        tab_layout.setTabTextColors(normalColor, normalColor)
        // disable click on any tabs
        val tabStrip = tab_layout.getChildAt(0) as LinearLayout
        for (i in 0 until tabStrip.childCount) {
            tabStrip.getChildAt(i).setOnTouchListener { v, event -> true }
        }

        val isPassword = view_pager.currentItem == 0
        passwordPinListener?.onEnterPasswordOrPin(spendingKey, isPassword)
    }

    override fun onClickCancel() {
        dismiss()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val parent = parentFragment
        if (parent != null) {
            passwordPinListener = parent as PasswordPinListener
        } else {
            passwordPinListener = context as PasswordPinListener
        }
    }

    override fun onDetach() {
        passwordPinListener = null
        super.onDetach()
    }

    interface PasswordPinListener {
        fun onEnterPasswordOrPin(spendingKey: String, isPassword: Boolean)
    }

    class ViewPagerAdapter(fragmentManager: FragmentManager,
                           private val fragmentList: List<Fragment>,
                           private val tabsTitles: List<String>) : FragmentPagerAdapter(fragmentManager) {

        override fun getItem(position: Int): Fragment {
            return fragmentList[position]
        }

        override fun getCount(): Int {
            return fragmentList.size
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return tabsTitles[position]
        }
    }
}
