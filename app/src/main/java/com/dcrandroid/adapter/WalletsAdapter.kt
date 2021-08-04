/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import com.dcrandroid.activities.WalletSettings
import com.dcrandroid.activities.privacy.AccountMixerActivity
import com.dcrandroid.activities.privacy.SetupPrivacy
import com.dcrandroid.activities.security.SignMessage
import com.dcrandroid.activities.verifyseed.VerifySeedInstruction
import com.dcrandroid.data.Account
import com.dcrandroid.data.Constants
import com.dcrandroid.dialog.AccountDetailsDialog
import com.dcrandroid.dialog.RequestNameDialog
import com.dcrandroid.extensions.*
import com.dcrandroid.fragments.PRIVACY_SETTINGS_REQUEST_CODE
import com.dcrandroid.fragments.VERIFY_SEED_REQUEST_CODE
import com.dcrandroid.fragments.WALLET_SETTINGS_REQUEST_CODE
import com.dcrandroid.util.CoinFormat
import com.dcrandroid.util.PopupMessage
import com.dcrandroid.util.SnackBar
import com.dcrandroid.util.WalletData
import dcrlibwallet.Dcrlibwallet
import dcrlibwallet.Wallet
import kotlinx.android.synthetic.main.wallet_row.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

// Item Types
const val ITEM_TYPE_WALLET = 0
const val ITEM_TYPE_WATCH_ONLY_WALLET_HEADER = 1
const val ITEM_TYPE_WATCH_ONLY_WALLET = 2

