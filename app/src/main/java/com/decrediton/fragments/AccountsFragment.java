package com.decrediton.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.decrediton.Activities.AccountDetailsActivity;
import com.decrediton.Adapter.AccountAdapter;
import com.decrediton.Data.Account;
import com.decrediton.MainActivity;
import com.decrediton.R;
import com.decrediton.Util.RecyclerTouchListener;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Macsleven on 28/11/2017.
 */

public class AccountsFragment extends Fragment {

    private List<Account> accountList = new ArrayList<>();
    private AccountAdapter accountAdapter;
    private LayoutInflater layoutInflater;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //returning our layout file
        //change R.layout.yourlayoutfilename for each of your fragments
        View rootView = inflater.inflate(R.layout.content_account, container, false);
        this.layoutInflater = LayoutInflater.from(rootView.getContext());
        RecyclerView recyclerView = rootView.getRootView().findViewById(R.id.recycler_view2);
        accountAdapter = new AccountAdapter(accountList,layoutInflater);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(rootView.getContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), LinearLayoutManager.VERTICAL));
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Account account = accountList.get(position);

                Toast.makeText(getContext()," this is working",Toast.LENGTH_LONG).show();

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
        prepareAccountData();
        registerForContextMenu(recyclerView);
        return rootView;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //you can set the title for your toolbar here for different fragments different titles
        getActivity().setTitle("Account");
    }
    private void prepareAccountData(){
        Account account = new Account("default"," 89.04838473"+" DCR","89.04838473"+"DCR","0.0 DCR","0.0 DCR","0.0 DCR","0.0 DCR","0","m / 44/","20 internal, 20 external");
        accountList.add(account);
        account = new Account("Import"," 89.04838473"+" DCR","89.04838473"+" DCR","0.0 DCR","0.0 DCR","0.0 DCR","0.0 DCR","0","m / 44/","20 internal, 20 external");
        accountList.add(account);

    }
}
