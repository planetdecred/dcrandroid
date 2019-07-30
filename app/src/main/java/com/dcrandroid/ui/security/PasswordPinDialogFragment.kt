package com.dcrandroid.ui.security

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.dcrandroid.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.fragment_password_pin_dialog.*

class PasswordPinDialogFragment : BottomSheetDialogFragment(), DialogButtonListener {

    private lateinit var spendingPasswordFragment: SpendingPasswordFragment
    private lateinit var spendingPinFragment: SpendingPinFragment
    private lateinit var fragmentList: List<Fragment>
    private lateinit var tabsTitleList: List<String>
    private lateinit var titleList: List<String>
    private var passwordPinListener: PasswordPinListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        spendingPasswordFragment = SpendingPasswordFragment(this)
        spendingPinFragment = SpendingPinFragment(this);
        fragmentList = listOf(spendingPasswordFragment, spendingPinFragment)
        tabsTitleList = listOf(context!!.getString(R.string.password), context!!.getString(R.string.pin))
        titleList = listOf(context!!.getString(R.string.create_spending_pass), context!!.getString(R.string.create_spending_pin))
    }

    override fun getTheme(): Int = R.style.BottomSheetDialogStyle

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = BottomSheetDialog(requireContext(), theme)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_password_pin_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view_pager.adapter = ViewPagerAdapter(childFragmentManager, fragmentList, tabsTitleList)
        tab_layout.setupWithViewPager(view_pager)
        tab_layout.addOnTabSelectedListener(TabSelectedListener(titleList, tv_title))
    }

    override fun onClickOk(spendingKey: String) {
        if (view_pager.currentItem == 0) {
            passwordPinListener?.onEnterPasswordOrPin(spendingKey, true)
        }

        if (view_pager.currentItem == 1) {
            passwordPinListener?.onEnterPasswordOrPin(spendingKey, false)
        }
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

    class TabSelectedListener(private val titleList: List<String>,
                              private val titleView: TextView) : TabLayout.OnTabSelectedListener {

        override fun onTabReselected(tab: TabLayout.Tab?) {

        }

        override fun onTabUnselected(tab: TabLayout.Tab?) {

        }

        override fun onTabSelected(tab: TabLayout.Tab?) {

            if (tab!!.position == 0) {
                titleView.text = titleList[0]
            } else {
                titleView.text = titleList[1]
            }
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
