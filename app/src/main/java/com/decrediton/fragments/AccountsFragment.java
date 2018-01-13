package com.decrediton.fragments;

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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.decrediton.Activities.AccountDetailsActivity;
import com.decrediton.Adapter.AccountAdapter;
import com.decrediton.Util.AccountResponse;
import com.decrediton.data.Account;
import com.decrediton.MainActivity;
import com.decrediton.R;
import com.decrediton.Util.RecyclerTouchListener;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dcrwallet.Dcrwallet;


/**
 * Created by Macsleven on 28/11/2017.
 */

public class AccountsFragment extends Fragment {

    private List<Account> accountList = new ArrayList<>();
    AccountAdapter accountAdapter;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.content_account, container, false);
        LayoutInflater layoutInflater = LayoutInflater.from(rootView.getContext());
        MainActivity.menuOpen.setVisible(true);
        RecyclerView recyclerView = rootView.getRootView().findViewById(R.id.recycler_view2);
        accountAdapter = new AccountAdapter(accountList, layoutInflater);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(rootView.getContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration( getContext(), LinearLayoutManager.VERTICAL));
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Account account = accountList.get(position);
                Intent i = new Intent(getContext(), AccountDetailsActivity.class);
                i.putExtra("AccountName",account.getAccountName());
                i.putExtra("AccountNumber",account.getAccountNumber());
                i.putExtra("Spendable",account.getSpendable());
                i.putExtra("ImmatureReward",account.getImmatureRewards());
                i.putExtra("HDPath",account.gethDPath());
                i.putExtra("Keys",account.getKeys());
                i.putExtra("total",account.getTotal());
                i.putExtra("ImmatureStakeGen",account.getImmatureStakeGeneration());
                i.putExtra("VotingAuthority",account.getVotingAuthority());
                i.putExtra("LockedByTickets",account.getLockedByTickets());
                startActivity(i);
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));
        recyclerView.setAdapter(accountAdapter);
        registerForContextMenu(recyclerView);
        prepareAccountData();
        return rootView;
    }

    public void prepareAccountData(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final AccountResponse response = AccountResponse.parse(Dcrwallet.getAccounts());
                    if(!response.errorOccurred) {
                        accountList.clear();
                        for (int i = 0; i < response.items.size(); i++) {
                            Account account = new Account();
                            AccountResponse.AccountItem item = response.items.get(i);
                            account.setAccountName(item.name);
                            account.setAccountNumber(String.valueOf(item.number));
                            account.setTotal(String.format(Locale.ENGLISH,"%f", item.balance.total));
                            account.setSpendable(String.valueOf(item.balance.spendable));
                            account.setImmatureRewards(String.valueOf(item.balance.immatureReward));
                            account.setImmatureStakeGeneration(String.valueOf(item.balance.immatureStakeGeneration));
                            account.setLockedByTickets(String.valueOf(item.balance.lockedByTickets));
                            account.setVotingAuthority(String.valueOf(item.balance.votingAuthority));
                            account.sethDPath("m / 44' / 20' / "+item.number);
                            account.setKeys(item.internalKeyCount+" Internal, "+item.externalKeyCount+" External, "+item.importedKeyCount+" Imported");
                            accountList.add(account);
                        }
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                accountAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //you can set the title for your toolbar here for different fragments different titles
        getActivity().setTitle("Account");
    }
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onDestroyView (){
        super.onDestroyView();
        MainActivity.menuOpen.setVisible(false);
    }
}
