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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.dcrandroid.activities.AccountDetailsActivity;
import com.dcrandroid.adapter.AccountAdapter;
import com.dcrandroid.data.Constants;
import com.dcrandroid.util.AccountResponse;
import com.dcrandroid.data.Account;
import com.dcrandroid.MainActivity;
import com.dcrandroid.R;
import com.dcrandroid.util.DcrConstants;
import com.dcrandroid.util.RecyclerTouchListener;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


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
        accountAdapter = new AccountAdapter(accountList, layoutInflater, getContext());
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(rootView.getContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration( getContext(), LinearLayoutManager.VERTICAL));
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Account account = accountList.get(position);
                Intent i = new Intent(getContext(), AccountDetailsActivity.class);
                i.putExtra(Constants.EXTRA_ACCOUNT_NAME,account.getAccountName());
                i.putExtra(Constants.EXTRA_ACCOUNT_NUMBER,account.getAccountNumber());
                i.putExtra(Constants.EXTRA_BALANCE_SPENDABLE,account.getSpendable());
                i.putExtra(Constants.EXTRA_BALANCE_IMMATURE_REWARDS,account.getImmatureRewards());
                i.putExtra(Constants.EXTRA_HD_PATH,account.getHDPath());
                i.putExtra(Constants.EXTRA_KEYS,account.getKeys());
                i.putExtra(Constants.EXTRA_BALANCE_TOTAL,account.getTotal());
                i.putExtra(Constants.EXTRA_BALANCE_IMMATURE_STAKE_GEN,account.getImmatureStakeGeneration());
                i.putExtra(Constants.EXTRA_BALANCE_VOTING_AUTHORITY,account.getVotingAuthority());
                i.putExtra(Constants.EXTRA_BALANCE_LOCKED_BY_TICKETS,account.getLockedByTickets());
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
                    final AccountResponse response = AccountResponse.parse(DcrConstants.getInstance().wallet.getAccounts());
                    if(!response.errorOccurred) {
                        accountList.clear();
                        for (int i = 0; i < response.items.size(); i++) {
                            Account account = new Account();
                            AccountResponse.AccountItem item = response.items.get(i);
                            account.setAccountName(item.name);
                            account.setAccountNumber(item.number);
                            account.setTotal(item.balance.total);
                            account.setSpendable(item.balance.spendable);
                            account.setImmatureRewards(item.balance.immatureReward);
                            account.setImmatureStakeGeneration(item.balance.immatureStakeGeneration);
                            account.setLockedByTickets(item.balance.lockedByTickets);
                            account.setVotingAuthority(item.balance.votingAuthority);
                            account.setHDPath("m / 44' / 11' / "+item.number);
//                            if(Dcrwallet.isTestNet()){
//                                account.setHDPath("m / 44' / 11' / "+item.number);
//                            }else{
//                                account.setHDPath("m / 44' / 20' / "+item.number);
//                            }
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
        getActivity().setTitle(getString(R.string.account));
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
