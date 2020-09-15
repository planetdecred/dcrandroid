/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.fragments

import android.app.Activity.RESULT_OK
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.HomeActivity
import com.dcrandroid.R
import com.dcrandroid.activities.RESTORE_WALLET_REQUEST_CODE
import com.dcrandroid.activities.RestoreWalletActivity
import com.dcrandroid.adapter.PopupDivider
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.floor


const val VERIFY_SEED_REQUEST_CODE = 200
const val WALLET_SETTINGS_REQUEST_CODE = 300

class WalletsFragment : BaseFragment() {

    private lateinit var adapter: WalletsAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_wallets, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.wallets_list_rv)
        setToolbarTitle(R.string.wallets, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        adapter = WalletsAdapter(context!!) { intent, requestCode ->
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
            SnackBar.showText(context!!, R.string.wallet_created)
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
                    InfoDialog(context!!)
                            .setMessage(getString(R.string.wallets_limit_error))
                            .setPositiveButton(getString(R.string.ok))
                            .show()
                    return false
                } else if (multiWallet!!.isConnectedToDecredNetwork) {
                    SnackBar.showError(context!!, R.string.disconnect_add_wallet)
                    return false
                }

                if (activity is HomeActivity) {
                    val homeActivity = activity as HomeActivity
                    val anchorView = homeActivity.findViewById<View>(R.id.add_new_wallet)

                    val dividerWidth = context!!.resources.getDimensionPixelSize(R.dimen.add_wallet_menu_width)

                    val items = arrayOf(
                            PopupItem(R.string.create_a_new_wallet),
                            PopupItem(R.string.import_existing_wallet),
                            PopupDivider(dividerWidth),
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

                                        PasswordPinDialogFragment(R.string.create, isSpending = true, isChange = false) { dialog, passphrase, passphraseType ->
                                            createWallet(dialog, newName, passphrase, passphraseType)
                                        }.show(context!!)

                                    } catch (e: Exception) {
                                        return@RequestNameDialog e
                                    }
                                    return@RequestNameDialog null
                                }.show(context!!)
                            }
                            1 -> {
                                val restoreIntent = Intent(context!!, RestoreWalletActivity::class.java)
                                startActivityForResult(restoreIntent, RESTORE_WALLET_REQUEST_CODE)
                            }
                            3 -> {
                                CreateWatchOnlyWallet {
                                    SnackBar.showText(context!!, R.string.watch_only_wallet_created)
                                    adapter.addWallet(it.id)
                                }.show(context!!)
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
        val actManager = context!!.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)

        return floor(memInfo.totalMem / 1e9).toInt()
    }

    private fun createWallet(dialog: FullScreenBottomSheetDialog, walletName: String, spendingKey: String, type: Int) = GlobalScope.launch(Dispatchers.IO) {
        val op = this@WalletsFragment.javaClass.name + ": createWallet"
        try {
            val wallet = multiWallet!!.createNewWallet(walletName, spendingKey, type)
            Utils.renameDefaultAccountToLocalLanguage(context!!, wallet)
            withContext(Dispatchers.Main) {
                dialog.dismiss()
                adapter.addWallet(wallet.id)
                refreshNavigationTabs() // to add the orange backup needed indicator to the tab icon
                SnackBar.showText(context!!, R.string.wallet_created)
            }
        } catch (e: Exception) {
            e.printStackTrace()

            withContext(Dispatchers.Main) {
                dialog.dismiss()
                Utils.showErrorDialog(this@WalletsFragment.context!!, op + ": " + e.message)
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
}