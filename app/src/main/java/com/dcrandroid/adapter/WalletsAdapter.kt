/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.adapter

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import com.dcrandroid.dialog.RenameAccountDialog
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.openedWalletsList
import com.dcrandroid.extensions.show
import com.dcrandroid.extensions.totalWalletBalance
import com.dcrandroid.util.SnackBar
import com.dcrandroid.util.Utils
import com.dcrandroid.util.WalletData
import dcrlibwallet.LibWallet
import kotlinx.android.synthetic.main.popup_layout.view.*
import kotlinx.android.synthetic.main.wallet_row.view.*

class WalletsAdapter(val context: Context): RecyclerView.Adapter<WalletsAdapter.WalletsViewHolder>() {

    private var wallets: ArrayList<LibWallet>
    private val multiWallet = WalletData.getInstance().multiWallet
    private var expanded = -1

    init {
        wallets = multiWallet.openedWalletsList()
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
                Utils.formatDecred(wallet.totalWalletBalance(context)))

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
            holder.container.setBackgroundResource(R.drawable.wallet_row_background)
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
            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.popup_layout, null)
            val window = PopupWindow(view, LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT, true)

            val recyclerView = view.popup_rv
            val items = arrayOf(
                    PopupItem(R.string.rename_wallet),
                    PopupItem(R.string.change_spending_pass),
                    PopupItem(R.string.view_property),
                    PopupItem(R.string.remove_wallet, R.color.orangeTextColor)
            )

            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = PopupMenuAdapter(context, items) {index ->
                window.dismiss()
                when(index){
                    0 -> { // rename account
                        val activity = context as AppCompatActivity
                        RenameAccountDialog(wallet.walletName, true){newName ->

                            try{
                                multiWallet.renameWallet(wallet.walletID, newName)
                            }catch (e: Exception){
                                return@RenameAccountDialog e
                            }
                            notifyItemChanged(position)
                            SnackBar.showText(context, R.string.wallet_renamed)

                            return@RenameAccountDialog null
                        }.show(activity.supportFragmentManager, null)
                    }
                }
            }
            window.showAsDropDown(it)
        }
    }

    fun addWallet(walletID: Long){
        val wallet = multiWallet.getWallet(walletID)
        wallets.add(wallet)
        notifyItemInserted(wallets.size - 1)
    }

    inner class WalletsViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val walletName =  itemView.wallet_name
        val totalBalance = itemView.wallet_total_balance

        val more = itemView.iv_more
        val expand = itemView.expand_icon

        val container = itemView.container
        val accountsLayout = itemView.accounts

        val accountsList = itemView.account_list_rv

    }
}