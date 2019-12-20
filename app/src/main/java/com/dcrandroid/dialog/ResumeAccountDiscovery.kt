/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.dcrandroid.HomeActivity
import com.dcrandroid.R
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.show
import com.dcrandroid.util.WalletData
import com.dcrandroid.view.PinViewUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dcrlibwallet.Dcrlibwallet
import dcrlibwallet.Wallet
import kotlinx.android.synthetic.main.account_discovery_sheet.*
import kotlinx.coroutines.*

class ResumeAccountDiscovery : BottomSheetDialogFragment() {

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>

    private var wallet: Wallet? = null

    private lateinit var pinViewUtil: PinViewUtil

    fun setWalletID(walletID: Long): ResumeAccountDiscovery {
        val multiWallet = WalletData.instance.multiWallet
        this.wallet = multiWallet!!.walletWithID(walletID)
        return this
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (wallet == null) {
            error("WalletID = null")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.account_discovery_sheet, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)

        val dialog: Dialog = BottomSheetDialog(requireContext(), theme)

        dialog.setOnShowListener {
            val bottomSheetDialog = dialog as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<FrameLayout>(R.id.design_bottom_sheet)
            bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet!!)
            bottomSheetBehavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                }

                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    }
                }
            })
        }

        return dialog
    }

    override fun getTheme(): Int = R.style.BottomSheetDialogStyle

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        pinViewUtil = PinViewUtil(resume_restore_pin, null)

        if (wallet!!.privatePassphraseType == Dcrlibwallet.PassphraseTypePin) {
            resume_restore_pass.hide()
            resume_restore_pin.show()

            pinViewUtil.showHint(R.string.enter_spending_pin)

            val bottomRowTopMargin = -resources.getDimensionPixelOffset(R.dimen.margin_padding_size_64)
            val bottomBarParams = bottom_row.layoutParams as LinearLayout.LayoutParams
            bottomBarParams.topMargin = bottomRowTopMargin
            bottom_row.layoutParams = bottomBarParams
        }

        if (WalletData.instance.multiWallet!!.openedWalletsCount() > 1) {
            unlock_title.text = getString(R.string.multi_resume_account_discovery_title, wallet!!.name)
        }

        pinViewUtil.pinChanged = {
            btn_unlock.isEnabled = it.isNotBlank()
            Unit
        }

        resume_restore_pass.validateInput = {
            btn_unlock.isEnabled = it.isNotBlank()
            true
        }

        btn_unlock.setOnClickListener { unlockWallet() }
    }

    private fun unlockWallet() = GlobalScope.launch(Dispatchers.Main) {

        try {
            resume_restore_pass.setError(null)
            btn_unlock.hide()
            resume_restore_pass.isEnabled = false
            discovery_progress_bar.show()

            withContext(Dispatchers.Default) {

                val pass = if (resume_restore_pass.isShown) {
                    resume_restore_pass.textString
                } else {
                    pinViewUtil.passCode
                }

                wallet?.unlockWallet(pass.toByteArray())
            }

            dismiss()

            if (activity is HomeActivity) {
                val homeActivity = activity as HomeActivity
                homeActivity.startSyncing()
            }
        } catch (e: Exception) {
            e.printStackTrace()

            if (e.message!! == Dcrlibwallet.ErrInvalidPassphrase) {

                val error = if (wallet!!.privatePassphraseType == Dcrlibwallet.PassphraseTypePass) {
                    getString(R.string.invalid_password)
                } else {
                    getString(R.string.invalid_pin)
                }

                if (resume_restore_pass.isShown) {
                    resume_restore_pass.setError(error)
                } else {
                    pinViewUtil.pinView.rejectInput = true
                    pinViewUtil.showError(R.string.invalid_pin)
                    btn_unlock.isEnabled = false

                    delay(2000)
                    withContext(Dispatchers.Main) {
                        pinViewUtil.reset()
                        pinViewUtil.showHint(R.string.enter_spending_pin)
                        pinViewUtil.pinView.rejectInput = false
                    }
                }

            }
        }

        btn_unlock.show()
        resume_restore_pass.isEnabled = true
        discovery_progress_bar.hide()


    }
}