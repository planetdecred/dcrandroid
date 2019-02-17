/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.dcrandroid.R;
import com.dcrandroid.activities.AddAccountActivity;
import com.dcrandroid.adapter.AccountAdapter;
import com.dcrandroid.data.Account;
import com.dcrandroid.data.Constants;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.WalletData;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import dcrlibwallet.LibWallet;

import static android.app.Activity.RESULT_OK;

/**
 * Created by Macsleven on 28/11/2017.
 */

public class AccountsFragment extends Fragment {

    private List<Account> accounts = new ArrayList<>();
    private AccountAdapter accountAdapter;

    private PreferenceUtil util;

    private LibWallet wallet;
    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals(Constants.SYNCED)) {
                prepareAccountData();
            }
        }
    };
    private RecyclerView recyclerView;
    private int EDIT_ACCOUNT_REQUEST_CODE = 200, CREATE_ACCOUNT_REQUEST_CODE = 1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View vi = inflater.inflate(R.layout.content_account, container, false);

        recyclerView = vi.findViewById(R.id.recycler_view2);

        return vi;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_ACCOUNT_REQUEST_CODE && resultCode == RESULT_OK) {
            String accountName = data.getStringExtra(Constants.ACCOUNT_NAME);
            int accountNumber = data.getIntExtra(Constants.ACCOUNT_NUMBER, -1);
            for (int i = 0; i < accounts.size(); i++) {
                if (accounts.get(i).getAccountNumber() == accountNumber) {
                    accounts.get(i).setAccountName(accountName);
                    accountAdapter.notifyItemChanged(i);
                    return;
                }
            }
        } else if (requestCode == CREATE_ACCOUNT_REQUEST_CODE && resultCode == RESULT_OK) {
            prepareAccountData();
        }
    }

    private void prepareAccountData() {
        try {
            accounts.clear();
            accounts.addAll(Account.parse(wallet.getAccounts(util.getBoolean(Constants.SPEND_UNCONFIRMED_FUNDS) ? 0 : Constants.REQUIRED_CONFIRMATIONS)));
            if (getActivity() == null) {
                return;
            }
            accountAdapter.notifyDataSetChanged();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getActivity() == null || getContext() == null) {
            return;
        }
        getActivity().setTitle(getString(R.string.accounts));
        util = new PreferenceUtil(getActivity());

        accountAdapter = new AccountAdapter(accounts, getActivity());
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        DividerItemDecoration itemDecoration = new DividerItemDecoration(getContext(), LinearLayoutManager.VERTICAL);
        itemDecoration.setDrawable(ContextCompat.getDrawable(getContext(), R.drawable.gray_divider));
        recyclerView.addItemDecoration(itemDecoration);
        recyclerView.setAdapter(accountAdapter);
        registerForContextMenu(recyclerView);

        wallet = WalletData.getInstance().wallet;

        prepareAccountData();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.accounts_page_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_account:
                Intent intent = new Intent(getContext(), AddAccountActivity.class);
                startActivityForResult(intent, CREATE_ACCOUNT_REQUEST_CODE);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getContext() != null) {
            getContext().unregisterReceiver(receiver);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getContext() != null) {
            IntentFilter filter = new IntentFilter(Constants.SYNCED);
            getContext().registerReceiver(receiver, filter);
        }
    }
}