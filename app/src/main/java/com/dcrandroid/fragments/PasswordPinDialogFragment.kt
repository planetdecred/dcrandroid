/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.dcrandroid.R
import com.dcrandroid.dialog.FullScreenBottomSheetDialog
import com.google.android.material.tabs.TabLayout
import dcrlibwallet.Dcrlibwallet
import kotlinx.android.synthetic.main.fragment_password_pin_dialog.*

interface DialogButtonListener {
    fun onClickOk(newPassphrase: String)
    fun onClickCancel()
}

class PasswordPinDialogFragment(@StringRes var positiveButtonTitle: Int, var isSpending: Boolean, var isChange: Boolean,
                                private val passwordPinListener: PasswordPinListener) : FullScreenBottomSheetDialog(), DialogButtonListener {

    private lateinit var spendingCreatePasswordFragment: CreatePasswordPromptFragment
    private lateinit var spendingCreatePinFragment: CreatePinPromptFragment

    private lateinit var fragmentList: List<Fragment>
    private lateinit var tabsTitleList: List<String>
    private lateinit var titleList: List<String>

    var tabIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        spendingCreatePasswordFragment = CreatePasswordPromptFragment(isSpending, positiveButtonTitle, this)

        spendingCreatePinFragment = CreatePinPromptFragment(isSpending, positiveButtonTitle, this)

        fragmentList = listOf(spendingCreatePasswordFragment, spendingCreatePinFragment)
        tabsTitleList = listOf(context!!.getString(R.string.password), context!!.getString(R.string.pin))
        titleList = if(isSpending){
            if(isChange){
                listOf(context!!.getString(R.string.change_spending_pass), context!!.getString(R.string.change_spending_pin))
            }else{
                listOf(context!!.getString(R.string.create_spending_pass),  context!!.getString(R.string.create_spending_pin))
            }

        }else{
            if(isChange){
                listOf(context!!.getString(R.string.change_startup_password), context!!.getString(R.string.change_startup_pin))
            }else{
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
                view_pager.requestLayout()
            }
        })

        tv_title.text = titleList[view_pager.currentItem]

        view_pager.post {
            view_pager.setCurrentItem(tabIndex, false)
        }
    }

    override fun onClickOk(spendingKey: String) {
        isCancelable = false

        val normalColor = Color.parseColor("#c4cbd2")
        tab_layout.setSelectedTabIndicatorColor(normalColor)
        tab_layout.setTabTextColors(normalColor, normalColor)
        // disable click on any tabs
        val tabStrip = tab_layout.getChildAt(0) as LinearLayout
        for (i in 0 until tabStrip.childCount) {
            tabStrip.getChildAt(i).setOnTouchListener { _, _ -> true }
        }

        val passphraseType = if (view_pager.currentItem == 0){
            Dcrlibwallet.PassphraseTypePass
        }else{
            Dcrlibwallet.PassphraseTypePin
        }
        passwordPinListener.onEnterPasswordOrPin(spendingKey, passphraseType)
    }

    override fun onClickCancel() {
        dismiss()
    }

    interface PasswordPinListener {
        fun onEnterPasswordOrPin(newPassphrase: String, passphraseType: Int)
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
