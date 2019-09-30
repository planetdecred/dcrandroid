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
import com.dcrandroid.activities.security.SignMessage
import com.dcrandroid.activities.security.ValidateAddress
import com.dcrandroid.data.Constants
import com.dcrandroid.dialog.RenameAccountDialog
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.openedWalletsList
import com.dcrandroid.extensions.show
import com.dcrandroid.extensions.totalWalletBalance
import com.dcrandroid.util.SnackBar
import com.dcrandroid.util.Utils
import com.dcrandroid.util.WalletData
import dcrlibwallet.LibWallet
import kotlinx.android.synthetic.main.wallet_row.view.*

class WalletsAdapter(val context: Context, val backupSeedClick:(walletID: Long) -> Unit): RecyclerView.Adapter<WalletsAdapter.WalletsViewHolder>() {

    private var wallets: ArrayList<LibWallet>
    private val multiWallet = WalletData.multiWallet
    private var expanded = -1

    init {
        wallets = multiWallet!!.openedWalletsList()
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

        holder.walletName.text = wallet.walletName
        holder.totalBalance.text = context.getString(R.string.dcr_amount,
                Utils.formatDecredWithComma(wallet.totalWalletBalance(context)))

        if(wallet.walletSeed.isNullOrBlank()){
            holder.backupNeeded.hide()
            holder.backupWarning.hide()
        }else{
            holder.backupNeeded.show()
            holder.backupWarning.show()

            holder.backupWarning.setOnClickListener {
                backupSeedClick(wallet.walletID)
            }
        }

        if(expanded == position){
            val adapter = AccountsAdapter(context, wallet.walletID)

            holder.accountsList.layoutManager = LinearLayoutManager(context)
            holder.accountsList.isNestedScrollingEnabled = false
            holder.accountsList.adapter = adapter

            holder.accountsLayout.show()

            holder.expand.setImageResource(R.drawable.ic_collapse02)
            holder.container.setBackgroundResource(R.drawable.curved_top_ripple)
        }else{
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

            if(currentlyExpanded != null){
                notifyItemChanged(currentlyExpanded)
            }
            notifyItemChanged(position)
        }

        // popup menu
        holder.more.setOnClickListener {

            val items = arrayOf(
                    PopupItem(R.string.rename_wallet),
                    PopupItem(R.string.change_spending_pass),
                    PopupItem(R.string.view_property),
                    PopupItem(R.string.validate_addresses),
                    PopupItem(R.string.sign_message),
                    PopupItem(R.string.remove_wallet, R.color.orangeTextColor)
            )

            PopupUtil.showPopup(it, items){window, index ->
               window.dismiss()
                when(index){
                    0 -> { // rename wallet
                        val activity = context as AppCompatActivity
                        RenameAccountDialog(wallet.walletName, true){newName ->

                            try{
                                multiWallet!!.renameWallet(wallet.walletID, newName)
                            }catch (e: Exception){
                                return@RenameAccountDialog e
                            }
                            notifyItemChanged(position)
                            SnackBar.showText(context, R.string.wallet_renamed)

                            return@RenameAccountDialog null
                        }.show(activity.supportFragmentManager, null)
                    }
                    3 -> {
                        val intent = Intent(context, ValidateAddress::class.java)
                        intent.putExtra(Constants.WALLET_ID, wallet.walletID)
                        context.startActivity(intent)
                    }
                    4 -> {
                        val intent = Intent(context, SignMessage::class.java)
                        intent.putExtra(Constants.WALLET_ID, wallet.walletID)
                        context.startActivity(intent)
                    }
                    5 -> {
                        println("Deleting Wallet")
                        if(!multiWallet!!.isSyncing && !multiWallet.isSynced){
                            multiWallet.deleteWallet(wallet.walletID, "".toByteArray())
                        }else{
                            SnackBar.showError(context, R.string.cancel_sync_create_wallet)
                        }

                    }
                }
            }
        }
    }

    fun addWallet(walletID: Long){
        val wallet = multiWallet!!.getWallet(walletID)
        wallets.add(wallet)
        notifyItemInserted(wallets.size - 1)
    }

    fun walletBackupVerified(walletID: Long){
        wallets.forEachIndexed { index, wallet ->
            if(wallet.walletID == walletID){
                wallets[index] = multiWallet!!.getWallet(walletID)
                notifyItemChanged(index)
                return
            }
        }
    }

    inner class WalletsViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val walletName =  itemView.wallet_name
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