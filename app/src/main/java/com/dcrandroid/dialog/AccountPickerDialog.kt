/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.annotation.StringRes
import androidx.recyclerview.widget.LinearLayoutManager
import com.dcrandroid.R
import com.dcrandroid.adapter.AccountPickerAdapter
import com.dcrandroid.data.Account
import com.dcrandroid.extensions.openedWalletsList
import com.dcrandroid.extensions.walletAccounts
import com.dcrandroid.util.WalletData
import kotlinx.android.synthetic.main.account_picker_sheet.*

class AccountPickerDialog(@StringRes val title: Int, private val currentAccount: Account) : FullScreenBottomSheetDialog(),
        ViewTreeObserver.OnScrollChangedListener {

    lateinit var filterAccount: (account: Account) -> Boolean
    lateinit var accountSelected: (account: Account) -> Unit?
    private var layoutManager: LinearLayoutManager? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.account_picker_sheet, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        account_picker_title.setText(title)

        val multiWallet = WalletData.multiWallet!!
        val wallets = multiWallet.openedWalletsList()

        val items = ArrayList<Any>()

        for (wallet in wallets) {
            items.add(wallet)
            val accounts = wallet.walletAccounts()
                    .dropLastWhile { it.accountNumber == Int.MAX_VALUE } // remove imported account
            items.addAll(accounts)
        }

        val adapter = AccountPickerAdapter(context!!, items.toTypedArray(), currentAccount)
        adapter.accountSelected = {
            dismiss()
            accountSelected(it)
        }
        adapter.filterAccount = filterAccount
        layoutManager = LinearLayoutManager(context)
        account_picker_rv.layoutManager = layoutManager
        account_picker_rv.adapter = adapter

        account_picker_rv.viewTreeObserver.addOnScrollChangedListener(this)

        go_back.setOnClickListener {
            dismiss()
        }
    }

    override fun onScrollChanged() {
        val firstVisibleItem = layoutManager!!.findFirstCompletelyVisibleItemPosition()
        app_bar.elevation = if (firstVisibleItem != 0) {
            resources.getDimension(R.dimen.app_bar_elevation)
        } else {
            0f
        }
    }
}