class WalletsAdapter(
    val fragment: Fragment,
    val launchIntent: (intent: Intent, requestCode: Int) -> Unit
) : RecyclerView.Adapter<WalletsAdapter.WalletsViewHolder>() {

    private var items: ArrayList<Any> = ArrayList()
    private val multiWallet = WalletData.multiWallet!!
    private var expanded = -1

    private var popupMessage: Toast? = null

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

    fun onPause() {
        popupMessage?.cancel()
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
        holder.totalBalance.text = fragment.context!!.getString(
            R.string.dcr_amount,
            CoinFormat.formatDecred(wallet.totalWalletBalance())
        )

        if (wallet.encryptedSeed == null) {
            holder.walletStatus.hide()
            holder.backupWarning.hide()
        } else {
            holder.walletStatus.show()
            holder.walletStatus.setText(R.string.not_backed_up)
            holder.walletStatus.setTextColor(fragment.context!!.getColor(R.color.colorError))
            holder.backupWarning.show()

            holder.backupWarning.setOnClickListener {
                val intent = Intent(fragment.context, VerifySeedInstruction::class.java)
                intent.putExtra(Constants.WALLET_ID, wallet.id)
                launchIntent(intent, VERIFY_SEED_REQUEST_CODE)
            }
        }

        if (wallet.isAccountMixerActive) {
            holder.walletStatus.show()
            holder.walletStatus.setText(R.string.mixing_elp)
            holder.walletStatus.setTextColor(fragment.context!!.getColor(R.color.blueGraySecondTextColor))
            holder.goToMixer.show()
        } else {
            holder.goToMixer.hide()
        }

        val layoutParams = holder.itemView.layoutParams as RecyclerView.LayoutParams
        layoutParams.bottomMargin =
            fragment.context!!.resources.getDimensionPixelOffset(R.dimen.margin_padding_size_4)
        layoutParams.topMargin =
            fragment.context!!.resources.getDimensionPixelOffset(R.dimen.margin_padding_size_4)

        val containerBackground: Int // this is a transparent ripple
        val viewBackground: Int
        var walletIcon = R.drawable.ic_wallet

        if (expanded == position) { // this should never hit for watching only wallets
            val adapter = AccountsAdapter(fragment.context!!, wallet.id)

            holder.accountsList.layoutManager = LinearLayoutManager(fragment.context!!)
            holder.accountsList.isNestedScrollingEnabled = false
            holder.accountsList.adapter = adapter

            holder.accountsLayout.show()

            holder.expand.show()
            holder.expand.setImageResource(R.drawable.ic_collapse02)

            containerBackground = R.drawable.ripple_bg_white_top_corner_14dp
            viewBackground = R.drawable.card_bg_14
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

                viewBackground = R.drawable.card_bg_14
                containerBackground = R.drawable.ripple_bg_white_corners_14dp
            }
        }

        holder.walletIcon.setImageResource(walletIcon)
        holder.itemView.setBackgroundResource(viewBackground)
        holder.container.setBackgroundResource(containerBackground)

        holder.container.setOnClickListener {
            if (wallet.isWatchingOnlyWallet) {
                val account = Account.from(wallet.getAccount(0))
                AccountDetailsDialog(fragment.context!!, wallet.id, account) {
                    null
                }.show(fragment.context!!)
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

        val showPrivacyPopup =
            !multiWallet.readBoolConfigValueForKey(Constants.SHOWN_PRIVACY_POPUP, false)
                    && !wallet.isWatchingOnlyWallet
        if (showPrivacyPopup) {
            var globalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null
            globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
                popupMessage = PopupMessage.showText(
                    holder.more,
                    R.string.privacy_popup_message,
                    Toast.LENGTH_SHORT
                )
                popupMessage?.show()

                holder.more.viewTreeObserver.removeOnGlobalLayoutListener(globalLayoutListener)

                multiWallet.setBoolConfigValueForKey(Constants.SHOWN_PRIVACY_POPUP, true)
            }

            holder.more.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
        }

        holder.goToMixer.setOnClickListener {
            val intent = Intent(fragment.context!!, AccountMixerActivity::class.java)
            intent.putExtra(Constants.WALLET_ID, wallet.id)
            fragment.startActivityForResult(intent, PRIVACY_SETTINGS_REQUEST_CODE)
        }

        // popup menu
        holder.more.setOnClickListener {

            popupMessage?.cancel()

            val dividerWidth =
                fragment.context!!.resources.getDimensionPixelSize(R.dimen.wallets_menu_width)
            val hasCheckedPrivacyPage =
                multiWallet.readBoolConfigValueForKey(Constants.CHECKED_PRIVACY_PAGE, false)

            val items = arrayOf(
                PopupItem(
                    R.string.sign_message,
                    R.color.textColor,
                    !wallet.isWatchingOnlyWallet
                ),
                PopupItem(
                    R.string.privacy,
                    R.color.textColor,
                    !wallet.isWatchingOnlyWallet,
                    !hasCheckedPrivacyPage && !wallet.isWatchingOnlyWallet
                ),
                PopupItem(R.string.rename),
                PopupItem(R.string.settings)
            )

            PopupUtil.showPopup(it, items as Array<Any>) { window, index ->
                window.dismiss()
                when (index) {
                    0 -> {
                        val intent = Intent(fragment.context!!, SignMessage::class.java)
                        intent.putExtra(Constants.WALLET_ID, wallet.id)
                        fragment.context!!.startActivity(intent)
                    }
                    1 -> {

                        val mixerConfigIsSet = wallet.readBoolConfigValueForKey(
                            Dcrlibwallet.AccountMixerConfigSet,
                            false
                        )

                        val intent = if (mixerConfigIsSet) {
                            Intent(fragment.context!!, AccountMixerActivity::class.java)
                        } else {
                            Intent(fragment.context!!, SetupPrivacy::class.java)
                        }
                        intent.putExtra(Constants.WALLET_ID, wallet.id)
                        fragment.startActivityForResult(intent, PRIVACY_SETTINGS_REQUEST_CODE)
                    }
                    2 -> { // rename wallet
                        RequestNameDialog(
                            R.string.rename_wallet_sheet_title,
                            wallet.name,
                            true
                        ) { newName ->

                            try {
                                multiWallet.renameWallet(wallet.id, newName)
                            } catch (e: Exception) {
                                return@RequestNameDialog e
                            }
                            notifyItemChanged(position)
                            SnackBar.showText(fragment.context!!, R.string.wallet_renamed)

                            return@RequestNameDialog null
                        }.show(fragment.activity!!.supportFragmentManager, null)
                    }
                    3 -> {
                        val intent = Intent(fragment.context!!, WalletSettings::class.java)
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
        val walletStatus = itemView.tv_wallet_status

        val more = itemView.iv_more
        val expand = itemView.expand_icon
        val backupWarning = itemView.backup_warning

        val goToMixer = itemView.go_to_mixer
        val container = itemView.container
        val accountsLayout = itemView.accounts

        val accountsList = itemView.account_list_rv

    }
}