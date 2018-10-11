package com.dcrandroid.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.dcrandroid.MainActivity;
import com.dcrandroid.R;
import com.dcrandroid.activities.TransactionDetailsActivity;
import com.dcrandroid.adapter.TransactionAdapter;
import com.dcrandroid.data.Account;
import com.dcrandroid.data.Constants;
import com.dcrandroid.util.CoinFormat;
import com.dcrandroid.util.DcrConstants;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.RecyclerTouchListener;
import com.dcrandroid.util.TransactionComparator;
import com.dcrandroid.util.TransactionsResponse;
import com.dcrandroid.util.Utils;

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

    public static final String OVERVIEW_FRAGMENT = "OverviewFragment";

    private List<TransactionsResponse.TransactionItem> transactionList = new ArrayList<>();

    private ImageView syncIndicator;
    private TextView tvBalance;
    private SwipeRefreshLayout swipeRefreshLayout;

    TransactionAdapter transactionAdapter;
    TextView refresh;
    PreferenceUtil util;
    RecyclerView recyclerView;
    DcrConstants constants;

    private int recyclerViewHeight, latestTransactionHeight;

    private boolean needsUpdate = false, isForeground;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getContext() == null) {
            return null;
        }
        util = new PreferenceUtil(getContext());
        constants = DcrConstants.getInstance();
        View rootView = inflater.inflate(R.layout.content_overview, container, false);
        swipeRefreshLayout = rootView.getRootView().findViewById(R.id.swipe_refresh_layout2);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimaryDarkBlue,
                R.color.colorPrimaryDarkBlue,
                R.color.colorPrimaryDarkBlue,
                R.color.colorPrimaryDarkBlue);
        swipeRefreshLayout.setOnRefreshListener(this);
        recyclerView = rootView.getRootView().findViewById(R.id.history_recycler_view2);
        refresh = rootView.getRootView().findViewById(R.id.no_history);
        transactionAdapter = new TransactionAdapter(transactionList, getContext());
        tvBalance = rootView.getRootView().findViewById(R.id.overview_av_balance);
        syncIndicator = rootView.getRootView().findViewById(R.id.iv_sync_indicator);
        syncIndicator.setBackgroundResource(R.drawable.sync_animation);

        if(!constants.synced) {
            syncIndicator.post(new Runnable() {
                @Override
                public void run() {
                    AnimationDrawable syncAnimation = (AnimationDrawable) syncIndicator.getBackground();
                    syncAnimation.start();
                }
            });
        }else{
            getBalance();
            syncIndicator.setVisibility(View.GONE);
            tvBalance.setVisibility(View.VISIBLE);
        }

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(rootView.getContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), LinearLayoutManager.VERTICAL));
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                if (transactionList.size() <= position || position < 0) {
                    return;
                }
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
                extras.putString(Constants.RAW, history.raw);
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
        TextView showHistory = rootView.findViewById(R.id.show_history);
        showHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() != null && getActivity() instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) getActivity();
                    mainActivity.displayHistory();
                }
            }
        });

        ViewTreeObserver vto = recyclerView.getViewTreeObserver();
        if (vto.isAlive()) {
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    recyclerViewHeight = recyclerView.getHeight();
                    if (recyclerViewHeight != 0)
                        prepareHistoryData();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    } else {
                        recyclerView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    }
                }
            });
        }

        recyclerView.setAdapter(transactionAdapter);
        registerForContextMenu(recyclerView);

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //you can set the title for your toolbar here for different fragments different titles
        if (getActivity() != null) {
            getActivity().setTitle(getString(R.string.overview));
        }
    }


    private int getMaxDisplayItems() {
        if (getActivity() == null) {
            return 0;
        }
        int px = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 52, getActivity().getResources().getDisplayMetrics()));
        return recyclerViewHeight / px;
    }

    private void getBalance(){
        if(!constants.synced){
            return;
        }

        new Thread(){
            public void run(){
                try {
                    if (getContext() == null) {
                        return;
                    }
                    final List<Account> accounts = Account.parse(constants.wallet.getAccounts(util.getBoolean(Constants.SPEND_UNCONFIRMED_FUNDS) ? 0 : Constants.REQUIRED_CONFIRMATIONS));
                    long totalBalance = 0;
                    for (int i = 0; i < accounts.size(); i++) {
                        if (util.getBoolean(Constants.HIDE_WALLET + accounts.get(i).getAccountNumber())) {
                            continue;
                        }
                        totalBalance += accounts.get(i).getBalance().getTotal();
                    }
                    final long finalTotalBalance = totalBalance;
                    if (getActivity() == null) {
                        return;
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvBalance.setText(CoinFormat.Companion.format(Utils.formatDecredWithComma(finalTotalBalance) + " DCR"));
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void prepareHistoryData() {
        if (!isForeground) {
            needsUpdate = true;
            return;
        }
        if (getActivity() == null) {
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(true);
            }
        });
        transactionList.clear();
        loadTransactions();
        if(!constants.synced){
            swipeRefreshLayout.setRefreshing(false);
            return;
        }
        getBalance();
        hideSyncIndicator();
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

    public void saveTransactions(ArrayList<TransactionsResponse.TransactionItem> transactions) {
        try {
            File path = new File(getContext().getFilesDir() + "/savedata/");
            path.mkdirs();
            File file = new File(getContext().getFilesDir() + "/savedata/transactions");
            file.createNewFile();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(file));
            objectOutputStream.writeObject(transactions);
            objectOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadTransactions() {
        try {
            File path = new File(getContext().getFilesDir() + "/savedata/");
            path.mkdirs();
            File file = new File(path, "transactions");
            if(file.exists()){
                ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(file));
                ArrayList<TransactionsResponse.TransactionItem> temp = (ArrayList<TransactionsResponse.TransactionItem>) objectInputStream.readObject();
                if (temp.size() > 0) {
                    if (temp.size() > getMaxDisplayItems()) {
                        transactionList.addAll(temp.subList(0, getMaxDisplayItems()));
                    } else {
                        transactionList.addAll(temp);
                    }
                    TransactionsResponse.TransactionItem latestTx = Collections.min(temp, new TransactionComparator.MinConfirmationSort());
                    latestTransactionHeight = latestTx.getHeight() + 1;
                }
                if (getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        transactionAdapter.notifyDataSetChanged();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(transactionList.size() == 0){
            recyclerView.setVisibility(View.GONE);
        }else{
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onRefresh() {
        getBalance();
        prepareHistoryData();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(getContext() != null) {
            IntentFilter filter = new IntentFilter(Constants.SYNCED);
            getContext().registerReceiver(receiver, filter);
        }
        isForeground = true;
        if (needsUpdate) {
            needsUpdate = false;
            prepareHistoryData();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(getContext() != null){
            getContext().unregisterReceiver(receiver);
        }
        isForeground = false;
    }

    @Override
    public void onResult(final String json) {
        if (getActivity() == null) {
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TransactionsResponse response = TransactionsResponse.parse(json);
                if(response.transactions.size() == 0){
                    recyclerView.setVisibility(View.GONE);
                    if (swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                } else {
                    ArrayList<TransactionsResponse.TransactionItem> transactions = response.transactions;
                    Collections.sort(transactions, new TransactionComparator.TimestampSort());
                    TransactionsResponse.TransactionItem latestTx = Collections.min(transactions, new TransactionComparator.MinConfirmationSort());
                    latestTransactionHeight = latestTx.getHeight() + 1;
                    transactionList.clear();
                    if (transactions.size() > 0) {
                        if (transactions.size() > getMaxDisplayItems()) {
                            transactionList.addAll(transactions.subList(0, getMaxDisplayItems()));
                        } else {
                            transactionList.addAll(transactions);
                        }
                    }
                    recyclerView.setVisibility(View.VISIBLE);
                    if (swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    transactionAdapter.notifyDataSetChanged();
                    saveTransactions(transactions);
                }
            }
        });
    }

    public void newTransaction(TransactionsResponse.TransactionItem transaction) {
        transaction.animate = true;

        for(int i = 0; i < transactionList.size(); i++){
            if(transactionList.get(i).hash.equals(transaction.hash)){
                //Transaction is a duplicate
                return;
            }
        }
        transactionList.add(0, transaction);
        if (transactionList.size() > getMaxDisplayItems()) {
            transactionList.remove(transactionList.size() - 1);
        }
        latestTransactionHeight = transaction.getHeight() + 1;

        if(getActivity() == null){
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                transactionAdapter.notifyDataSetChanged();
            }
        });
        getBalance();
    }

    public void transactionConfirmed(String hash, int height) {
        for (int i = 0; i < transactionList.size(); i++) {
            if (transactionList.get(i).hash.equals(hash)) {
                TransactionsResponse.TransactionItem transaction = transactionList.get(i);
                transaction.height = height;
                latestTransactionHeight = transaction.getHeight() + 1;
                transactionList.set(i, transaction);
                transactionAdapter.notifyDataSetChanged();
                break;
            }
        }
        getBalance();
    }

    public void blockAttached(int height) {
        if ((height - latestTransactionHeight) < 2) {
            for (int i = 0; i < transactionList.size(); i++) {
                TransactionsResponse.TransactionItem tx = transactionList.get(i);
                if ((height - tx.getHeight()) >= 2) {
                    continue;
                }
                tx.animate = true;
                final int finalI = i;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        transactionAdapter.notifyItemChanged(finalI);
                    }
                });
            }
        }
    }

    private void hideSyncIndicator(){
        ((AnimationDrawable) syncIndicator.getBackground()).stop();
        syncIndicator.setVisibility(View.GONE);
        tvBalance.setVisibility(View.VISIBLE);
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction() != null && intent.getAction().equals(Constants.SYNCED)) {
                getBalance();
                hideSyncIndicator();
            }
        }
    };
}
