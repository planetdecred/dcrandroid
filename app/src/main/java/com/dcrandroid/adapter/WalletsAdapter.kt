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
import com.dcrandroid.activities.AccountMixerActivity
import com.dcrandroid.activities.VerifySeedInstruction
import com.dcrandroid.activities.WalletSettings
import com.dcrandroid.activities.security.SignMessage
import com.dcrandroid.data.Account
import com.dcrandroid.data.Constants
import com.dcrandroid.dialog.AccountDetailsDialog
import com.dcrandroid.dialog.RequestNameDialog
import com.dcrandroid.extensions.*
import com.dcrandroid.fragments.VERIFY_SEED_REQUEST_CODE
import com.dcrandroid.fragments.WALLET_SETTINGS_REQUEST_CODE
import com.dcrandroid.util.CoinFormat
import com.dcrandroid.util.SnackBar
import com.dcrandroid.util.WalletData
import dcrlibwallet.Wallet
import kotlinx.android.synthetic.main.wallet_row.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

// Item Types
const val ITEM_TYPE_WALLET = 0
const val ITEM_TYPE_WATCH_ONLY_WALLET_HEADER = 1
const val ITEM_TYPE_WATCH_ONLY_WALLET = 2

class WalletsAdapter(val context: Context, val launchIntent: (intent: Intent, requestCode: Int) -> Unit) : RecyclerView.Adapter<WalletsAdapter.WalletsViewHolder>() {

    private var items: ArrayList<Any> = ArrayList()
    private val multiWallet = WalletData.multiWallet!!
    private var expanded = -1

    init {
        reloadList()
    }

