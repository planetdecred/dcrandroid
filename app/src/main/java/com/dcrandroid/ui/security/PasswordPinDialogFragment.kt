package com.dcrandroid.ui.security

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.dcrandroid.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
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
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        spendingPasswordFragment = SpendingPasswordFragment(this)
        spendingPinFragment = SpendingPinFragment(this)
        fragmentList = listOf(spendingPasswordFragment, spendingPinFragment)
        tabsTitleList = listOf(context!!.getString(com.dcrandroid.R.string.password), context!!.getString(R.string.pin))
        titleList = listOf(context!!.getString(R.string.create_spending_pass), context!!.getString(R.string.create_spending_pin))
    }

    override fun getTheme(): Int = R.style.BottomSheetDialogStyle

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        val dialog: Dialog = BottomSheetDialog(requireContext(), theme)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        dialog.setOnShowListener {
            Handler().postDelayed({
                val bottomSheetDialog = dialog as BottomSheetDialog
                val bottomSheet = bottomSheetDialog.findViewById<FrameLayout>(R.id.design_bottom_sheet)
                bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet!!)
                bottomSheetBehavior.state = STATE_EXPANDED
            }, 300)
        }
        return dialog
    }

    /*override fun onStart() {
        super.onStart()

        val bottomSheet = dialog?.findViewById<View>(R.id.design_bottom_sheet) as FrameLayout?
        bottomSheet?.layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT
        view?.post {
            val parent = view?.parent as View
            val params = parent.layoutParams as CoordinatorLayout.LayoutParams
            params.topMargin = 40
            val bottomSheetBehavior = params.behavior as BottomSheetBehavior
            view?.measuredHeight?.let { bottomSheetBehavior.peekHeight = it }
        }
    }*/

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
        val normalColor = Color.parseColor("#c4cbd2")
        tab_layout.setSelectedTabIndicatorColor(normalColor)
        tab_layout.setTabTextColors(normalColor, normalColor)
        // disable click on any tabs
        val tabStrip = tab_layout.getChildAt(0) as LinearLayout
        for (i in 0 until tabStrip.childCount) {
            tabStrip.getChildAt(i).setOnTouchListener { v, event -> true }
        }

        // drag the dialog down
        if (bottomSheetBehavior.state == STATE_EXPANDED) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
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
