package com.dcrandroid.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dcrandroid.activities.AccountDetailsActivity;
import com.dcrandroid.adapter.AccountAdapter;
import com.dcrandroid.data.Balance;
import com.dcrandroid.data.Constants;
import com.dcrandroid.data.Account;
import com.dcrandroid.MainActivity;
import com.dcrandroid.R;
import com.dcrandroid.util.DcrConstants;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.RecyclerTouchListener;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Macsleven on 28/11/2017.
 */

public class AccountsFragment extends Fragment {

    private List<Account> accountList = new ArrayList<>();
    AccountAdapter accountAdapter;
    private PreferenceUtil util;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.content_account, container, false);
        LayoutInflater layoutInflater = LayoutInflater.from(rootView.getContext());
        MainActivity.menuOpen.setVisible(true);
        RecyclerView recyclerView = rootView.getRootView().findViewById(R.id.recycler_view2);
        accountAdapter = new AccountAdapter(accountList, layoutInflater, getContext());
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(rootView.getContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), LinearLayoutManager.VERTICAL));
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Account account = accountList.get(position);
                Balance balance = account.getBalance();
                Intent i = new Intent(getContext(), AccountDetailsActivity.class);
                i.putExtra(Constants.ACCOUNT_NAME, account.getAccountName());
                i.putExtra(Constants.EXTRA_ACCOUNT_NUMBER, account.getAccountNumber());
                i.putExtra(Constants.EXTRA_BALANCE_SPENDABLE, balance.getSpendable());
                i.putExtra(Constants.EXTRA_BALANCE_IMMATURE_REWARDS, balance.getImmatureReward());
                i.putExtra(Constants.EXTRA_HD_PATH, account.getHDPath());
                i.putExtra(Constants.KEYS, account.getInternalKeyCount() + " Internal, " + account.getExternalKeyCount() + " External, " + account.getImportedKeyCount() + " Imported");
                i.putExtra(Constants.EXTRA_BALANCE_TOTAL, balance.getTotal());
                i.putExtra(Constants.EXTRA_BALANCE_IMMATURE_STAKE_GEN, balance.getImmatureStakeGeneration());
                i.putExtra(Constants.EXTRA_BALANCE_VOTING_AUTHORITY, balance.getVotingAuthority());
                i.putExtra(Constants.EXTRA_BALANCE_LOCKED_BY_TICKETS, balance.getLockedByTickets());
                startActivity(i);
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));
        recyclerView.setAdapter(accountAdapter);
        registerForContextMenu(recyclerView);
        return rootView;
    }

    public void prepareAccountData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    accountList.clear();
                    accountList.addAll(Account.parse(DcrConstants.getInstance().wallet.getAccounts(util.getBoolean(Constants.SPEND_UNCONFIRMED_FUNDS) ? 0 : Constants.REQUIRED_CONFIRMATIONS)));
                    if (getActivity() == null) {
                        return;
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            accountAdapter.notifyDataSetChanged();
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //you can set the title for your toolbar here for different fragments different titles
        if (getActivity() == null) {
            return;
        }
        getActivity().setTitle(getString(R.string.account));
        util = new PreferenceUtil(getActivity());
        prepareAccountData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MainActivity.menuOpen.setVisible(false);
    }
}