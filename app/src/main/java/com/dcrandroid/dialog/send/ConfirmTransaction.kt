/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.dialog.send

import android.os.Bundle
import android.text.Spannable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import com.dcrandroid.R
import com.dcrandroid.data.Account
import com.dcrandroid.data.TransactionData
import com.dcrandroid.dialog.CollapsedBottomSheetDialog
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.show
import com.dcrandroid.util.CoinFormat
import com.dcrandroid.util.PassPromptTitle
import com.dcrandroid.util.PassPromptUtil
import com.dcrandroid.util.SnackBar
import dcrlibwallet.LibWallet
import kotlinx.android.synthetic.main.confirm_send_sheet.*
import kotlinx.coroutines.*
import java.math.RoundingMode

class ConfirmTransaction(val sendSuccess:() -> Unit): CollapsedBottomSheetDialog() {

    lateinit var wallet: LibWallet


    private val selectedAccount: Account
    get() = transactionData.sourceAccount

    lateinit var transactionData: TransactionData
    lateinit var authoredTxData: AuthoredTxData

    fun setTxData(transactionData: TransactionData, authoredTxData: AuthoredTxData): CollapsedBottomSheetDialog{
        this.transactionData = transactionData
        this.authoredTxData = authoredTxData
        this.wallet = multiWallet!!.getWallet(transactionData.sourceAccount.walletID)
        return this
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.confirm_send_sheet, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // set up layout
        send_from_account_name.text = HtmlCompat.fromHtml(getString(R.string.send_from_account,
                selectedAccount.accountName, wallet.name), 0)

        val dcrAmount = dcrFormat.format(transactionData.dcrAmount.setScale(8, RoundingMode.HALF_EVEN).toDouble())
        val amountStr = if(transactionData.exchangeDecimal != null) {
            val usdAmount = dcrToFormattedUSD(transactionData.exchangeDecimal, transactionData.dcrAmount.toDouble())
            HtmlCompat.fromHtml(getString(R.string.x_dcr_usd, dcrAmount, usdAmount), 0)
        }else{
            getString(R.string.x_dcr, dcrAmount)
        }
        if(amountStr is Spannable) {
            CoinFormat.formatSpannable(amountStr, AmountRelativeSize)
            send_amount.text = amountStr
        }else{
            send_amount.text = CoinFormat.format(amountStr as String, AmountRelativeSize)
        }


        tx_fee.text = authoredTxData.fee
        total_cost.text = authoredTxData.totalCost
        balance_after_send.text = authoredTxData.balanceAfter
        send_btn.text = getString(R.string.send_x_dcr, dcrAmount)

        // address & account
        if(transactionData.destinationAccount == null){
            address_account_name.text = transactionData.destinationAddress
        }else{
            confirm_dest_type.setText(R.string.to_self)

            val destinationAccount = transactionData.destinationAccount!!
            val receivingWallet = multiWallet!!.getWallet(destinationAccount.walletID)
            address_account_name.text = HtmlCompat.fromHtml(getString(R.string.selected_account_name,
                    destinationAccount.accountName, receivingWallet.name), 0)
        }

        send_btn.setOnClickListener {
            showProcessing()

            val title = PassPromptTitle(R.string.confirm_to_send, R.string.confirm_to_send, R.string.confirm_to_send)
            PassPromptUtil(context!!, wallet.walletID, true, title){pass ->
                if(pass == null){
                    showSendButton()
                    return@PassPromptUtil
                }

                GlobalScope.launch(Dispatchers.Default){
                    try {
                        authoredTxData.txAuthor.broadcast(pass.toByteArray())
                        showSuccess()
                    }catch (e: Exception){
                        showSendButton()
                        SnackBar.showError(container!!, R.string.send_fail_msg)
                        e.printStackTrace()
                    }
                }
            }.show()
        }
    }

    private fun showSendButton() = GlobalScope.launch(Dispatchers.Main){
        send_btn.show()
        processing_layout.hide()
        success_layout.hide()
        go_back.isEnabled = true
        isCancelable = true
    }

    private fun showSuccess() = GlobalScope.launch(Dispatchers.Main){
        success_layout.show()
        send_btn.hide()
        processing_layout.hide()
        go_back.isEnabled = false
        isCancelable = false

        withContext(Dispatchers.Default){
            delay(5000)
            dismiss()
            sendSuccess()
        }
    }

    private fun showProcessing()  = GlobalScope.launch(Dispatchers.Main){
        processing_layout.show()
        send_btn.hide()
        success_layout.hide()
        go_back.isEnabled = false
        isCancelable = false
    }
}