/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.fragments

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.view.animation.AnimationUtils
import androidx.annotation.StringRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.HomeActivity
import com.dcrandroid.R
import com.dcrandroid.activities.RESTORE_WALLET_REQUEST_CODE
import com.dcrandroid.activities.RestoreWalletActivity
import com.dcrandroid.adapter.PopupItem
import com.dcrandroid.adapter.PopupUtil
import com.dcrandroid.adapter.WalletsAdapter
import com.dcrandroid.data.Constants
import com.dcrandroid.dialog.CreateWatchOnlyWallet
import com.dcrandroid.dialog.FullScreenBottomSheetDialog
import com.dcrandroid.dialog.InfoDialog
import com.dcrandroid.dialog.RequestNameDialog
import com.dcrandroid.util.SnackBar
import com.dcrandroid.util.Utils
import dcrlibwallet.Dcrlibwallet
import kotlinx.android.synthetic.main.toolbar_layout.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.floor


const val VERIFY_SEED_REQUEST_CODE = 200
const val WALLET_SETTINGS_REQUEST_CODE = 300
const val PRIVACY_SETTINGS_REQUEST_CODE = 400

class WalletsFragment : BaseFragment() {

    private lateinit var adapter: WalletsAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_wallets, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.wallets_list_rv)
        setToolbarTitle(R.string.wallets, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        adapter = WalletsAdapter(this) { intent, requestCode ->
            startActivityForResult(intent, requestCode)
        }

        val layoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = layoutManager
        recyclerView.isNestedScrollingEnabled = false
        recyclerView.adapter = adapter

        recyclerView.viewTreeObserver.addOnScrollChangedListener {
            if (layoutManager.findFirstCompletelyVisibleItemPosition() == 0) {
                setToolbarTitle(R.string.wallets, false)
            } else {
                setToolbarTitle(R.string.wallets, true)
            }
        }

        setupLogoAnim()
    }

    override fun onPause() {
        super.onPause()
        adapter.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == VERIFY_SEED_REQUEST_CODE && resultCode == RESULT_OK) {

            val walletID = data!!.getLongExtra(Constants.WALLET_ID, -1)
            adapter.walletBackupVerified(walletID)
            refreshNavigationTabs()

        } else if (requestCode == WALLET_SETTINGS_REQUEST_CODE && resultCode == RESULT_OK) {
            adapter.reloadList()
            refreshNavigationTabs()
        } else if (requestCode == RESTORE_WALLET_REQUEST_CODE && resultCode == RESULT_OK) {
            val walletID = data?.getLongExtra(Constants.WALLET_ID, -1)
            adapter.addWallet(walletID!!)
            SnackBar.showText(requireContext(), R.string.wallet_created)
        } else if (requestCode == PRIVACY_SETTINGS_REQUEST_CODE) {
            adapter.reloadList()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.accounts_page_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_new_wallet -> {

                if (multiWallet!!.openedWalletsCount() >= numOfAllowedWallets()) {
                    InfoDialog(requireContext())
                            .setMessage(getString(R.string.wallets_limit_error))
                            .setPositiveButton(getString(R.string.ok))
                            .show()
                    return false
                }

                if (activity is HomeActivity) {
                    val homeActivity = activity as HomeActivity
                    val anchorView = homeActivity.findViewById<View>(R.id.add_new_wallet)

                    val items: Array<Any> = arrayOf(
                            PopupItem(R.string.create_a_new_wallet),
                            PopupItem(R.string.import_existing_wallet),
                            PopupItem(R.string.import_watching_only_wallet)
                    )

                    PopupUtil.showPopup(anchorView, items) { window, index ->
                        window.dismiss()
                        when (index) {
                            0 -> {

                                RequestNameDialog(R.string.wallet_name, "", true) { newName ->
                                    try {
                                        if (multiWallet!!.walletNameExists(newName)) {
                                            return@RequestNameDialog Exception(Dcrlibwallet.ErrExist)
                                        }

                                        PasswordPinDialogFragment(
                                                R.string.create,
                                                isSpending = true,
                                                isChange = false
                                        ) { dialog, passphrase, passphraseType ->
                                            createWallet(
                                                    dialog,
                                                    newName,
                                                    passphrase,
                                                    passphraseType
                                            )
                                        }.show(requireContext())

                                    } catch (e: Exception) {
                                        return@RequestNameDialog e
                                    }
                                    return@RequestNameDialog null
                                }.show(requireContext())
                            }
                            1 -> {
                                val restoreIntent =
                                        Intent(requireContext(), RestoreWalletActivity::class.java)
                                startActivityForResult(restoreIntent, RESTORE_WALLET_REQUEST_CODE)
                            }
                            2 -> {
                                CreateWatchOnlyWallet {
                                    SnackBar.showText(
                                            requireContext(),
                                            R.string.watch_only_wallet_created
                                    )
                                    adapter.addWallet(it.id)
                                }.show(requireContext())
                            }
                        }
                    }
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun numOfAllowedWallets(): Int {
        val actManager =
                requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)

        return floor(memInfo.totalMem / 1e9).toInt()
    }

    private fun createWallet(
            dialog: FullScreenBottomSheetDialog,
            walletName: String,
            spendingKey: String,
            type: Int
    ) = GlobalScope.launch(Dispatchers.IO) {
        val op = this@WalletsFragment.javaClass.name + ": createWallet"
        try {
            val wallet = multiWallet!!.createNewWallet(walletName, spendingKey, type)
            Utils.renameDefaultAccountToLocalLanguage(requireContext(), wallet)
            withContext(Dispatchers.Main) {
                dialog.dismiss()
                adapter.addWallet(wallet.id)
                refreshNavigationTabs() // to add the orange backup needed indicator to the tab icon
                SnackBar.showText(requireContext(), R.string.wallet_created)
            }
        } catch (e: Exception) {
            e.printStackTrace()

            withContext(Dispatchers.Main) {
                dialog.dismiss()
                Utils.showErrorDialog(requireContext(), op + ": " + e.message)
                Dcrlibwallet.logT(op, e.message)
            }
        }
    }

    override fun onTxOrBalanceUpdateRequired(walletID: Long?) {
        super.onTxOrBalanceUpdateRequired(walletID)

        GlobalScope.launch(Dispatchers.Main) {
            if (walletID == null) {
                adapter.reloadList()
            } else {
                adapter.updateWalletRow(walletID)
            }
        }
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
    private fun WalletsFragment.setupLogoAnim() {
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