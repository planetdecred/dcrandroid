/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.dcrandroid.R
import com.dcrandroid.extensions.openedWalletsList
import kotlinx.android.synthetic.main.multi_wallet_transactions_page.*

class MultiWalletTransactions : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.multi_wallet_transactions_page, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setToolbarTitle(R.string.transactions, false)

        val adapter = TransactionsTabsAdapter(childFragmentManager)
        transactions_pager.adapter = adapter
        transactions_tab.setupWithViewPager(transactions_pager)
    }

    inner class TransactionsTabsAdapter(supportFragmentManager: FragmentManager) : FragmentStatePagerAdapter(supportFragmentManager,
            BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        private val wallets = multiWallet!!.openedWalletsList()
        override fun getCount(): Int {
            return wallets.size
        }

        override fun getItem(position: Int): Fragment {
            return TransactionsFragment()
                    .setWalletID(wallets[position].id)
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return wallets[position].name
        }

    }
}