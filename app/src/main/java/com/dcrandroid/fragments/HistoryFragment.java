package com.dcrandroid.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dcrandroid.activities.TransactionDetailsActivity;
import com.dcrandroid.adapter.TransactionAdapter;
import com.dcrandroid.R;
import com.dcrandroid.data.Constants;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.RecyclerTouchListener;
import com.dcrandroid.util.TransactionsResponse;
import com.dcrandroid.data.Transaction;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import dcrwallet.Dcrwallet;

/**
 * Created by Macsleven on 28/11/2017.
 */

public class HistoryFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener{
    private List<Transaction> transactionList = new ArrayList<>();
    private TransactionAdapter transactionAdapter;
    private  TextView refresh;
    private SwipeRefreshLayout swipeRefreshLayout;
    RecyclerView recyclerView;
//    private View progressContainer;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.content_history, container, false);
        LayoutInflater layoutInflater = LayoutInflater.from(rootView.getContext());
        swipeRefreshLayout = rootView.getRootView().findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary,
                R.color.colorPrimary,
                R.color.colorPrimary,
                R.color.colorPrimary);
        swipeRefreshLayout.setOnRefreshListener(this);
//        progressContainer = rootView.findViewById(R.id.progressContainers);
        refresh = rootView.getRootView().findViewById(R.id.no_history);
        transactionAdapter = new TransactionAdapter(transactionList, layoutInflater);
        recyclerView = rootView.getRootView().findViewById(R.id.history_recycler_view);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(rootView.getContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration( getContext(), LinearLayoutManager.VERTICAL));
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Transaction history = transactionList.get(position);
                Intent i = new Intent(getContext(), TransactionDetailsActivity.class);
                i.putExtra(Constants.EXTRA_AMOUNT,history.getAmount());
                i.putExtra(Constants.EXTRA_TRANSACTION_FEE,history.getTransactionFee());
                i.putExtra(Constants.EXTRA_TRANSACTION_DATE,history.getTxDate());
                System.out.println("TxType: "+history.getType());
                i.putExtra("Height", history.getHeight());
                i.putExtra(Constants.EXTRA_TRANSACTION_TYPE,history.getType());
                i.putExtra(Constants.EXTRA_TRANSACTION_STATUS,history.getTxStatus());
                i.putExtra(Constants.EXTRA_TRANSACTION_HASH, history.getHash());
                i.putStringArrayListExtra(Constants.EXTRA_INPUT_USED,history.getUsedInput());
                i.putStringArrayListExtra(Constants.EXTRA_NEW_WALLET_OUTPUT,history.getWalletOutput());
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
        getActivity().setTitle(getString(R.string.history));
    }

    private void prepareHistoryData(){
        swipeRefreshLayout.setRefreshing(true);
        loadTransactions();
        transactionList.clear();
        new Thread(){
            public void run(){
                PreferenceUtil util = new PreferenceUtil(HistoryFragment.this.getContext());
                int blockHeight = util.getInt(PreferenceUtil.BLOCK_HEIGHT);
                int startHeight = Integer.parseInt(util.get(PreferenceUtil.TRANSACTION_HEIGHT,"1"));
                String result = Dcrwallet.getTransactions(blockHeight, 0);
                TransactionsResponse response = TransactionsResponse.parse(result);
                if(response.errorOccurred){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(!refresh.isShown()){
                                refresh.setVisibility(View.VISIBLE);
                            }
                            if(swipeRefreshLayout.isRefreshing()){
                                swipeRefreshLayout.setRefreshing(false);
                            }
                        }
                    });
                }
                else if(response.transactions.size() == 0){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(!refresh.isShown()){
                                refresh.setVisibility(View.VISIBLE);
                            }
                            recyclerView.setVisibility(View.GONE);
                            if(swipeRefreshLayout.isRefreshing()){
                                swipeRefreshLayout.setRefreshing(false);
                            }
                        }
                    });
                }
                else {
                    util.set(PreferenceUtil.TRANSACTION_HEIGHT, String.valueOf(blockHeight));
                    final List<Transaction> temp = new ArrayList<>();
                    for (int i = 0; i < response.transactions.size(); i++) {
                        Transaction transaction = new Transaction();
                        TransactionsResponse.TransactionItem item = response.transactions.get(i);
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(item.timestamp * 1000);
                        SimpleDateFormat sdf = new SimpleDateFormat(" dd yyyy, hh:mma",Locale.getDefault());
                        transaction.setTxDate(calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT,Locale.getDefault()) + sdf.format(calendar.getTime()).toLowerCase());
                        transaction.setTransactionFee(item.fee);
                        transaction.setType(item.type);
                        transaction.setHash(item.hash);
                        transaction.setHeight(item.height);
                        transaction.setAmount(item.amount);
                        transaction.setTxStatus(item.status);
                        ArrayList<String> usedInput = new ArrayList<>();
                        for (int j = 0; j < item.debits.size(); j++) {
                            usedInput.add(item.debits.get(j).accountName + "\n" + String.format(Locale.getDefault(), "%f", item.debits.get(j).previous_amount));
                        }
                        ArrayList<String> output = new ArrayList<>();
                        for (int j = 0; j < item.credits.size(); j++) {
                            output.add(item.credits.get(j).address + "\n" + String.format(Locale.getDefault(), "%f", item.credits.get(j).amount));
                        }
                        transaction.setUsedInput(usedInput);
                        transaction.setWalletOutput(output);
                        temp.add(transaction);
                    }
                    if(getActivity() == null){
                        return;
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Collections.reverse(temp);
                            transactionList.clear();
                            transactionList.addAll(0,temp);
                            if(refresh.isShown()){
                                refresh.setVisibility(View.INVISIBLE);
                            }
                            recyclerView.setVisibility(View.VISIBLE);
                            if(swipeRefreshLayout.isRefreshing()){
                                swipeRefreshLayout.setRefreshing(false);
                            }
                             transactionAdapter.notifyDataSetChanged();
                            saveTransactions();
                        }
                    });
                }
            }
        }.start();
    }

    public void saveTransactions(){
        try {
            File path = new File(getContext().getFilesDir()+"/savedata/");
            path.mkdirs();
            File file = new File(getContext().getFilesDir()+"/savedata/transactions");
            file.createNewFile();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(file));
            objectOutputStream.writeObject(transactionList);
            objectOutputStream.close();
            System.out.println("Transaction Written");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void loadTransactions(){
        try {
            File path = new File(getContext().getFilesDir()+"/savedata/");
            path.mkdirs();
            File file = new File(getContext().getFilesDir()+"/savedata/transactions");
            if(file.exists()){
                ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(file));
                List<Transaction> temp = (List<Transaction>) objectInputStream.readObject();
                transactionList.addAll(temp);
                transactionAdapter.notifyDataSetChanged();
                System.out.println("Done: "+transactionList.size());
                if(transactionList.size() == 0){
                    if(refresh.isShown()){
                        refresh.setVisibility(View.INVISIBLE);
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onRefresh() {
        prepareHistoryData();
    }
}