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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.dcrandroid.BuildConfig;
import com.dcrandroid.R;
import com.dcrandroid.activities.TransactionDetailsActivity;
import com.dcrandroid.adapter.TransactionAdapter;
import com.dcrandroid.data.Constants;
import com.dcrandroid.util.DcrConstants;
import com.dcrandroid.util.RecyclerTouchListener;
import com.dcrandroid.util.TransactionComparator;
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

    private TextView refresh;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private Spinner spinnerHistory;

    private int latestTransactionHeight;
    private boolean needsUpdate = false, isForeground;
    private String transactionTypeSelected = "";

    private TransactionAdapter transactionAdapter;
    private ArrayAdapter<String> sortSpinnerAdapter;

    private List<TransactionItem> transactionList = new ArrayList<>();
    private List<TransactionItem> fixedTransactionList = new ArrayList<>();
    private ArrayList<String> availableTxTypes = new ArrayList<>();

    private DcrConstants constants;

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
                if (transactionList.size() <= position || position < 0) {
                    return;
                }
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
        setupSortListener();
        return rootView;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getActivity() != null)
            getActivity().setTitle(getString(R.string.history));

        swipeRefreshLayout.setOnRefreshListener(this);
        prepareHistoryData();
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

        recyclerView.setVisibility(View.GONE);
        refresh.setVisibility(View.VISIBLE);
        swipeRefreshLayout.setRefreshing(true);

        loadTransactions();

        if (transactionList.size() == 0) {
            refresh.setText(R.string.no_transactions);
            refresh.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            refresh.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        if (!constants.synced) {
            refresh.setText(R.string.no_transactions_sync);
            swipeRefreshLayout.setRefreshing(false);
            System.out.println("Not going past cached transactions");
            return;
        }

        System.out.println("Loading from wallet");
        new Thread() {
            public void run() {
                try {
                    System.out.println("About to Load");
                    constants.wallet.getTransactions(HistoryFragment.this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void saveTransactions() {
        try {
            if (getActivity() == null || getContext() == null) {
                return;
            }

            File path = new File(getContext().getFilesDir() + "/" + BuildConfig.NetType + "/" + "savedata");
            path.mkdirs();
            File file = new File(path, "history_transactions");
            file.createNewFile();

            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(file));
            objectOutputStream.writeObject(fixedTransactionList);
            objectOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadTransactions() {
        try {
            if (getActivity() == null || getContext() == null) {
                return;
            }

            File path = new File(getContext().getFilesDir() + "/" + BuildConfig.NetType + "/" + "savedata");
            path.mkdirs();
            File file = new File(path, "history_transactions");
            if (file.exists()) {
                ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(file));
                List<TransactionItem> temp = (List<TransactionItem>) objectInputStream.readObject();
                fixedTransactionList.clear();
                fixedTransactionList.addAll(temp);
                transactionList.clear();
                transactionList.addAll(0, fixedTransactionList);
                TransactionsResponse.TransactionItem latestTx = Collections.min(temp, new TransactionComparator.MinConfirmationSort());
                latestTransactionHeight = latestTx.getHeight() + 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
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
                    refresh.setText(R.string.no_transactions_have_occured);
                    refresh.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    if (swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                } else {
                    fixedTransactionList = response.transactions;
                    Collections.sort(fixedTransactionList, new TransactionComparator.TimestampSort());
                    TransactionsResponse.TransactionItem latestTx = Collections.min(fixedTransactionList, new TransactionComparator.MinConfirmationSort());
                    latestTransactionHeight = latestTx.getHeight() + 1;

                    availableTxTypes.clear();

                    for (int i = 0; i < fixedTransactionList.size(); i++) {
                        String type = fixedTransactionList.get(i).type;
                        if (type.equalsIgnoreCase(Constants.VOTE) || type.equalsIgnoreCase(Constants.TICKET_PURCHASE)
                                || type.equalsIgnoreCase(Constants.REVOCATION)) {
                            type = Constants.STAKING.toUpperCase();
                        }

                        type = firstLetterCap(type);
                        if (!availableTxTypes.contains(type)) {
                            availableTxTypes.add(type);
                        }

                        if (availableTxTypes.size() >= 3) { // There're only 3 sort types
                            break;
                        }
                    }

                    availableTxTypes.add(0, "All");

                    if (getContext() == null) {
                        return;
                    }

                    String[] types = availableTxTypes.toArray(new String[0]);
                    sortSpinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, types);
                    sortSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerHistory.setAdapter(sortSpinnerAdapter);


                    sortTransactions();

                    saveTransactions();
                }
            }
        });
    }

    @Override
    public void onRefresh() {
        prepareHistoryData();
    }

    private void sortTransactions() {
        transactionList.clear();
        if (transactionTypeSelected.equalsIgnoreCase("ALL")) {
            transactionList.addAll(fixedTransactionList);
            transactionAdapter.notifyDataSetChanged();
        } else {
            for (int i = 0; i < fixedTransactionList.size(); i++) {
                TransactionItem item = fixedTransactionList.get(i);
                if (transactionTypeSelected.equalsIgnoreCase(Constants.STAKING)) {
                    if (item.type.equals(Constants.VOTE) || item.type.equals(Constants.REVOCATION)
                            || item.type.equals(Constants.TICKET_PURCHASE)) {
                        transactionList.add(item);
                    }
                } else if (transactionTypeSelected.equalsIgnoreCase(item.type)) { // Regular & coinbase transaction
                    transactionList.add(item);
                }
            }
        }

        if (transactionList.size() > 0) {
            recyclerView.setVisibility(View.VISIBLE);
            refresh.setVisibility(View.GONE);
        } else {
            refresh.setText(R.string.no_transactions);
            refresh.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }

        if (swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }

        transactionAdapter.notifyDataSetChanged();
    }

    public void newTransaction(TransactionsResponse.TransactionItem transaction) {
        latestTransactionHeight = transaction.getHeight() + 1;
        fixedTransactionList.add(0, transaction);

        if (transactionTypeSelected.equalsIgnoreCase("ALL") ||
                transactionTypeSelected.equalsIgnoreCase(transaction.type) ||
                (transactionTypeSelected.equalsIgnoreCase(Constants.STAKING) &&
                        (transaction.type.equals(Constants.VOTE) ||
                                transaction.type.equals(Constants.REVOCATION) ||
                                transaction.type.equals(Constants.TICKET_PURCHASE)))) {
            transaction.animate = true;
            transactionList.add(0, transaction);
            recyclerView.post(new Runnable() {
                @Override
                public void run() {
                    transactionAdapter.notifyDataSetChanged();
                }
            });
        }
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

    public void blockAttached(int height) {
        if ((height - latestTransactionHeight) < 2) {
            for (int i = 0; i < transactionList.size(); i++) {
                TransactionsResponse.TransactionItem tx = transactionList.get(i);
                if ((height - tx.getHeight()) >= 2) {
                    continue;
                }
                tx.animate = true;
                final int finalI = i;
                if (getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        transactionAdapter.notifyItemChanged(finalI);
                    }
                });
            }
        }
    }

    private void setupSortListener() {
        spinnerHistory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                transactionTypeSelected = availableTxTypes.get(position);
                sortTransactions();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private String firstLetterCap(String s) {
        if (s.length() > 0) {
            return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
        }
        return s;
    }
}