package com.dcrandroid.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import com.dcrandroid.util.DcrConstants;
import com.dcrandroid.util.RecyclerTouchListener;
import com.dcrandroid.util.TransactionSorter;
import com.dcrandroid.util.TransactionsResponse;
import com.dcrandroid.util.TransactionsResponse.TransactionItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mobilewallet.GetTransactionsResponse;

/**
 * Created by Macsleven on 28/11/2017.
 */

public class HistoryFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, GetTransactionsResponse {
    private List<TransactionItem> transactionList = new ArrayList<>();
    private TransactionAdapter transactionAdapter;
    private  TextView refresh;
    private SwipeRefreshLayout swipeRefreshLayout;
    RecyclerView recyclerView;
    private DcrConstants constants;

    private boolean needsUpdate = false,  isForeground;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        constants = DcrConstants.getInstance();
        View rootView = inflater.inflate(R.layout.content_history, container, false);
        LayoutInflater layoutInflater = LayoutInflater.from(rootView.getContext());
        swipeRefreshLayout = rootView.getRootView().findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary,
                R.color.colorPrimary,
                R.color.colorPrimary,
                R.color.colorPrimary);
        swipeRefreshLayout.setOnRefreshListener(this);
        refresh = rootView.getRootView().findViewById(R.id.no_history);
        transactionAdapter = new TransactionAdapter(transactionList, layoutInflater);
        recyclerView = rootView.getRootView().findViewById(R.id.history_recycler_view);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(rootView.getContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), LinearLayoutManager.VERTICAL));
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                TransactionItem history = transactionList.get(position);
                Intent i = new Intent(getContext(), TransactionDetailsActivity.class);
                Bundle extras = new Bundle();
                extras.putLong(Constants.AMOUNT, history.getAmount());
                extras.putLong(Constants.FEE, history.getFee());
                extras.putLong(Constants.TIMESTAMP, history.getTimestamp());
                extras.putInt(Constants.HEIGHT, history.getHeight());
                extras.putLong(Constants.TOTAL_INPUT, history.totalInput);
                extras.putLong(Constants.TOTAL_OUTPUT, history.totalOutputs);
                extras.putString(Constants.TYPE, history.type);
                extras.putString(Constants.HASH, history.hash);
                extras.putInt(Constants.DIRECTION, history.getDirection());
                extras.putSerializable(Constants.INPUTS, history.inputs);
                extras.putSerializable(Constants.OUTPUTS, history.outputs);
                i.putExtras(extras);
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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //you can set the title for your toolbar here for different fragments different titles
        if (getActivity() != null)
        getActivity().setTitle(getString(R.string.history));
    }

    @Override
    public void onResume() {
        super.onResume();
        isForeground = true;
        if(needsUpdate){
            needsUpdate = false;
            prepareHistoryData();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        isForeground = false;
    }

    public void prepareHistoryData(){
        if(!isForeground){
            needsUpdate = true;
            return;
        }
        swipeRefreshLayout.setRefreshing(true);
        loadTransactions();
        new Thread(){
            public void run(){
                try {
                    constants.wallet.getTransactions(HistoryFragment.this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void saveTransactions(){
        try {
            File path = new File(getContext().getFilesDir()+"/savedata/");
            path.mkdirs();
            File file = new File(getContext().getFilesDir()+"/savedata/history_transactions");
            file.createNewFile();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(file));
            objectOutputStream.writeObject(transactionList);
            objectOutputStream.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void loadTransactions(){
        try {
            File path = new File(getContext().getFilesDir()+"/savedata/");
            path.mkdirs();
            File file = new File(getContext().getFilesDir()+"/savedata/history_transactions");
            if(file.exists()){
                ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(file));
                List<TransactionItem> temp = (List<TransactionItem>) objectInputStream.readObject();
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
        new Thread(){
            public void run(){
                try {
                    constants.wallet.getTransactions(HistoryFragment.this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    @Override
    public void onResult(final String s) {
        if(getActivity() == null){
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TransactionsResponse response = TransactionsResponse.parse(s);
                if(response.transactions.size() == 0){
                    if(!refresh.isShown()){
                        refresh.setVisibility(View.VISIBLE);
                    }
                    recyclerView.setVisibility(View.GONE);
                    if(swipeRefreshLayout.isRefreshing()){
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }else{
                    ArrayList<TransactionItem> transactions = response.transactions;
                    Collections.sort(transactions, new TransactionSorter());
                    transactionList.clear();
                    transactionList.addAll(0, transactions);
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
            }
        });
    }
}