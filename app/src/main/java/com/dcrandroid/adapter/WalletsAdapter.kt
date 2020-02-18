/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import com.dcrandroid.activities.VerifySeedInstruction
import com.dcrandroid.activities.WalletSettings
import com.dcrandroid.activities.security.SignMessage
import com.dcrandroid.activities.security.VerifyMessage
import com.dcrandroid.data.Constants
import com.dcrandroid.dialog.RenameAccountDialog
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.openedWalletsList
import com.dcrandroid.extensions.show
import com.dcrandroid.extensions.totalWalletBalance
import com.dcrandroid.fragments.VERIFY_SEED_REQUEST_CODE
import com.dcrandroid.fragments.WALLET_SETTINGS_REQUEST_CODE
import com.dcrandroid.util.SnackBar
import com.dcrandroid.util.Utils
import com.dcrandroid.util.WalletData
import dcrlibwallet.Wallet
import kotlinx.android.synthetic.main.wallet_row.view.*

class WalletsAdapter(val context: Context, val launchIntent: (intent: Intent, requestCode: Int) -> Unit) : RecyclerView.Adapter<WalletsAdapter.WalletsViewHolder>() {

    private var wallets: ArrayList<Wallet>
    private val multiWallet = WalletData.multiWallet
    private var expanded = -1

    init {
        wallets = multiWallet!!.openedWalletsList()
    }

    fun reloadList() {
        wallets.clear()
        wallets.addAll(multiWallet!!.openedWalletsList())
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalletsViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        val view = inflater.inflate(R.layout.wallet_row, parent, false)
        return WalletsViewHolder(view)
    }

    override fun getItemCount(): Int {
        return wallets.size
    }

    override fun onBindViewHolder(holder: WalletsViewHolder, position: Int) {
        val wallet = wallets[position]

        holder.walletName.text = wallet.name
        holder.totalBalance.text = context.getString(R.string.dcr_amount,
                Utils.formatDecredWithComma(wallet.totalWalletBalance()))

        if (wallet.seed.isNullOrBlank()) {
            holder.backupNeeded.hide()
            holder.backupWarning.hide()
        } else {
            holder.backupNeeded.show()
            holder.backupWarning.show()

            holder.backupWarning.setOnClickListener {
                val intent = Intent(context, VerifySeedInstruction::class.java)
                intent.putExtra(Constants.WALLET_ID, wallet.id)
                launchIntent(intent, VERIFY_SEED_REQUEST_CODE)
            }
        }

        if (expanded == position) {
            val adapter = AccountsAdapter(context, wallet.id)

            holder.accountsList.layoutManager = LinearLayoutManager(context)
            holder.accountsList.isNestedScrollingEnabled = false
            holder.accountsList.adapter = adapter

            holder.accountsLayout.show()

            holder.expand.setImageResource(R.drawable.ic_collapse02)
            holder.container.setBackgroundResource(R.drawable.curved_top_ripple)
        } else {
            holder.accountsList.adapter = null
            holder.accountsLayout.hide()

            holder.expand.setImageResource(R.drawable.ic_expand02)
            holder.container.setBackgroundResource(R.drawable.ripple_bg_white_corners_8dp)
        }

        holder.container.setOnClickListener {
            var currentlyExpanded: Int? = null
            expanded = when (expanded) {
                position -> -1
                -1 -> position
                else -> { // an item is currently expanded
                    currentlyExpanded = expanded
                    position
                }
            }

            if (currentlyExpanded != null) {
                notifyItemChanged(currentlyExpanded)
            }
            notifyItemChanged(position)
        }

        // popup menu
        holder.more.setOnClickListener {

            val dividerWidth = context.resources.getDimensionPixelSize(R.dimen.wallets_menu_width)

            val items = arrayOf(
                    PopupItem(R.string.sign_message),
                    PopupItem(R.string.verify_message),
                    PopupDivider(dividerWidth),
                    PopupItem(R.string.view_property),
                    PopupDivider(dividerWidth),
                    PopupItem(R.string.rename),
                    PopupItem(R.string.settings)
            )

            PopupUtil.showPopup(it, items) { window, index ->
                window.dismiss()
                when (index) {
                    0 -> {
                        val intent = Intent(context, SignMessage::class.java)
                        intent.putExtra(Constants.WALLET_ID, wallet.id)
                        context.startActivity(intent)
                    }
                    1 -> {
                        val intent = Intent(context, VerifyMessage::class.java)
                        intent.putExtra(Constants.WALLET_ID, wallet.id)
                        context.startActivity(intent)
                    }
                    5 -> { // rename wallet
                        val activity = context as AppCompatActivity
                        RenameAccountDialog(wallet.name, true) { newName ->

                            try {
                                multiWallet!!.renameWallet(wallet.id, newName)
                            } catch (e: Exception) {
                                return@RenameAccountDialog e
                            }
                            notifyItemChanged(position)
                            SnackBar.showText(context, R.string.wallet_renamed)

                            return@RenameAccountDialog null
                        }.show(activity.supportFragmentManager, null)
                    }
                    6 -> {
                        val intent = Intent(context, WalletSettings::class.java)
                        intent.putExtra(Constants.WALLET_ID, wallet.id)
                        launchIntent(intent, WALLET_SETTINGS_REQUEST_CODE)
                    }
                }
            }
        }
    }

    fun addWallet(walletID: Long) {
        val wallet = multiWallet!!.walletWithID(walletID)
        wallets.add(wallet)
        notifyItemInserted(wallets.size - 1)
    }

    fun walletBackupVerified(walletID: Long) {
        wallets.forEachIndexed { index, wallet ->
            if (wallet.id == walletID) {
                wallets[index] = multiWallet!!.walletWithID(walletID)
                notifyItemChanged(index)
                return
            }
        }
    }

    inner class WalletsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val walletName = itemView.wallet_name
        val totalBalance = itemView.wallet_total_balance
        val backupNeeded = itemView.backup_needed

        val more = itemView.iv_more
        val expand = itemView.expand_icon
        val backupWarning = itemView.backup_warning

        val container = itemView.container
        val accountsLayout = itemView.accounts

        val accountsList = itemView.account_list_rv

    }
}