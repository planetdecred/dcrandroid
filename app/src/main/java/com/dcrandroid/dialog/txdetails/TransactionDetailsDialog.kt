/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.dialog.txdetails

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.text.HtmlCompat
import com.dcrandroid.BuildConfig
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.data.Transaction
import com.dcrandroid.dialog.FullScreenBottomSheetDialog
import com.dcrandroid.dialog.InfoDialog
import com.dcrandroid.extensions.openedWalletsList
import com.dcrandroid.extensions.show
import com.dcrandroid.extensions.toggleVisibility
import com.dcrandroid.util.CoinFormat
import com.dcrandroid.util.Utils
import dcrlibwallet.Dcrlibwallet
import kotlinx.android.synthetic.main.transaction_details.*
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class TransactionDetailsDialog(val transaction: Transaction) : FullScreenBottomSheetDialog(null), View.OnClickListener, ViewTreeObserver.OnScrollChangedListener {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.transaction_details, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        tx_details_icon.setImageResource(transaction.iconResource)
        tx_details_amount.text = CoinFormat.format(transaction.amount, 0.625f)

        val sdf = SimpleDateFormat("MMM dd, yyyy hh:mma", Locale.getDefault())

        val symbols = DateFormatSymbols(Locale.getDefault())
        symbols.amPmStrings = arrayOf(getString(R.string.am), getString(R.string.pm))
        sdf.dateFormatSymbols = symbols
        val timestamp = sdf.format(transaction.timestampMillis)
        tx_timestamp.text = timestamp

        val spendUnconfirmedFunds = multiWallet.readBoolConfigValueForKey(Dcrlibwallet.SpendUnconfirmedConfigKey, Constants.DEF_SPEND_UNCONFIRMED)

        status_icon.setImageResource(transaction.getConfirmationIconRes(spendUnconfirmedFunds))

        if (transaction.confirmations > 1 || spendUnconfirmedFunds) {
            tv_confirmations.text = HtmlCompat.fromHtml(getString(R.string.tx_details_confirmations, transaction.confirmations), 0)

            tx_block_row.show()
            tx_block.text = transaction.height.toString()
        } else {
            tv_confirmations.apply {
                setText(R.string.pending)
                setTextColor(context.getColor(R.color.lightGrayTextColor))
            }
        }

        view_dcrdata.setOnClickListener(this)
        iv_info.setOnClickListener(this)
        tx_details_id.setOnClickListener(this)
        tx_details_id.text = transaction.hash
        tx_details_type.text = transaction.type
        tx_details_fee.text = getString(R.string.x_dcr, Utils.formatDecredWithComma(transaction.fee))

        when (transaction.type) {
            Dcrlibwallet.TxTypeRegular -> {
                when (transaction.direction) {
                    Dcrlibwallet.TxDirectionSent -> {
                        tx_source_row.show()
                        tx_details_source.text = getSourceAccount()

                        tx_source_wallet_badge.text = multiWallet.walletWithID(transaction.walletID).name
                        tx_source_wallet_badge.show()

                        tx_dest_row.show()
                        tx_details_dest.apply {
                            text = getDestinationAddress()
                            tx_details_dest.setOnClickListener(this@TransactionDetailsDialog)
                        }

                        toolbar_title.setText(R.string.sent)
                    }
                    Dcrlibwallet.TxDirectionReceived -> {
                        tx_source_row.show()
                        tx_details_source.setText(R.string.external)

                        tx_dest_row.show()
                        tx_dest_label.setText(R.string.to_account)
                        tx_details_dest.text = getReceiveAccount()
                        tx_details_dest.setTextColor(resources.getColor(R.color.darkBlueTextColor))

                        tx_dest_wallet_badge.text = multiWallet.walletWithID(transaction.walletID).name
                        tx_dest_wallet_badge.show()

                        toolbar_title.setText(R.string.received)
                    }
                    else -> toolbar_title.setText(R.string.transferred)
                }
            }
        }

        tv_toggle_details.setOnClickListener(this)
        tx_details_scroll.viewTreeObserver.addOnScrollChangedListener(this)
        populateInputOutput()
    }

    // returns first external output address if any
    private fun getDestinationAddress(): String? {
        for (output in transaction.outputs!!) {
            if (output.account == -1) {
                return output.address
            }
        }
        return null
    }

    private fun getSourceAccount(): String? {
        for (input in transaction.inputs!!) {
            if (input.accountNumber != null && input.accountNumber != -1) {
                return input.accountName
            }
        }

        return null
    }

    // returns first internal output address if any
    private fun getReceiveAccount(): String? {
        for (output in transaction.outputs!!) {
            if (output.account != -1) {
                return output.accountName
            }
        }
        return null
    }

    private fun populateInputOutput() {
        val inputs = ArrayList<DropDownItem>()
        for (input in transaction.inputs!!) {
            val amount = getString(R.string.tx_details_account, Utils.formatDecredWithComma(input.amount), input.accountName)
            var inputBadge = ""
            if (input.accountNumber != null && input.accountNumber != -1){
                inputBadge = multiWallet.walletWithID(transaction.walletID).name
            }
            inputs.add(DropDownItem(amount, input.previousOutpoint!!, inputBadge))
        }

        InputOutputDropdown(input_dropdown_layout, inputs.toTypedArray(), top_bar)

        val outputs = ArrayList<DropDownItem>()
        for (output in transaction.outputs!!) {
            val amount = getString(R.string.tx_details_account, Utils.formatDecredWithComma(output.amount), output.accountName)
            var outputBadge = ""
            if (output.account != -1){
                outputBadge = multiWallet.walletWithID(transaction.walletID).name
            }
            outputs.add(DropDownItem(amount, output.address!!, outputBadge))
        }

        InputOutputDropdown(output_dropdown_layout, outputs.toTypedArray(), isInput = false, toastAnchor = top_bar)
    }

    override fun onScrollChanged() {
        top_bar.elevation = if (tx_details_scroll.scrollY == 0) {
            0f
        } else {
            resources.getDimension(R.dimen.app_bar_elevation)
        }
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.tv_toggle_details -> {
                tx_extra_details.toggleVisibility()
                val strRes = if (tx_extra_details.visibility == View.VISIBLE) {
                    R.string.hide_details
                } else {
                    R.string.show_details
                }

                tv_toggle_details.setText(strRes)
            }
            R.id.tx_details_id -> {
                Utils.copyToClipboard(top_bar, tx_details_id.text.toString(), R.string.tx_id_copied)
            }
            R.id.tx_details_dest -> {
                Utils.copyToClipboard(top_bar, tx_details_dest.text.toString(), R.string.address_copy_text)
            }
            R.id.iv_info -> {
                val content = HtmlCompat.fromHtml(getString(R.string.tx_details_copy_info), 0)
                InfoDialog(context!!)
                        .setDialogTitle(getString(R.string.how_to_copy))
                        .setMessage(content)
                        .setPositiveButton(getString(R.string.got_it), null)
                        .show()
            }
            R.id.view_dcrdata -> {
                val url = if (BuildConfig.IS_TESTNET) {
                    "https://testnet.dcrdata.org/tx/" + transaction.hash
                } else {
                    "https://explorer.dcrdata.org/tx/" + transaction.hash
                }

                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(browserIntent)
            }
        }
    }

}