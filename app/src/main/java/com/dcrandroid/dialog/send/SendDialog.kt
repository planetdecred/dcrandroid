/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.dialog.send

import android.app.Activity.RESULT_OK
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.text.HtmlCompat
import androidx.fragment.app.FragmentActivity
import com.dcrandroid.R
import com.dcrandroid.adapter.PopupItem
import com.dcrandroid.adapter.PopupUtil
import com.dcrandroid.data.Constants
import com.dcrandroid.data.TransactionData
import com.dcrandroid.dialog.FullScreenBottomSheetDialog
import com.dcrandroid.dialog.InfoDialog
import com.dcrandroid.view.util.AccountCustomSpinner
import com.dcrandroid.util.SnackBar
import com.dcrandroid.util.Utils
import com.dcrandroid.view.util.SCAN_QR_REQUEST_CODE
import dcrlibwallet.Dcrlibwallet
import dcrlibwallet.TxAuthor
import dcrlibwallet.TxFeeAndSize
import kotlinx.android.synthetic.main.fee_layout.*
import kotlinx.android.synthetic.main.send_page_amount_card.*
import kotlinx.android.synthetic.main.send_page_sheet.*
import kotlinx.coroutines.*
import java.util.*

class SendDialog(val fragmentActivity: FragmentActivity, dismissListener: DialogInterface.OnDismissListener) :
        FullScreenBottomSheetDialog(dismissListener), ViewTreeObserver.OnScrollChangedListener {

    private lateinit var sourceAccountSpinner: AccountCustomSpinner
    private lateinit var destinationAddressCard: DestinationAddressCard

    private lateinit var amountHelper: AmountInputHelper

    private var sendMax = false

    private val validForConstruct: Boolean
        get() {
            return (amountHelper.dcrAmount != null || sendMax) &&
                    destinationAddressCard.estimationAddress != null
        }

    private val validForSend: Boolean
        get() {
            return (amountHelper.dcrAmount != null || sendMax) &&
                    destinationAddressCard.destinationAddress != null
        }

    private var authoredTxData: AuthoredTxData? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.send_page_sheet, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        amountHelper = AmountInputHelper(amount_card, scrollToBottom).apply {
            amountChanged = this@SendDialog.amountChanged
        }

        sourceAccountSpinner = AccountCustomSpinner(activity!!.supportFragmentManager,
                source_account_spinner, R.string.source_account_picker_title, sourceAccountChanged)

        destinationAddressCard = DestinationAddressCard(context!!, dest_address_card, validateAddress).apply {
            addressChanged = this@SendDialog.addressChanged
            addressInputHelper.textChanged = this@SendDialog.addressChanged
            destinationAccountSpinner.selectedAccountChanged = destAccountChanged
        }

        send_scroll_view.viewTreeObserver.addOnScrollChangedListener(this)

        iv_send_max.setOnClickListener {
            sendMax = true
            constructTransaction()
        }

        send_next.setOnClickListener {
            if (!validForSend || authoredTxData == null) {
                // should never encounter this
                SnackBar.showText(context!!, R.string.send_invalid)
                return@setOnClickListener
            }

            if(!multiWallet.isSynced){
                SnackBar.showError(app_bar, R.string.not_connected)
                return@setOnClickListener
            }

            val transactionData = TransactionData().apply {
                dcrAmount = amountHelper.dcrAmount!!
                exchangeDecimal = amountHelper.exchangeDecimal

                sendMax = true

                sourceAccount = sourceAccountSpinner.selectedAccount!!

                destinationAddress = destinationAddressCard.destinationAddress!!
                destinationAccount = destinationAddressCard.destinationAccount
            }

            ConfirmTransaction(fragmentActivity, sendSuccess)
                    .setTxData(transactionData, authoredTxData!!)
                    .show(activity!!.supportFragmentManager, null)
        }

        clearEstimates()
    }

    override fun showOptionsMenu(v: View) {
        val items: Array<Any> = arrayOf(
                PopupItem(R.string.clear_fields)
        )

        PopupUtil.showPopup(v, items) { window, _ ->
            window.dismiss()

            // clear fields
            amountHelper.setAmountDcr(0) // clear
            destinationAddressCard.clear()
        }
    }

    override fun showInfo() {
        InfoDialog(context!!)
                .setDialogTitle(getString(R.string.send_dcr))
                .setMessage(getString(R.string.send_dcr_info))
                .setPositiveButton(getString(R.string.got_it), null)
                .show()
    }

    private val sourceAccountChanged: (AccountCustomSpinner) -> Unit = {
        amountHelper.selectedAccount = it.selectedAccount
        constructTransaction()
    }

    private val destAccountChanged: (AccountCustomSpinner) -> Unit = {
        constructTransaction()
    }

    private val addressChanged:() -> Unit = {
        constructTransaction()
    }

    val amountChanged: (Boolean) -> Unit = { byUser ->
        if(view != null){
            if (byUser) {
                sendMax = false
            }

            if (amountHelper.dcrAmount != null && amountHelper.dcrAmount!!.toDouble() > 0) {
                constructTransaction()
            } else {
                clearEstimates()
            }
        }
    }

    private val validateAddress: (String) -> Boolean = {
        sourceAccountSpinner.wallet.isAddressValid(it)
    }

    override fun onScrollChanged() {
        app_bar.elevation = if (send_scroll_view.scrollY == 0) {
            0f
        } else {
            resources.getDimension(R.dimen.app_bar_elevation)
        }
    }

    private val scrollToBottom: () -> Unit = {
        send_scroll_view.postDelayed({
            send_scroll_view.smoothScrollTo(0, send_scroll_view.bottom)
        }, 200)
    }

    private val sendSuccess:() -> Unit = {
        GlobalScope.launch(Dispatchers.Main){
            SnackBar.showText(context!!, R.string.transaction_sent)
            dismiss()
        }
    }

    private fun clearEstimates() = GlobalScope.launch(Dispatchers.Main) {
        send_next.isEnabled = false

        val zeroDcr = HtmlCompat.fromHtml(getString(R.string._dcr), 0)
        balance_after_send.text = zeroDcr

        if (amountHelper.exchangeDecimal == null) {
            tx_fee.text = zeroDcr
            total_cost.text = zeroDcr
        } else {
            val zeroDcrUsd = HtmlCompat.fromHtml(getString(R.string._dcr_usd), 0)
            tx_fee.text = zeroDcrUsd
            total_cost.text = zeroDcrUsd
        }

        tx_size.text = getString(R.string.x_bytes, 0)
    }

    private fun constructTransaction() = GlobalScope.launch(Dispatchers.Main) {
        if (!validForConstruct) {
            clearEstimates()
            return@launch
        }

        amountHelper.setError(null)

        if (!sendMax) {
            // validate amount
            if (amountHelper.enteredAmount != null && amountHelper.enteredAmount!!.scale() > amountHelper.maxDecimalPlaces) {
                var err = getString(R.string.amount_invalid_decimal_places)
                err = String.format(Locale.getDefault(), err, amountHelper.maxDecimalPlaces)
                amountHelper.setError(err)
                clearEstimates()
                return@launch
            }
        }

        try {
            authoredTxData = authorTx()
            send_next.isEnabled = validForSend

            tx_size.text = getString(R.string.x_bytes, authoredTxData!!.estSignedSize)
            balance_after_send.text = authoredTxData!!.balanceAfter

            tx_fee.text = authoredTxData!!.fee
            total_cost.text = authoredTxData!!.totalCost

            if (sendMax) {
                amountHelper.setAmountDcr(authoredTxData!!.amountAtom)
            }

        } catch (e: Exception) {
            e.printStackTrace()

            amountHelper.setError(Utils.translateError(context!!, e))
            clearEstimates()
        }
    }

    private fun authorTx(): AuthoredTxData {
        val amount = amountHelper.dcrAmount
        var amountAtom = when {
            sendMax -> 0
            else -> Dcrlibwallet.amountAtom(amount!!.toDouble())
        }

        val selectedAccount = sourceAccountSpinner.selectedAccount!!

        val txAuthor: TxAuthor
        val feeAndSize: TxFeeAndSize

        try {
            val requiredConfirmations = when {
                multiWallet.readBoolConfigValueForKey(Dcrlibwallet.SpendUnconfirmedConfigKey, Constants.DEF_SPEND_UNCONFIRMED) -> 0
                else -> Constants.REQUIRED_CONFIRMATIONS
            }

            txAuthor = sourceAccountSpinner.wallet.newUnsignedTx(selectedAccount.accountNumber, requiredConfirmations)
            println("Estimation address: ${destinationAddressCard.estimationAddress}")
            txAuthor.addSendDestination(destinationAddressCard.estimationAddress, amountAtom, sendMax)
            feeAndSize = txAuthor.estimateFeeAndSize()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }

        val feeAtom = feeAndSize.fee.atomValue

        if (sendMax) {
            amountAtom = selectedAccount.balance.spendable - feeAtom
        }

        val totalCostAtom = amountAtom + feeAtom
        val balanceAfterSend = getString(R.string.x_dcr, Utils.formatDecredWithComma(selectedAccount.balance.spendable - totalCostAtom))

        val feeString = Utils.formatDecredWithComma(feeAtom)
        val totalCostString = Utils.formatDecredWithComma(totalCostAtom)

        val feeSpanned: Spanned
        val totalCostSpanned: Spanned

        if (amountHelper.exchangeDecimal == null) {
            feeSpanned = SpannableString(getString(R.string.x_dcr, feeString))
            totalCostSpanned = SpannableString(getString(R.string.x_dcr, totalCostString))
        } else {
            val feeCoin = Dcrlibwallet.amountCoin(feeAtom)
            val totalCostCoin = Dcrlibwallet.amountCoin(totalCostAtom)

            val feeUSD = dcrToFormattedUSD(amountHelper.exchangeDecimal, feeCoin, 4)
            val totalCostUSD = dcrToFormattedUSD(amountHelper.exchangeDecimal, totalCostCoin, 2)

            feeSpanned = HtmlCompat.fromHtml(getString(R.string.x_dcr_usd, feeString, feeUSD), 0)
            totalCostSpanned = HtmlCompat.fromHtml(getString(R.string.x_dcr_usd, totalCostString, totalCostUSD), 0)
        }

        val txData = AuthoredTxData().apply {
            estSignedSize = feeAndSize.estimatedSignedSize
            fee = feeSpanned
            totalCost = totalCostSpanned
            balanceAfter = balanceAfterSend

            this.amountAtom = amountAtom

            this.txAuthor = txAuthor
        }

        return txData
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == SCAN_QR_REQUEST_CODE && resultCode == RESULT_OK){
            val result = data!!.getStringExtra(Constants.RESULT).replace("decred:", "")
            destinationAddressCard.addressInputHelper.scanQRSuccess(result)
        }
    }
}

class AuthoredTxData {
    var estSignedSize: Long = 0
    lateinit var fee: Spanned
    lateinit var totalCost: Spanned
    lateinit var balanceAfter: String

    var amountAtom: Long = 0

    lateinit var txAuthor: TxAuthor
}