/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.fragments

import android.app.Activity.RESULT_OK
import android.os.Bundle
import android.view.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import com.dcrandroid.adapter.WalletsAdapter
import com.dcrandroid.util.WalletData
import com.dcrandroid.activities.SetupWalletActivity
import android.content.Intent
import android.widget.Toast
import com.dcrandroid.activities.VerifySeedInstruction
import com.dcrandroid.data.Constants
import dcrlibwallet.MultiWallet

const val CREATE_WALLET_REQUEST_CODE = 100
const val VERIFY_SEED_REQUEST_CODE = 200

class WalletsFragment: BaseFragment() {

    private lateinit var adapter: WalletsAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_wallets, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.wallets_list_rv)
        setToolbarTitle(R.string.wallets, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        adapter = WalletsAdapter(context!!){walletID ->
            val intent = Intent(context, VerifySeedInstruction::class.java)
            intent.putExtra(Constants.WALLET_ID, walletID)
            startActivityForResult(intent, VERIFY_SEED_REQUEST_CODE)
        }

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.isNestedScrollingEnabled = false
        recyclerView.adapter = adapter
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == CREATE_WALLET_REQUEST_CODE && resultCode == RESULT_OK){
            val walletID = data!!.getLongExtra(Constants.WALLET_ID, -1)
            adapter.addWallet(walletID)
            refreshNavigationTabs()
        }else if(requestCode == VERIFY_SEED_REQUEST_CODE && resultCode == RESULT_OK){
            val walletID = data!!.getLongExtra(Constants.WALLET_ID, -1)
            adapter.walletBackupVerified(walletID)
            refreshNavigationTabs()
        }
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
                startActivityForResult(i, CREATE_WALLET_REQUEST_CODE)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}