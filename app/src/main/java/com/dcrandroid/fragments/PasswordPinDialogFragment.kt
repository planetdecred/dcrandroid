/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.dcrandroid.R
import com.dcrandroid.dialog.FullScreenBottomSheetDialog
import com.google.android.material.tabs.TabLayout
import dcrlibwallet.Dcrlibwallet
import kotlinx.android.synthetic.main.fragment_password_pin_dialog.*

class PasswordPinDialogFragment(@StringRes var positiveButtonTitle: Int, var isSpending: Boolean, var isChange: Boolean,
                                private val onPassphraseConfirmed: (dialog: FullScreenBottomSheetDialog, passphrase: String, passphraseType: Int) -> Unit) : FullScreenBottomSheetDialog() {

    private lateinit var spendingCreatePasswordFragment: CreatePasswordPromptFragment
    private lateinit var spendingCreatePinFragment: CreatePinPromptFragment

    private lateinit var fragmentList: List<Fragment>
    private lateinit var tabsTitleList: List<String>
    private lateinit var titleList: List<String>

    var tabIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        spendingCreatePasswordFragment = CreatePasswordPromptFragment(isSpending, positiveButtonTitle, handleCompletion)

        spendingCreatePinFragment = CreatePinPromptFragment(isSpending, positiveButtonTitle, handleCompletion)

        fragmentList = listOf(spendingCreatePasswordFragment, spendingCreatePinFragment)
        tabsTitleList = listOf(context!!.getString(R.string.password), context!!.getString(R.string.pin))
        titleList = if (isSpending) {
            if (isChange) {
                listOf(context!!.getString(R.string.change_spending_pass), context!!.getString(R.string.change_spending_pin))
            } else {
                listOf(context!!.getString(R.string.create_spending_pass), context!!.getString(R.string.create_spending_pin))
            }

        } else {
            if (isChange) {
                listOf(context!!.getString(R.string.change_startup_password), context!!.getString(R.string.change_startup_pin))
            } else {
                listOf(context!!.getString(R.string.create_startup_password), context!!.getString(R.string.create_startup_pin))
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_password_pin_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view_pager.adapter = ViewPagerAdapter(childFragmentManager, fragmentList, tabsTitleList)
        tab_layout.setupWithViewPager(view_pager)
        tab_layout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
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
                view_pager.requestLayout()
            }
        })

        tv_title.text = titleList[view_pager.currentItem]

        view_pager.post {
            view_pager.setCurrentItem(tabIndex, false)
        }
    }

    private val handleCompletion: (passphrase: String?) -> Unit = { passphrase ->
        if (passphrase == null) {
            dismiss()
        } else {
            isCancelable = false

            val disabledColor = resources.getColor(R.color.lightGray)
            tab_layout.setSelectedTabIndicatorColor(disabledColor)
            tab_layout.setTabTextColors(disabledColor, disabledColor)

            // disable tabs
            tab_layout.touchables.forEach { it.isEnabled = false }

            val passphraseType = if (view_pager.currentItem == 0) {
                Dcrlibwallet.PassphraseTypePass
            } else {
                Dcrlibwallet.PassphraseTypePin
            }

            onPassphraseConfirmed(this, passphrase, passphraseType)
        }
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
