/*
 * Copyright (c) 2018-2021 The Decred developers
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
import com.dcrandroid.data.Account
import com.dcrandroid.data.Constants
import com.dcrandroid.data.DecredAddressURI
import com.dcrandroid.data.TransactionData
import com.dcrandroid.dialog.FullScreenBottomSheetDialog
import com.dcrandroid.dialog.InfoDialog
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.show
import com.dcrandroid.util.CoinFormat
import com.dcrandroid.util.CurrencyUtil
import com.dcrandroid.util.SnackBar
import com.dcrandroid.util.Utils
import com.dcrandroid.view.util.AccountCustomSpinner
import com.dcrandroid.view.util.SCAN_QR_REQUEST_CODE
import dcrlibwallet.Dcrlibwallet
import dcrlibwallet.TxAuthor
import dcrlibwallet.TxFeeAndSize
import kotlinx.android.synthetic.main.fee_layout.*
import kotlinx.android.synthetic.main.send_page_amount_card.*
import kotlinx.android.synthetic.main.send_page_sheet.*
import kotlinx.android.synthetic.main.send_page_sheet.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

class SendDialog(
    val fragmentActivity: FragmentActivity,
    dismissListener: DialogInterface.OnDismissListener
) :
    FullScreenBottomSheetDialog(dismissListener), ViewTreeObserver.OnScrollChangedListener {

    private lateinit var sourceAccountSpinner: AccountCustomSpinner
    private lateinit var destinationAddressCard: DestinationAddressCard

    private lateinit var amountHelper: AmountInputHelper

    private var sendMax = false
        set(value) {
            if (value) {
                iv_send_max?.setImageResource(R.drawable.ic_send_max_enabled)
            } else {
                iv_send_max?.setImageResource(R.drawable.ic_send_max)
            }

            field = value
        }

    var savedInstanceState: Bundle? = null

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.send_page_sheet, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        amountHelper = AmountInputHelper(amount_card, scrollToBottom).apply {
            amountChanged = this@SendDialog.amountChanged
        }

        destinationAddressCard =
            DestinationAddressCard(requireContext(), dest_address_card, validateAddress).apply {
                addressChanged = destAddressChanged
                addressInputHelper.textChanged = destAddressChanged
                destinationAccountSpinner.selectedAccountChanged = destAccountChanged
            }

        sourceAccountSpinner = AccountCustomSpinner(
            requireActivity().supportFragmentManager,
            source_account_spinner, sourceAccountChanged
        )
        sourceAccountSpinner.init {
            // If wallet has privacy enabled, enable only mixed account when sending to an address
            // and enable all accounts when sending to an account
            val wallet = multiWallet.walletWithID(it.walletID)
            var accountIsEnabled =
                !wallet.isWatchingOnlyWallet // all accounts are enabled for non-privacy wallets

            if (wallet.readBoolConfigValueForKey(Dcrlibwallet.AccountMixerConfigSet, false)) {
                if (destinationAddressCard.isSendToAccount) {
                    // unmixed accounts are not valid for sending if destination account is another wallet
                    val destinationWalletID = destinationAddressCard.destinationAccount!!.walletID
                    if (destinationWalletID != it.walletID) {
                        accountIsEnabled = it.isMixerMixedAccount
                    }
                } else {
                    // only mixed account can send to an address
                    accountIsEnabled = it.isMixerMixedAccount
                }
            }

            accountIsEnabled
        }
        sourceAccountSpinner.pickerTitle = R.string.source_account_picker_title

        send_scroll_view.viewTreeObserver.addOnScrollChangedListener(this)

        iv_send_max.setOnClickListener {
            sendMax = true
            constructTransaction()
        }

        send_next.setOnClickListener {
            if (!validForSend || authoredTxData == null) {
                // should never encounter this
                SnackBar.showText(requireContext(), R.string.send_invalid)
                return@setOnClickListener
            }

            if (!multiWallet.isSynced) {
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
                .show(requireActivity().supportFragmentManager, null)
        }

        clearEstimates()
    }

    override fun onTxOrBalanceUpdateRequired(walletID: Long?) {
        super.onTxOrBalanceUpdateRequired(walletID)
        GlobalScope.launch(Dispatchers.Main) {
            sourceAccountSpinner.refreshBalance()

            if (destinationAddressCard.isSendToAccount) {
                destinationAddressCard.destinationAccountSpinner.refreshBalance()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        savedInstanceState = Bundle()
        // Included this boolean value to avoid insufficient balance error if true onResume.
        savedInstanceState?.putBoolean(Constants.SEND_MAX, sendMax)

        // Fetch and save the selected source account.
        val selectedSourceAccount = amountHelper.selectedAccount
        savedInstanceState?.putSerializable(
            Constants.SELECTED_SOURCE_ACCOUNT,
            selectedSourceAccount
        )

        // Fetch and save the selected destination account.
        val selectedDestAccount = destinationAddressCard.destinationAccountSpinner.selectedAccount
        savedInstanceState?.putSerializable(
            Constants.SELECTED_DESTINATION_ACCOUNT,
            selectedDestAccount
        )

        // Save destination address state depending on if spinner or address input was selected.
        savedInstanceState?.putBoolean(
            Constants.SEND_TO_ACCOUNT,
            destinationAddressCard.isSendToAccount
        )
    }

    override fun onResume() {
        super.onResume()
        destinationAddressCard.addressInputHelper.onResume()
        if (savedInstanceState != null) {
            // Fetch saved sendMax Value.
            sendMax = savedInstanceState!!.getBoolean(Constants.SEND_MAX)

            // Update UI based on previously selected send to account.
            val selectedSourceAccount =
                savedInstanceState!!.getSerializable(Constants.SELECTED_SOURCE_ACCOUNT) as Account?
            val selectedDestAccount =
                savedInstanceState!!.getSerializable(Constants.SELECTED_DESTINATION_ACCOUNT) as Account?

            // To avoid using an account object with invalid data, the account will be fetched from
            // the wallet instead of using the serialized object.
            if (selectedSourceAccount != null) {
                val wallet = multiWallet.walletWithID(selectedSourceAccount.walletID)
                sourceAccountSpinner.selectedAccount =
                    Account.from(wallet.getAccount(selectedSourceAccount.accountNumber))
            }
            if (selectedDestAccount != null) {
                val wallet = multiWallet.walletWithID(selectedDestAccount.walletID)
                destinationAddressCard.destinationAccountSpinner.selectedAccount =
                    Account.from(wallet.getAccount(selectedDestAccount.accountNumber))
            }

            // Show destination address input / spinner depending on which was previously selected.
            val sendToAccount = savedInstanceState!!.getBoolean(Constants.SEND_TO_ACCOUNT)
            if (sendToAccount) {
                destinationAddressCard.addressInputHelper.hide()
                destinationAddressCard.destinationAccountSpinner.show()
                destinationAddressCard.layout.send_dest_toggle.setText(R.string.send_to_address)
            } else {
                destinationAddressCard.addressInputHelper.show()
                destinationAddressCard.destinationAccountSpinner.hide()
                destinationAddressCard.layout.send_dest_toggle.setText(R.string.send_to_account)

            }
            savedInstanceState = null
        }
    }

    override fun showOptionsMenu(v: View) {
        val items: Array<Any> = arrayOf(
            PopupItem(R.string.clear_fields)
        )

        PopupUtil.showPopup(v, items) { window, _ ->
            window.dismiss()
            clearFields()
        }
    }

    private fun clearFields() {
        amountHelper.setAmountDCR(0) // clear
        destinationAddressCard.clear()
    }

    override fun showInfo() {
        InfoDialog(requireContext())
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
        sourceAccountSpinner.refreshSelectedAccount()
    }

    private val destAddressChanged: () -> Unit = {
        constructTransaction()
        sourceAccountSpinner.refreshSelectedAccount()
    }

    private val amountChanged: (Boolean) -> Unit = { byUser ->
        if (view != null) {
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
        multiWallet.isAddressValid(it)
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

    private val sendSuccess: (shouldExit: Boolean) -> Unit = {
        GlobalScope.launch(Dispatchers.Main) {
            if (it) {
                SnackBar.showText(requireContext(), R.string.transaction_sent)
                dismissAllowingStateLoss()
            } else {
                clearFields()
            }

        }
    }

    private fun clearEstimates() = GlobalScope.launch(Dispatchers.Main) {
        send_next.isEnabled = false

        val zeroDcr = HtmlCompat.fromHtml(getString(R.string._dcr), 0)
        balance_after_send.text = zeroDcr
        change_to_unmixed_label.hide()

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

            val wallet = multiWallet.walletWithID(sourceAccountSpinner.selectedAccount!!.walletID)
            val sourceAccountNumber = sourceAccountSpinner.selectedAccount!!.accountNumber
            if (!sendMax && (wallet.accountMixerMixChange() ||
                        wallet.mixedAccountNumber() == sourceAccountNumber) &&
                wallet.unmixedAccountNumber() != sourceAccountNumber &&
                authoredTxData!!.change > 0
            ) {

                val changeAccountName = wallet.accountName(wallet.unmixedAccountNumber())
                change_to_unmixed_label.apply {
                    text = HtmlCompat.fromHtml(
                        getString(
                            R.string.change_sent_to_unmixed,
                            CoinFormat.formatDecred(authoredTxData!!.change), changeAccountName
                        ), 0
                    )
                    show()
                }
            } else {
                change_to_unmixed_label.hide()
            }

            balance_after_send.text = authoredTxData!!.balanceAfter
            tx_fee.text = authoredTxData!!.fee
            total_cost.text = authoredTxData!!.totalCost

            if (sendMax) {
                amountHelper.setAmountDCR(authoredTxData!!.amountAtom)
            }

        } catch (e: Exception) {
            e.printStackTrace()

            amountHelper.setError(Utils.translateError(requireContext(), e))
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
            txAuthor = multiWallet.newUnsignedTx(
                sourceAccountSpinner.wallet.id,
                selectedAccount.accountNumber
            )
            txAuthor.addSendDestination(
                destinationAddressCard.estimationAddress,
                amountAtom,
                sendMax
            )
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

        val wallet = multiWallet.walletWithID(sourceAccountSpinner.selectedAccount!!.walletID)
        val balance = if ((wallet.accountMixerMixChange() || selectedAccount.isMixerMixedAccount) &&
            !selectedAccount.isMixerUnMixedAccount && feeAndSize.change != null
        ) {
            // Deduct change to get an accurate balance since the change will be sent to another account
            selectedAccount.balance.spendable - (totalCostAtom + feeAndSize.change.atomValue)
        } else {
            selectedAccount.balance.spendable - totalCostAtom
        }

        val balanceAfterSend = if (balance > 0) {
            getString(R.string.x_dcr, CoinFormat.formatDecred(balance))
        } else {
            getString(R.string.x_dcr, "0")
        }

        val feeString = CoinFormat.formatDecred(feeAtom)
        val totalCostString = CoinFormat.formatDecred(totalCostAtom)

        val feeSpanned: Spanned
        val totalCostSpanned: Spanned

        if (amountHelper.exchangeDecimal == null) {
            feeSpanned = SpannableString(getString(R.string.x_dcr, feeString))
            totalCostSpanned = SpannableString(getString(R.string.x_dcr, totalCostString))
        } else {
            val feeCoin = Dcrlibwallet.amountCoin(feeAtom)
            val totalCostCoin = Dcrlibwallet.amountCoin(totalCostAtom)

            val feeUSD = CurrencyUtil.dcrToFormattedUSD(amountHelper.exchangeDecimal, feeCoin)
            val totalCostUSD =
                CurrencyUtil.dcrToFormattedUSD(amountHelper.exchangeDecimal, totalCostCoin, 2)

            feeSpanned = HtmlCompat.fromHtml(getString(R.string.x_dcr_usd, feeString, feeUSD), 0)
            totalCostSpanned =
                HtmlCompat.fromHtml(getString(R.string.x_dcr_usd, totalCostString, totalCostUSD), 0)
        }

        return AuthoredTxData().apply {
            estSignedSize = feeAndSize.estimatedSignedSize
            fee = feeSpanned
            totalCost = totalCostSpanned
            balanceAfter = balanceAfterSend

            this.amountAtom = amountAtom
            if (feeAndSize.change != null) {
                this.change = feeAndSize.change.atomValue
            }

            this.txAuthor = txAuthor
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCAN_QR_REQUEST_CODE && resultCode == RESULT_OK) {
            val result = data!!.getStringExtra(Constants.RESULT)

            val decredAddressUri = DecredAddressURI.from(result!!)
            destinationAddressCard.addressInputHelper.editText.setText(decredAddressUri.address)
            if (decredAddressUri.amount != null) {
                sendMax = false
                amountHelper.setAmountDCR(decredAddressUri.amount!!)
            }
        }
    }
}

class AuthoredTxData {
    var estSignedSize: Long = 0
    lateinit var fee: Spanned
    lateinit var totalCost: Spanned
    lateinit var balanceAfter: String

    var amountAtom: Long = 0
    var change: Long = 0

    lateinit var txAuthor: TxAuthor
}