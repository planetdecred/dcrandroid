/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.dialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.dcrandroid.R
import com.dcrandroid.data.Transaction
import com.dcrandroid.util.WalletData
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import dcrlibwallet.*

open class FullScreenBottomSheetDialog(val dismissListener: DialogInterface.OnDismissListener? = null) :
    BottomSheetDialogFragment(),
    SyncProgressListener, TxAndBlockNotificationListener {

    var TAG = this.javaClass.name
    var isForeground = false
    var requiresDataUpdate = false

    protected val multiWallet = WalletData.multiWallet!!

    override fun getTheme(): Int = R.style.BottomSheetDialogStyle

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)

        val dialog: Dialog = BottomSheetDialog(requireContext(), theme)

        dialog.setOnShowListener {
            val bottomSheetDialog = dialog as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<FrameLayout>(R.id.design_bottom_sheet)
            val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet!!)

            val wm = requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            wm.defaultDisplay.getMetrics(metrics)

            bottomSheetBehavior.peekHeight = metrics.heightPixels

            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            bottomSheetBehavior.addBottomSheetCallback(object :
                BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                }

                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        dismiss()
                    }
                }
            })
        }

        return dialog
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        view?.findViewById<ImageView>(R.id.go_back)?.setOnClickListener {
            dismiss()
        }

        view?.findViewById<ImageView>(R.id.iv_info)?.setOnClickListener {
            showInfo()
        }

        view?.findViewById<ImageView>(R.id.iv_options)?.setOnClickListener {
            showOptionsMenu(it)
        }
    }

    open fun showInfo() {}
    open fun showOptionsMenu(v: View) {}

    // For password and pin dialog
    open fun setProcessing(processing: Boolean) {
        throw IllegalAccessException()
    }

    // For password and pin dialog
    open fun showError() {
        throw IllegalAccessException()
    }


    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        dismissListener?.onDismiss(dialog)
    }

    fun show(context: Context) {
        val supportFragmentManager = (context as AppCompatActivity).supportFragmentManager
        super.show(supportFragmentManager, javaClass.name)
//        if (supportFragmentManager.findFragmentByTag(this::class.java.name) == null) {
//            super.show(supportFragmentManager, javaClass.name)
//        }

    }

    override fun onResume() {
        super.onResume()
        multiWallet.removeSyncProgressListener(TAG)
        multiWallet.removeTxAndBlockNotificationListener(TAG)

        multiWallet.addSyncProgressListener(this, TAG)
        multiWallet.addTxAndBlockNotificationListener(this, true, TAG)

        isForeground = true
        if (requiresDataUpdate) {
            requiresDataUpdate = false
            onTxOrBalanceUpdateRequired(null)
        }
    }

    override fun onPause() {
        super.onPause()
        isForeground = false
    }

    override fun onDestroy() {
        super.onDestroy()
        multiWallet.removeSyncProgressListener(TAG)
        multiWallet.removeTxAndBlockNotificationListener(TAG)
    }

    open fun onTxOrBalanceUpdateRequired(walletID: Long?) {
        if (!isForeground) {
            requiresDataUpdate = true
            return
        }
    }

    override fun onCFiltersFetchProgress(cfiltersFetchProgress: CFiltersFetchProgressReport?) {

    }

    override fun onHeadersRescanProgress(p0: HeadersRescanProgressReport?) {

    }

    override fun onAddressDiscoveryProgress(p0: AddressDiscoveryProgressReport?) {

    }

    override fun onSyncCanceled(p0: Boolean) {

    }

    override fun onPeerConnectedOrDisconnected(p0: Int) {

    }

    override fun onSyncCompleted() {
        onTxOrBalanceUpdateRequired(null)
    }

    override fun onSyncStarted(p0: Boolean) {

    }

    override fun onHeadersFetchProgress(p0: HeadersFetchProgressReport?) {

    }

    override fun onSyncEndedWithError(p0: Exception?) {

    }

    override fun debug(p0: DebugInfo?) {

    }

    override fun onBlockAttached(walletID: Long, blockHeight: Int) {
        onTxOrBalanceUpdateRequired(walletID)
    }

    override fun onTransactionConfirmed(walletID: Long, hash: String, blockHeight: Int) {
        onTxOrBalanceUpdateRequired(walletID)
    }

    override fun onTransaction(transactionJson: String?) {
        val transaction = Gson().fromJson(transactionJson, Transaction::class.java)
        onTxOrBalanceUpdateRequired(transaction.walletID)
    }
}