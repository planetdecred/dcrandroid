/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.SpinnerAdapter
import android.widget.TextView
import com.dcrandroid.R
import com.dcrandroid.data.Account
import com.dcrandroid.util.CoinFormat
import dcrlibwallet.Dcrlibwallet

class AccountSpinnerAdapter(val accounts: List<Account>, val inflater: LayoutInflater) : BaseAdapter(), SpinnerAdapter {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var vi = convertView
        if (vi == null) {
            vi = inflater.inflate(R.layout.spinner_list_item_1, parent, false)
        }

        val nameAndBal = CoinFormat.format(accounts[position].accountName + " [${Dcrlibwallet.amountCoin(accounts[position].balance.spendable)} dcr]")

        val text = vi!!.findViewById<TextView>(android.R.id.text1)
        text.text = nameAndBal

        return vi
    }

    override fun getItem(position: Int): Account {
        return accounts[position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getCount(): Int {
        return accounts.size
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = inflater.inflate(R.layout.dropdown_item_1, parent, false)

        val nameAndBal = CoinFormat.format(accounts[position].accountName + " [${Dcrlibwallet.amountCoin(accounts[position].balance.spendable)} dcr]")

        val textView = view.findViewById<TextView>(android.R.id.text1)
        textView.text = nameAndBal

        return view
    }
}