package com.dcrandroid.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dcrandroid.adapter.AccountAdapter;
import com.dcrandroid.data.Constants;
import com.dcrandroid.data.Account;
import com.dcrandroid.MainActivity;
import com.dcrandroid.R;
import com.dcrandroid.util.DcrConstants;
import com.dcrandroid.util.PreferenceUtil;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;


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
        DividerItemDecoration itemDecoration = new DividerItemDecoration(getContext(), LinearLayoutManager.VERTICAL);
        itemDecoration.setDrawable(ContextCompat.getDrawable(getContext(), R.drawable.gray_divider));
        recyclerView.addItemDecoration(itemDecoration);
        recyclerView.setAdapter(accountAdapter);
        registerForContextMenu(recyclerView);
        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200 && resultCode == RESULT_OK){
            String accountName = data.getStringExtra(Constants.ACCOUNT_NAME);
            int accountNumber = data.getIntExtra(Constants.ACCOUNT_NUMBER, -1);
            for (int i = 0; i < accountList.size(); i++){
                if (accountList.get(i).getAccountNumber() == accountNumber){
                    accountList.get(i).setAccountName(accountName);
                    accountAdapter.notifyItemChanged(i);
                    return;
                }
            }
        }
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