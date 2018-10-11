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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.dcrandroid.R;
import com.dcrandroid.activities.TransactionDetailsActivity;
import com.dcrandroid.adapter.TransactionAdapter;
import com.dcrandroid.data.Constants;
import com.dcrandroid.util.DcrConstants;
import com.dcrandroid.util.RecyclerTouchListener;
import com.dcrandroid.util.TransactionComparator;
import com.dcrandroid.util.TransactionItemSorter;
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
    private ArrayList<TransactionItem> transactions = new ArrayList<>();
    private TransactionAdapter transactionAdapter;
    private TextView refresh;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private DcrConstants constants;
    private Spinner spinnerHistory;
    private int latestTransactionHeight;
    private boolean needsUpdate = false, isForeground;
    private String transactionTypeSelected = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        constants = DcrConstants.getInstance();
        View rootView = inflater.inflate(R.layout.content_history, container, false);
        spinnerHistory = rootView.findViewById(R.id.spinnerHistory);
        swipeRefreshLayout = rootView.getRootView().findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setColorSchemeResources(
                R.color.colorPrimaryDarkBlue,
                R.color.colorPrimaryDarkBlue,
                R.color.colorPrimaryDarkBlue,
                R.color.colorPrimaryDarkBlue);
        swipeRefreshLayout.setOnRefreshListener(this);
        refresh = rootView.getRootView().findViewById(R.id.no_history);
        transactionAdapter = new TransactionAdapter(transactionList, rootView.getContext());
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

        recyclerView.setAdapter(transactionAdapter);
        registerForContextMenu(recyclerView);
        prepareHistoryData();
        setupSort();
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
        if (needsUpdate) {
            needsUpdate = false;
            prepareHistoryData();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        isForeground = false;
    }

    public void prepareHistoryData() {
        if (!isForeground) {
            needsUpdate = true;
            return;
        }
        recyclerView.setVisibility(View.VISIBLE);
        swipeRefreshLayout.setRefreshing(true);
        loadTransactions();
        if (!constants.synced) {
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        recyclerView.setVisibility(View.VISIBLE);
        new Thread() {
            public void run() {
                try {
                    constants.wallet.getTransactions(HistoryFragment.this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void saveTransactions() {
        try {
            File path = new File(getContext().getFilesDir() + "/savedata/");
            path.mkdirs();
            File file = new File(getContext().getFilesDir() + "/savedata/history_transactions");
            file.createNewFile();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(file));
            objectOutputStream.writeObject(transactionList);
            objectOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadTransactions() {
        try {
            File path = new File(getContext().getFilesDir() + "/savedata/");
            path.mkdirs();
            File file = new File(getContext().getFilesDir() + "/savedata/history_transactions");
            if (file.exists()) {
                ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(file));
                List<TransactionItem> temp = (List<TransactionItem>) objectInputStream.readObject();
                transactionList.addAll(temp);
                TransactionsResponse.TransactionItem latestTx = Collections.min(temp, new TransactionComparator.MinConfirmationSort());
                latestTransactionHeight = latestTx.getHeight() + 1;
                transactionAdapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (transactionList.size() == 0) {
            recyclerView.setVisibility(View.GONE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onRefresh() {
        if(!transactionTypeSelected.equals("")) {
            swipeRefreshLayout.setRefreshing(false);
        } else {
            prepareHistoryData();
        }
    }

    public void newTransaction(TransactionsResponse.TransactionItem transaction) {
        transaction.animate = true;
        latestTransactionHeight = transaction.getHeight() + 1;
        transactionList.add(0, transaction);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                transactionAdapter.notifyDataSetChanged();
            }
        });
    }

    public void transactionConfirmed(String hash, int height) {
        for (int i = 0; i < transactionList.size(); i++) {
            if (transactionList.get(i).hash.equals(hash)) {
                TransactionsResponse.TransactionItem transaction = transactionList.get(i);
                transaction.height = height;
                latestTransactionHeight = transaction.getHeight() + 1;
                transactionList.set(i, transaction);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        transactionAdapter.notifyDataSetChanged();
                    }
                });
                break;
            }
        }
    }

    @Override
    public void onResult(final String s) {
        if (getActivity() == null) {
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TransactionsResponse response = TransactionsResponse.parse(s);
                if (response.transactions.size() == 0) {
                    recyclerView.setVisibility(View.GONE);
                    if (swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                } else {
                    transactions = response.transactions;
                    Collections.sort(transactions, new TransactionComparator.TimestampSort());
                    TransactionsResponse.TransactionItem latestTx = Collections.min(transactions, new TransactionComparator.MinConfirmationSort());
                    latestTransactionHeight = latestTx.getHeight() + 1;
                    transactionList.clear();
                    transactionList.addAll(0, transactions);
                    recyclerView.setVisibility(View.VISIBLE);
                    if (swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    transactionAdapter.notifyDataSetChanged();
                    saveTransactions();

                }
            }
        });
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

    private void setupSort() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(), R.array.history_transaction_list, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHistory.setAdapter(adapter);
        spinnerHistory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                switch (position) {
                    case 0:
                        transactionTypeSelected = "";
                        onRefresh();
                        break;
                    case 1:
                        transactionTypeSelected = "regular";
                        TransactionItemSorter.itemSorter(transactionList, transactions, transactionTypeSelected);
                        transactionAdapter.notifyDataSetChanged();
                        break;
                    case 2:
                        transactionTypeSelected = "0";
                        TransactionItemSorter.itemSorter(transactionList, transactions, transactionTypeSelected);
                        transactionAdapter.notifyDataSetChanged();
                        break;
                    case 3:
                        transactionTypeSelected = "1";
                        TransactionItemSorter.itemSorter(transactionList, transactions, transactionTypeSelected);
                        transactionAdapter.notifyDataSetChanged();
                        break;
                    case 4:
                        transactionTypeSelected = "ticket";
                        TransactionItemSorter.itemSorter(transactionList, transactions, transactionTypeSelected);
                        transactionAdapter.notifyDataSetChanged();
                        break;
                    case 5:
                        transactionTypeSelected = "vote";
                        TransactionItemSorter.itemSorter(transactionList, transactions, transactionTypeSelected);
                        transactionAdapter.notifyDataSetChanged();
                        break;
                    case 6:
                        transactionTypeSelected = "revoke";
                        TransactionItemSorter.itemSorter(transactionList, transactions, transactionTypeSelected);
                        transactionAdapter.notifyDataSetChanged();
                        break;
                }
            }


            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }


}