/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.fragments

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.dcrandroid.HomeActivity
import com.dcrandroid.R
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.show
import com.dcrandroid.util.WalletData
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dcrlibwallet.Dcrlibwallet
import dcrlibwallet.LibWallet
import kotlinx.android.synthetic.main.account_discovery_sheet.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception

class ResumeAccountDiscovery: BottomSheetDialogFragment() {

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>

    private var wallet: LibWallet? = null

    fun setWalletID(walletID: Long): ResumeAccountDiscovery{
        val multiWallet = WalletData.instance.multiWallet
        this.wallet = multiWallet!!.getWallet(walletID)
        return this
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(wallet == null){
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

        if(wallet!!.spendingPassphraseType == Dcrlibwallet.SpendingPassphraseTypePin){
            input_layout.hint = getString(R.string.spending_pin)
            resume_restore_pass.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }

        if(WalletData.instance.multiWallet!!.openedWalletsCount() > 1){
            unlock_title.text = getString(R.string.multi_resume_account_discovery_title, wallet!!.walletName)
        }

        resume_restore_pass.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                tv_create.isEnabled = !resume_restore_pass.text.isNullOrEmpty()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        tv_create.setOnClickListener{unlockWallet()}
    }

    private fun unlockWallet() = GlobalScope.launch(Dispatchers.Main){

        try{
            input_layout.error = null
            tv_create.hide()
            resume_restore_pass.isEnabled = false
            discovery_progress_bar.show()

            withContext(Dispatchers.Default){
                val pass = resume_restore_pass.text.toString()
                wallet?.unlockWallet(pass.toByteArray())
            }

            dismiss()

            if(activity is HomeActivity){
                val homeActivity = activity as HomeActivity
                homeActivity.startSyncing()
            }
        }catch (e: Exception){
            e.printStackTrace()

            if (e.message!! == Dcrlibwallet.ErrInvalidPassphrase){
                input_layout.error = if (wallet!!.spendingPassphraseType == Dcrlibwallet.SpendingPassphraseTypePin){
                    getString(R.string.invalid_pin)
                }else{
                    getString(R.string.invalid_pin)
                }
            }
        }

        tv_create.show()
        resume_restore_pass.isEnabled = true
        discovery_progress_bar.hide()


    }
}