package com.dcrandroid.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import com.dcrandroid.MainActivity;
import com.dcrandroid.activities.TransactionDetailsActivity;
import com.dcrandroid.adapter.TransactionAdapter;
import com.dcrandroid.R;

import com.dcrandroid.data.Account;
import com.dcrandroid.data.Constants;
import com.dcrandroid.util.DcrConstants;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.RecyclerTouchListener;
import com.dcrandroid.util.TransactionSorter;
import com.dcrandroid.util.TransactionsResponse;
import com.dcrandroid.util.Utils;
import com.dcrandroid.view.CurrencyTextView;

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

public class OverviewFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, GetTransactionsResponse {

    private List<TransactionsResponse.TransactionItem> transactionList = new ArrayList<>();
    private CurrencyTextView tvBalance;
    private SwipeRefreshLayout swipeRefreshLayout;

    TransactionAdapter transactionAdapter;
    TextView refresh;
    PreferenceUtil util;
    RecyclerView recyclerView;
    DcrConstants constants;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if(getContext() == null){
            return null;
        }
        util = new PreferenceUtil(getContext());
        constants = DcrConstants.getInstance();
        View rootView = inflater.inflate(R.layout.content_overview, container, false);
        LayoutInflater layoutInflater = LayoutInflater.from(rootView.getContext());
        swipeRefreshLayout = rootView.getRootView().findViewById(R.id.swipe_refresh_layout2);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary,
                R.color.colorPrimary,
                R.color.colorPrimary,
                R.color.colorPrimary);
        swipeRefreshLayout.setOnRefreshListener(this);
        recyclerView = rootView.getRootView().findViewById(R.id.history_recycler_view2);
        refresh = rootView.getRootView().findViewById(R.id.no_history);
        transactionAdapter = new TransactionAdapter(transactionList, layoutInflater);
        tvBalance = rootView.getRootView().findViewById(R.id.overview_av_balance);
        tvBalance.formatAndSetText(Utils.formatDecred(0) + " DCR");
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(rootView.getContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration( getContext(), LinearLayoutManager.VERTICAL));
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                TransactionsResponse.TransactionItem history = transactionList.get(position);
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
                extras.putSerializable(Constants.Inputs, history.inputs);
                extras.putSerializable(Constants.OUTPUTS, history.outputs);
                i.putExtras(extras);
                startActivity(i);
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));
        TextView showHistory= rootView.findViewById(R.id.show_history);
        showHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() != null && getActivity() instanceof MainActivity){
                    MainActivity mainActivity = (MainActivity) getActivity();
                    mainActivity.displayHistory();
                }
            }
        });

        recyclerView.setAdapter(transactionAdapter);
        registerForContextMenu(recyclerView);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //you can set the title for your toolbar here for different fragments different titles
        if (getActivity() != null){
            getActivity().setTitle(getString(R.string.overview));
        }
        getBalance();
        prepareHistoryData();
    }

    private void getBalance(){
        new Thread(){
            public void run(){
                try {
                    if(getContext() == null){
                        return;
                    }
                    final ArrayList<Account> accounts = Account.parse(constants.wallet.getAccounts(util.getBoolean(Constants.SPEND_UNCONFIRMED_FUNDS) ? 0 : Constants.REQUIRED_CONFIRMATIONS));
                    long totalBalance = 0;
                    for(int i = 0; i < accounts.size(); i++){
                        totalBalance += accounts.get(i).getBalance().getTotal();
                    }
                    final long finalTotalBalance = totalBalance;
                    if(getActivity() == null){
                        return;
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvBalance.formatAndSetText(Utils.formatDecred(finalTotalBalance) + " DCR");
                        }
                    });
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void prepareHistoryData(){
        swipeRefreshLayout.setRefreshing(true);
        transactionList.clear();
        loadTransactions();
        new Thread(){
            public void run(){
                try {
                    constants.wallet.getTransactions(OverviewFragment.this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void saveTransactions(ArrayList<TransactionsResponse.TransactionItem> transactions){
        try {
            File path = new File(getContext().getFilesDir()+"/savedata/");
            path.mkdirs();
            File file = new File(getContext().getFilesDir()+"/savedata/transactions");
            file.createNewFile();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(file));
            objectOutputStream.writeObject(transactions);
            objectOutputStream.close();
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
                ArrayList<TransactionsResponse.TransactionItem> temp = (ArrayList<TransactionsResponse.TransactionItem>) objectInputStream.readObject();
                if(temp.size() > 0) {
                    if (temp.size() > 7) {
                        transactionList.addAll(temp.subList(0, 7));
                    } else {
                        transactionList.addAll(temp);
                    }
                }
                transactionAdapter.notifyDataSetChanged();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onRefresh() {
        prepareHistoryData();
    }

    @Override
    public void onResult(final String json) {
        if(getActivity() == null){
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TransactionsResponse response = TransactionsResponse.parse(json);
                if(response.transactions.size() == 0){
                    refresh.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    if(swipeRefreshLayout.isRefreshing()){
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }else{
                    ArrayList<TransactionsResponse.TransactionItem> transactions = response.transactions;
                    Collections.sort(transactions, new TransactionSorter());
                    transactionList.clear();
                    if (transactions.size() > 0) {
                        if (transactions.size() > 7) {
                            transactionList.addAll(transactions.subList(0, 7));
                        }else{
                            transactionList.addAll(transactions);
                        }
                    }
                    if(refresh.isShown()){
                        refresh.setVisibility(View.INVISIBLE);
                    }
                    recyclerView.setVisibility(View.VISIBLE);
                    if(swipeRefreshLayout.isRefreshing()){
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    transactionAdapter.notifyDataSetChanged();
                    if(refresh.isShown()){
                        refresh.setVisibility(View.GONE);
                    }
                    saveTransactions(transactions);
                }
            }
        });
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction() == null){
                return;
            }
            if(intent.getAction().equals(Constants.BLOCK_SCAN_COMPLETE)){
                prepareHistoryData();
            }else if(intent.getAction().equals(Constants.NEW_TRANSACTION)){
                TransactionsResponse.TransactionItem transaction = new TransactionsResponse.TransactionItem();
                Bundle b = intent.getExtras();
                if (b == null){
                    return;
                }
                transaction.timestamp = b.getLong(Constants.TIMESTAMP, 0);
                transaction.fee = b.getLong(Constants.FEE, 0);
                transaction.type = b.getString(Constants.TYPE);
                transaction.hash = b.getString(Constants.HASH);
                transaction.height = b.getInt(Constants.HEIGHT, 0);
                transaction.amount = b.getLong(Constants.AMOUNT, 0);
                transaction.direction = b.getInt(Constants.DIRECTION, -1);
                transaction.inputs = (ArrayList<TransactionsResponse.TransactionInput>) b.getSerializable(Constants.Inputs);
                transaction.outputs = (ArrayList<TransactionsResponse.TransactionOutput>) b.getSerializable(Constants.OUTPUTS);
                transaction.totalInput = b.getLong(Constants.TOTAL_INPUT, 0);
                transaction.totalOutputs = b.getLong(Constants.TOTAL_OUTPUT, 0);
                transaction.animate = true;
                transactionList.add(0, transaction);
                transactionAdapter.notifyDataSetChanged();
                if(transactionList.size() > 7){
                    transactionList.remove(transactionList.size() - 1);
                }
            }else if(intent.getAction().equals(Constants.TRANSACTION_CONFIRMED)){
                String hash = intent.getStringExtra(Constants.HASH);
                for(int i = 0; i < transactionList.size(); i++){
                    if(transactionList.get(i).hash.equals(hash)){
                        TransactionsResponse.TransactionItem transaction = transactionList.get(i);
                        transaction.height = intent.getIntExtra(Constants.HEIGHT, -1);
                        transactionList.set(i, transaction);
                        transactionAdapter.notifyDataSetChanged();
                        break;
                    }
                }
            }
            getBalance();
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        System.out.println("Overview OnPause");
        if(getActivity() != null){
            getActivity().unregisterReceiver(receiver);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        System.out.println("Overview OnResume");
        if(getActivity() != null) {
            IntentFilter filter = new IntentFilter(Constants.BLOCK_SCAN_COMPLETE);
            filter.addAction(Constants.NEW_TRANSACTION);
            filter.addAction(Constants.TRANSACTION_CONFIRMED);
            getActivity().registerReceiver(receiver, filter);
        }
    }
}
