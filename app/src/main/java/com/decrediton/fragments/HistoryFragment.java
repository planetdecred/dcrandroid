package com.decrediton.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.decrediton.Activities.TransactionDetailsActivity;
import com.decrediton.Adapter.TransactionAdapter;
import com.decrediton.R;
import com.decrediton.Util.AccountResponse;
import com.decrediton.Util.PreferenceUtil;
import com.decrediton.Util.RecyclerTouchListener;
import com.decrediton.Util.TransactionsResponse;
import com.decrediton.data.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dcrwallet.Balance;
import dcrwallet.Dcrwallet;

/**
 * Created by Macsleven on 28/11/2017.
 */

public class HistoryFragment extends Fragment{
        private List<Transaction> transactionList = new ArrayList<>();
        TransactionAdapter transactionAdapter;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.content_history, container, false);
        LayoutInflater layoutInflater = LayoutInflater.from(rootView.getContext());
        RecyclerView recyclerView = rootView.getRootView().findViewById(R.id.history_recycler_view);
        transactionAdapter = new TransactionAdapter(transactionList, layoutInflater);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(rootView.getContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration( getContext(), LinearLayoutManager.VERTICAL));
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Transaction history = transactionList.get(position);
                Intent i = new Intent(getContext(), TransactionDetailsActivity.class);
                i.putExtra("Amount",history.getAccountName());
                i.putExtra("Address",history.getAddress());
                i.putExtra("TxDate",history.getTxDate());
                i.putExtra("TxType",history.getTxType());
                i.putExtra("AccountName",history.getAccountName());
                i.putExtra("TxStatus",history.getTxStatus());
                startActivity(i);
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));
        recyclerView.setAdapter(transactionAdapter);
        registerForContextMenu(recyclerView);
        prepareHistoryData();
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //you can set the title for your toolbar here for different fragments different titles
        getActivity().setTitle("History");
    }

    private void prepareHistoryData(){
        new Thread(){
            public void run(){
                PreferenceUtil util = new PreferenceUtil(HistoryFragment.this.getContext());
                int blockHeight = Integer.parseInt(util.get(PreferenceUtil.BLOCK_HEIGHT));
                System.out.println("Block Height: "+blockHeight);
                String result = Dcrwallet.getTransactions(blockHeight);
                System.out.println(result);
                TransactionsResponse response = TransactionsResponse.parse(result);
                for(int i = 0; i < response.transactions.size(); i++){
                    Transaction transaction = new Transaction();
                    TransactionsResponse.TransactionItem item = response.transactions.get(i);
                    transaction.setAccountName("NULL");
                    transaction.setTxDate(String.valueOf(item.timestamp));
                    transaction.setAmount(String.format(Locale.getDefault(),"%f",item.fee/ AccountResponse.SATOSHI));
                    transaction.setTxType(item.type);
                    transaction.setTxStatus("confirmed");
                    transactionList.add(transaction);
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        transactionAdapter.notifyDataSetChanged();
                    }
                });
            }
        }.start();
    }
}