    fun reloadList() {
        items.clear()
        items.addAll(multiWallet.fullCoinWalletsList())
        val watchOnlyWallets = multiWallet.watchOnlyWalletsList()
        if (watchOnlyWallets.isNotEmpty()) {
            items.add(WatchOnlyWalletHeader())
            items.addAll(watchOnlyWallets)
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalletsViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        val view = if (viewType == ITEM_TYPE_WATCH_ONLY_WALLET_HEADER) {
            inflater.inflate(R.layout.watch_only_wallet_list_header, parent, false)
        } else {
            inflater.inflate(R.layout.wallet_row, parent, false)
        }

        return WalletsViewHolder(view)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemViewType(position: Int): Int {
        val item = items[position]
        if (item is Wallet) {
            if (item.isWatchingOnlyWallet) {
                return ITEM_TYPE_WATCH_ONLY_WALLET
            }

            return ITEM_TYPE_WALLET
        }

        return ITEM_TYPE_WATCH_ONLY_WALLET_HEADER
    }

    override fun onBindViewHolder(holder: WalletsViewHolder, position: Int) {
        if (getItemViewType(position) != ITEM_TYPE_WATCH_ONLY_WALLET_HEADER) {
            val wallet = items[position] as Wallet
            setupWalletRow(wallet, holder, position)
        }
    }

    private fun setupWalletRow(wallet: Wallet, holder: WalletsViewHolder, position: Int) {
        holder.walletName.text = wallet.name
        holder.totalBalance.text = context.getString(R.string.dcr_amount,
                CoinFormat.formatDecred(wallet.totalWalletBalance()))

        if (wallet.encryptedSeed == null) {
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

        val layoutParams = holder.itemView.layoutParams as RecyclerView.LayoutParams
        layoutParams.bottomMargin = context.resources.getDimensionPixelOffset(R.dimen.margin_padding_size_4)
        layoutParams.topMargin = context.resources.getDimensionPixelOffset(R.dimen.margin_padding_size_4)

        val containerBackground: Int // this is a transparent ripple
        val viewBackground: Int
        var walletIcon = R.drawable.ic_wallet

        if (expanded == position) { // this should never hit for watching only wallets
            val adapter = AccountsAdapter(context, wallet.id)

            holder.accountsList.layoutManager = LinearLayoutManager(context)
            holder.accountsList.isNestedScrollingEnabled = false
            holder.accountsList.adapter = adapter

            holder.accountsLayout.show()

            holder.expand.show()
            holder.expand.setImageResource(R.drawable.ic_collapse02)

            containerBackground = R.drawable.curved_top_ripple
            viewBackground = R.drawable.card_bg
        } else {
            holder.accountsList.adapter = null
            holder.accountsLayout.hide()

            if (wallet.isWatchingOnlyWallet) {

                walletIcon = R.drawable.ic_watch_only_wallet
                layoutParams.topMargin = 0

                holder.expand.hide()
                if (position == itemCount - 1) {
                    viewBackground = R.drawable.card_bg_footer
                    containerBackground = R.drawable.curved_bottom_ripple
                } else {
                    layoutParams.bottomMargin = 0
                    // ripple won't work, the background is not clickable. Just the white bg is needed
                    viewBackground = R.drawable.bg_white_ripple

                    containerBackground = R.drawable.ripple
                }
            } else {
                holder.expand.show()
                holder.expand.setImageResource(R.drawable.ic_expand02)

                viewBackground = R.drawable.card_bg
                containerBackground = R.drawable.ripple_bg_white_corners_8dp
            }
        }

        holder.walletIcon.setImageResource(walletIcon)
        holder.itemView.setBackgroundResource(viewBackground)
        holder.container.setBackgroundResource(containerBackground)

        holder.container.setOnClickListener {
            if (wallet.isWatchingOnlyWallet) {
                val account = Account.from(wallet.getAccount(0))
                AccountDetailsDialog(context, wallet.id, account) {
                    null
                }.show(context)
            } else {
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

        }

        // popup menu
        holder.more.setOnClickListener {

            val dividerWidth = context.resources.getDimensionPixelSize(R.dimen.wallets_menu_width)

            val items = arrayOf(
                    PopupItem(R.string.sign_message, R.color.darkBlueTextColor, !wallet.isWatchingOnlyWallet),
                    PopupDivider(dividerWidth),
                    PopupItem(R.string.privacy, R.color.darkBlueTextColor, !wallet.isWatchingOnlyWallet),
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
                    2 -> {
                        val intent = Intent(context, AccountMixerActivity::class.java)
                        intent.putExtra(Constants.WALLET_ID, wallet.id)
                        context.startActivity(intent)
                    }
                    4 -> { // rename wallet
                        val activity = context as AppCompatActivity
                        RequestNameDialog(R.string.rename_wallet_sheet_title, wallet.name, true) { newName ->

                            try {
                                multiWallet.renameWallet(wallet.id, newName)
                            } catch (e: Exception) {
                                return@RequestNameDialog e
                            }
                            notifyItemChanged(position)
                            SnackBar.showText(context, R.string.wallet_renamed)

                            return@RequestNameDialog null
                        }.show(activity.supportFragmentManager, null)
                    }
                    5 -> {
                        val intent = Intent(context, WalletSettings::class.java)
                        intent.putExtra(Constants.WALLET_ID, wallet.id)
                        launchIntent(intent, WALLET_SETTINGS_REQUEST_CODE)
                    }
                }
            }
        }
    }

    fun addWallet(walletID: Long) = GlobalScope.launch(Dispatchers.Main) {
        val wallet = multiWallet.walletWithID(walletID) ?: return@launch

        var watchOnlyWalletHeaderPosition = -1
        for (i in 0 until itemCount) {
            if (items[i] is WatchOnlyWalletHeader) {
                watchOnlyWalletHeaderPosition = i
                break
            }
        }

        if (watchOnlyWalletHeaderPosition == -1 && wallet.isWatchingOnlyWallet) {
            items.add(WatchOnlyWalletHeader())
            notifyItemInserted(items.size - 1)
        }

        when {
            !wallet.isWatchingOnlyWallet && watchOnlyWalletHeaderPosition != -1 -> {
                items.add(watchOnlyWalletHeaderPosition, wallet)
                notifyItemInserted(watchOnlyWalletHeaderPosition)
            }
            else -> {
                items.add(wallet)
                notifyItemInserted(items.size - 1)
                if (wallet.isWatchingOnlyWallet) {
                    // notify previous last item to remove shadow
                    notifyItemChanged(items.size - 2)
                }
            }
        }

    }

    fun updateWalletRow(walletID: Long) {
        items.forEachIndexed { index, wallet ->
            if (wallet is Wallet && wallet.id == walletID) {
                items[index] = multiWallet.walletWithID(walletID)
                notifyItemChanged(index)
                return
            }
        }
    }

    fun walletBackupVerified(walletID: Long) = updateWalletRow(walletID)

    inner class WatchOnlyWalletHeader

    inner class WalletsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val walletIcon = itemView.wallet_icon
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