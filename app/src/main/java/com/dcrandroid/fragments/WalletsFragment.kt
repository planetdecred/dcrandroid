/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.fragments

import android.os.Bundle
import android.view.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import com.dcrandroid.adapter.WalletsAdapter
import com.dcrandroid.data.Account
import com.dcrandroid.util.WalletData
import com.dcrandroid.activities.SetupWalletActivity
import android.content.Intent
import android.widget.Toast
import com.dcrandroid.extensions.openedWalletsList
import dcrlibwallet.MultiWallet

class WalletsFragment: BaseFragment() {

    private lateinit var recyclerView: RecyclerView
    private val multiWallet: MultiWallet
    get() = WalletData.getInstance().multiWallet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.account_list_rv)
        setToolbarTitle(R.string.wallets, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val multiWallet = WalletData.getInstance().multiWallet
        val wallets = multiWallet.openedWalletsList()
        val adapter = WalletsAdapter(wallets)

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.isNestedScrollingEnabled = false
        recyclerView.adapter = adapter
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.accounts_page_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.add_new_wallet -> {
                if(multiWallet.isSyncing || multiWallet.isSynced){
                    Toast.makeText(context!!, "Cancel sync before creating wallet", Toast.LENGTH_SHORT).show()
                    return false
                }

                val i = Intent(context, SetupWalletActivity::class.java)
                startActivity(i)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}