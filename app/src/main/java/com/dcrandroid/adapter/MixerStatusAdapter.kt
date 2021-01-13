/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import com.dcrandroid.extensions.openedWalletsList
import com.dcrandroid.util.CoinFormat
import com.dcrandroid.util.WalletData
import dcrlibwallet.Dcrlibwallet
import dcrlibwallet.Wallet
import kotlinx.android.synthetic.main.mixer_status_row.view.*

class MixerStatusAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val multiWallet = WalletData.multiWallet!!
    val wallets = multiWallet.openedWalletsList()
    val mixingWallets: List<Wallet>
        get() {
            return wallets.filter { it.isAccountMixerActive }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(
                LayoutInflater.from(parent.context)
                        .inflate(R.layout.mixer_status_row, parent, false))
    }

    override fun getItemCount() = mixingWallets.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        holder.itemView.mixer_status_wallet_name.text = mixingWallets[position].name

        val unmixedAccountNumber = mixingWallets[position].readInt32ConfigValueForKey(Dcrlibwallet.AccountMixerUnmixedAccount, -1)
        holder.itemView.mixer_status_unmixed_balance.text = CoinFormat.formatAlpha(mixingWallets[position].getAccountBalance(unmixedAccountNumber).total)
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v)
}