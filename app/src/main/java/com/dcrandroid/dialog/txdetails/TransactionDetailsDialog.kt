/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.dialog.txdetails

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.HtmlCompat
import com.dcrandroid.BuildConfig
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.data.Transaction
import com.dcrandroid.dialog.FullScreenBottomSheetDialog
import com.dcrandroid.dialog.InfoDialog
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.show
import com.dcrandroid.extensions.toggleVisibility
import com.dcrandroid.util.CoinFormat
import com.dcrandroid.util.SnackBar
import com.dcrandroid.util.TimeUtils
import com.dcrandroid.util.Utils
import dcrlibwallet.Dcrlibwallet
import kotlinx.android.synthetic.main.transaction_details.*
import kotlinx.android.synthetic.main.transaction_row.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class TransactionDetailsDialog(val transaction: Transaction) : FullScreenBottomSheetDialog(null),
    View.OnClickListener, ViewTreeObserver.OnScrollChangedListener {

    private val wallet = multiWallet.walletWithID(transaction.walletID)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.transaction_details, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        tx_details_icon.setImageResource(transaction.iconResource)

        if (transaction.type == Dcrlibwallet.TxTypeMixed) {
            val amountDcrFormat = CoinFormat.format(transaction.mixDenomination)

            val amountBuilder = SpannableStringBuilder(amountDcrFormat)
            if (transaction.mixCount > 1) {
                var mixCount = SpannableString("\t x${transaction.mixCount}") as Spannable
                mixCount = CoinFormat.applyColor(
                    mixCount,
                    requireContext().resources.getColor(R.color.text3)
                )
                amountBuilder.append(mixCount)
            }

            tx_details_amount.text = amountBuilder
        } else {
            val txAmount =
                if (transaction.direction == Dcrlibwallet.TxDirectionSent && transaction.type == Dcrlibwallet.TxTypeRegular) {
                    -transaction.amount
                } else {
                    transaction.amount
                }
            tx_details_amount.text = CoinFormat.format(txAmount, 0.625f)
        }

        val sdf = SimpleDateFormat(getString(R.string.date_time_format), Locale.getDefault())

        val symbols = DateFormatSymbols(Locale.getDefault())
        symbols.amPmStrings = arrayOf(getString(R.string.am), getString(R.string.pm))
        sdf.dateFormatSymbols = symbols
        val timestamp = sdf.format(transaction.timestampMillis)
        tx_timestamp.text = timestamp

        setConfirmationStatus()

        rebroadcast_button.setOnClickListener(this)
        view_dcrdata.setOnClickListener(this)
        iv_info.setOnClickListener(this)
        tx_details_id.setOnClickListener(this)
        tx_details_id.text = transaction.hash
        tx_details_type.text = transaction.type
        tx_details_fee.text = getString(R.string.x_dcr, CoinFormat.formatDecred(transaction.fee))

        when (transaction.type) {
            Dcrlibwallet.TxTypeRegular -> {
                when (transaction.direction) {
                    Dcrlibwallet.TxDirectionSent -> {
                        tx_source_row.show()
                        tx_details_source.text = getSourceAccount()

                        tx_source_wallet_badge.text = wallet.name
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
                        tx_details_dest.setTextColor(resources.getColor(R.color.text1))

                        tx_dest_wallet_badge.text = wallet.name
                        tx_dest_wallet_badge.show()

                        toolbar_title.setText(R.string.received)
                    }
                    else -> toolbar_title.setText(R.string.transferred)
                }
            }
            else -> {
                val title = when (transaction.type) {
                    Dcrlibwallet.TxTypeTicketPurchase -> R.string.ticket_purchase
                    Dcrlibwallet.TxTypeVote -> R.string.vote
                    Dcrlibwallet.TxTypeRevocation -> R.string.revoked
                    Dcrlibwallet.TxTypeMixed -> R.string.mixed
                    else -> R.string.tx_sort_coinbase
                }

                toolbar_title.setText(title)
            }
        }

        if (transaction.matchesFilter(Dcrlibwallet.TxFilterStaking)) {
            setupTicketDetails()
        }

        view_ticket_spent.setOnClickListener {
            if (transaction.ticketSpentHash != "") {
                val transaction = wallet.getTransaction(transaction.ticketSpentHash)
                TransactionDetailsDialog(
                    Transaction.from(transaction)
                ).show(requireContext())
            }
        }

        tv_toggle_details.setOnClickListener(this)
        tx_details_scroll.viewTreeObserver.addOnScrollChangedListener(this)
        populateInputOutput()
    }

    override fun onTxOrBalanceUpdateRequired(walletID: Long?) {
        super.onTxOrBalanceUpdateRequired(walletID)
        GlobalScope.launch(Dispatchers.Main) {
            setupTicketDetails()
            setConfirmationStatus()
        }
    }

    private fun setupTicketDetails() {
        var progressDrawable =
            ResourcesCompat.getDrawable(resources, R.drawable.green_progress_bar, null)
        var progressBarLabel = R.string.spendable_in
        var progressBarMax = 0
        var progressBarProgress = 0

        var status: String? = null
        var voteReward: String? = null
        var daysToVote: String? = null

        if (transaction.type == Dcrlibwallet.TxTypeRevocation) {
            view_ticket_spent_row.show()

            if (transaction.confirmations <= BuildConfig.TicketMaturity) {
                ticket_details.show()

                progressDrawable =
                    ResourcesCompat.getDrawable(resources, R.drawable.green_progress_bar, null)
                progressBarMax = BuildConfig.TicketMaturity
                progressBarProgress = transaction.confirmations

            }
        } else if (transaction.type == Dcrlibwallet.TxTypeVote) {
            ticket_details.show()
            view_ticket_spent_row.show()

            daysToVote = getString(R.string.x_days, transaction.daysToVoteOrRevoke)
            voteReward = getString(R.string.x_dcr, CoinFormat.formatDecred(transaction.voteReward))

            progressDrawable =
                ResourcesCompat.getDrawable(resources, R.drawable.green_progress_bar2, null)
            progressBarMax = BuildConfig.TicketMaturity
            progressBarProgress = transaction.confirmations

        } else if (transaction.type == Dcrlibwallet.TxTypeTicketPurchase) {
            var ticketSpender: dcrlibwallet.Transaction? = null
            if (wallet.ticketHasVotedOrRevoked(transaction.hash)) {
                ticketSpender = wallet.ticketSpender(transaction.hash)
            }

            ticket_details.show()
            status_row.show()

            status = if (ticketSpender != null) {
                if (ticketSpender.type == Dcrlibwallet.TxTypeVote) {
                    getString(R.string.voted)
                } else {
                    getString(R.string.revoked)
                }
            } else if (transaction.matchesFilter(Dcrlibwallet.TxFilterLive)) {
                getString(R.string.live)
            } else if (transaction.matchesFilter(Dcrlibwallet.TxFilterImmature)) {
                getString(R.string.immature)
            } else if (transaction.matchesFilter(Dcrlibwallet.TxFilterUnmined)) {
                getString(R.string.unmined)
            } else {
                getString(R.string.missed_expired)
            }

            if (ticketSpender != null && ticketSpender.type == Dcrlibwallet.TxTypeVote) {
                daysToVote = getString(R.string.x_days, ticketSpender.daysToVoteOrRevoke)
                voteReward =
                    getString(R.string.x_dcr, CoinFormat.formatDecred(ticketSpender.voteReward))
            }

            progressBarLabel = R.string.maturity
            progressDrawable =
                ResourcesCompat.getDrawable(resources, R.drawable.blue_progress_bar2, null)
            progressBarMax = BuildConfig.TicketMaturity
            progressBarProgress = transaction.confirmations

            if (transaction.matchesFilter(Dcrlibwallet.TxFilterLive) && ticketSpender == null) {
                progressBarLabel = R.string.age

                progressDrawable =
                    ResourcesCompat.getDrawable(resources, R.drawable.blue_progress_bar, null)
                val liveBlocks = transaction.confirmations - BuildConfig.TicketMaturity
                progressBarProgress = liveBlocks
                progressBarMax = BuildConfig.TicketExpiry
            }
        }

        if (status != null) {
            status_row.show()
            tv_status.text = status
        }

        if (daysToVote != null) {
            days_to_vote_row.show()
            tv_days_to_vote.text = daysToVote
        }

        if (voteReward != null) {
            vote_reward_row.show()
            tv_vote_reward.text = voteReward
        }

        if (progressBarProgress <= progressBarMax && progressBarMax != 0) {
            maturity_progress_row.show()

            maturity_progress_label.text = getString(progressBarLabel)

            maturity_progress_bar.progressDrawable = progressDrawable
            maturity_progress_bar.max = progressBarMax
            maturity_progress_bar.progress = progressBarProgress

            maturity_progress_prefix.text = String.format(
                "%d%%",
                ((progressBarProgress / progressBarMax.toFloat()) * 100).toInt()
            )

            val remainingTime =
                (progressBarMax - progressBarProgress) * BuildConfig.TargetTimePerBlock
            maturity_progress_suffix.text =
                TimeUtils.calculateTime(remainingTime.toLong(), requireContext())
        }
    }

    private fun setConfirmationStatus() {
        val spendUnconfirmedFunds = multiWallet.readBoolConfigValueForKey(
            Dcrlibwallet.SpendUnconfirmedConfigKey,
            Constants.DEF_SPEND_UNCONFIRMED
        )

        status_icon.setImageResource(transaction.getConfirmationIconRes(spendUnconfirmedFunds))
        tv_confirmations.setTextColor(requireContext().getColor(R.color.blueGraySecondTextColor))

        if (transaction.confirmations >= Dcrlibwallet.DefaultRequiredConfirmations || spendUnconfirmedFunds) {
            tv_confirmations.text = HtmlCompat.fromHtml(
                getString(
                    R.string.tx_details_confirmations,
                    transaction.confirmations
                ), 0
            )

            tx_block_row.show()
            tx_block.text = transaction.height.toString()
        } else if (transaction.confirmations == 1) {
            tv_confirmations.text = HtmlCompat.fromHtml(
                getString(
                    R.string.tx_details_pending_confirmation,
                    transaction.confirmations
                ), 0
            )

            tx_block_row.show()
            tx_block.text = transaction.height.toString()
        } else {
            tv_confirmations.apply {
                setText(R.string.pending)
                setTextColor(context.getColor(R.color.lightGrayTextColor))
            }
        }

        if (transaction.height == -1) {
            rebroadcast_button.show()
        } else {
            rebroadcast_button.hide()
        }
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
            if (input.accountNumber != -1) {
                return wallet.accountName(input.accountNumber)
            }
        }

        return null
    }

    // returns first internal output address if any
    private fun getReceiveAccount(): String? {
        for (output in transaction.outputs!!) {
            if (output.account != -1) {
                return wallet.accountName(output.account)
            }
        }
        return null
    }

    private fun populateInputOutput() {
        val inputs = ArrayList<DropDownItem>()
        for (input in transaction.inputs!!) {
            val accountName = if (input.accountNumber >= 0) {
                wallet.accountName(input.accountNumber)
            } else getString(R.string.external).lowercase(Locale.getDefault())
            val amount = getString(
                R.string.tx_details_account,
                CoinFormat.formatDecred(input.amount),
                accountName
            )
            var inputBadge = ""
            if (input.accountNumber != -1) {
                inputBadge = wallet.name
            }
            inputs.add(DropDownItem(amount, input.previousOutpoint!!, inputBadge))
        }

        InputOutputDropdown(input_dropdown_layout, inputs.toTypedArray(), top_bar)

        val outputs = ArrayList<DropDownItem>()
        for (output in transaction.outputs!!) {
            val accountName = if (output.account >= 0) {
                wallet.accountName(output.account)
            } else getString(R.string.external).lowercase(Locale.getDefault())
            val amount = getString(
                R.string.tx_details_account,
                CoinFormat.formatDecred(output.amount),
                accountName
            )
            var outputBadge = ""
            if (output.account != -1) {
                outputBadge = wallet.name
            }
            outputs.add(DropDownItem(amount, output.address!!, outputBadge))
        }

        InputOutputDropdown(
            output_dropdown_layout,
            outputs.toTypedArray(),
            isInput = false,
            toastAnchor = top_bar
        )
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
                Utils.copyToClipboard(
                    top_bar,
                    tx_details_dest.text.toString(),
                    R.string.address_copy_text
                )
            }
            R.id.iv_info -> {
                val content = HtmlCompat.fromHtml(getString(R.string.tx_details_copy_info), 0)
                InfoDialog(requireContext())
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
            R.id.rebroadcast_button -> {

                if (!multiWallet.isConnectedToDecredNetwork) {
                    SnackBar.showError(requireContext(), R.string.not_connected)
                    return
                }

                try {
                    wallet.publishUnminedTransactions()
                    SnackBar.showText(requireContext(), R.string.rebroadcast_tx_success)
                } catch (e: Exception) {
                    e.printStackTrace()
                    val op = "rebroadcast tx"
                    Utils.showErrorDialog(requireContext(), op + ": " + e.message)
                    Dcrlibwallet.logT(op, e.message)
                }
            }
        }
    }

}