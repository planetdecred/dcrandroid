package com.dcrandroid.fragments;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.dcrandroid.activities.TransactionDetailsActivity;
import com.dcrandroid.adapter.TransactionAdapter;
import com.dcrandroid.R;

//import dcrwallet.BlockScanResponse;
//import dcrwallet.Dcrwallet;

import com.dcrandroid.data.Constants;
import com.dcrandroid.util.AccountResponse;
import com.dcrandroid.util.DcrConstants;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.RecyclerTouchListener;
import com.dcrandroid.util.TransactionsResponse;
import com.dcrandroid.util.Utils;
import com.dcrandroid.data.Transaction;
import com.dcrandroid.view.CurrencyTextView;

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

import mobilewallet.BlockScanResponse;
import mobilewallet.GetTransactionsResponse;

/**
 * Created by Macsleven on 28/11/2017.
 */

//public class OverviewFragment extends Fragment implements BlockScanResponse,SwipeRefreshLayout.OnRefreshListener{
public class OverviewFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, BlockScanResponse, GetTransactionsResponse {
    private List<Transaction> transactionList = new ArrayList<>(), tempTxList = new ArrayList<>();
    private Button reScanBlock;
    private CurrencyTextView tvBalance;
    private SwipeRefreshLayout swipeRefreshLayout;
    TransactionAdapter transactionAdapter;
    ProgressDialog pd;
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
        reScanBlock =  rootView.getRootView().findViewById(R.id.overview_rescan_btn);
        tvBalance = rootView.getRootView().findViewById(R.id.overview_av_balance);
        tvBalance.formatAndSetText(String.format(Locale.getDefault(),"%f DCR", util.getFloat(PreferenceUtil.TOTAL_BALANCE)));
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(rootView.getContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration( getContext(), LinearLayoutManager.VERTICAL));
        pd = Utils.getProgressDialog(getContext(),false,false,getString(R.string.scanning_blocks));
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Transaction history = transactionList.get(position);
                Intent i = new Intent(getContext(), TransactionDetailsActivity.class);
                i.putExtra("Height", history.getHeight());
                i.putExtra(Constants.EXTRA_AMOUNT,history.getAmount());
                i.putExtra(Constants.EXTRA_TRANSACTION_FEE,history.getTransactionFee());
                i.putExtra(Constants.EXTRA_TRANSACTION_DATE,history.getTxDate());
                i.putExtra(Constants.EXTRA_TRANSACTION_TYPE,history.getType());
                i.putExtra(Constants.EXTRA_TRANSACTION_TOTAL_INPUT, history.totalInput);
                i.putExtra(Constants.EXTRA_TRANSACTION_TOTAL_OUTPUT, history.totalOutput);
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
        reScanBlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(getContext())
                        .setTitle("Rescan blocks")
                        .setMessage("Are you sure? This could take some time.")
                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                pd.show();
                                constants.wallet.rescan(0, OverviewFragment.this);
                                //Dcrwallet.reScanBlocks(OverviewFragment.this, util.getInt("block_checkpoint"));
                            }
                        }).setNegativeButton("NO", null)
                        .show();
            }
        });
        recyclerView.setAdapter(transactionAdapter);
        registerForContextMenu(recyclerView);
        prepareHistoryData();
        getBalance();
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //you can set the title for your toolbar here for different fragments different titles
        getActivity().setTitle(getString(R.string.overview));
    }

    private void getBalance(){
        new Thread(){
            public void run(){
                try {
                    if(getContext() == null){
                        return;
                    }
                    final AccountResponse response = AccountResponse.parse(constants.wallet.getAccounts());
                    float totalBalance = 0;
                    for(int i = 0; i < response.items.size(); i++){
                        AccountResponse.Balance balance = response.items.get(i).balance;
                        totalBalance += balance.total;
                    }
                    util.setFloat(PreferenceUtil.TOTAL_BALANCE,totalBalance);
                    final float finalTotalBalance = totalBalance;
                    if(getActivity() == null){
                        return;
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvBalance.formatAndSetText(String.format(Locale.getDefault(),"%f DCR",finalTotalBalance));
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
        tempTxList.clear();
        transactionList.clear();
        loadTransactions();
        new Thread(){
            public void run(){
                if(OverviewFragment.this.getContext() == null){
                    return;
                }
                PreferenceUtil util = new PreferenceUtil(OverviewFragment.this.getContext());
                int blockHeight = util.getInt(PreferenceUtil.BLOCK_HEIGHT);
                String result = null;
                try {
                    constants.wallet.getTransactions(OverviewFragment.this);
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
            File file = new File(getContext().getFilesDir()+"/savedata/transactions");
            file.createNewFile();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(file));
            objectOutputStream.writeObject(tempTxList);
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
                tempTxList = (List<Transaction>) objectInputStream.readObject();
                if(tempTxList.size() > 0) {
                    if (tempTxList.size() > 7) {
                        transactionList.addAll(tempTxList.subList(0, 7));
                    } else {
                        transactionList.addAll(tempTxList.subList(0, tempTxList.size() - 1));
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
    public void onEnd(final int height, final boolean cancelled) {
        if(getActivity() == null){
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(!cancelled) {
                    if (pd.isShowing()) {
                        pd.dismiss();
                    }
                    Toast.makeText(getContext(), height + " " + getString(R.string.blocks_scanned), Toast.LENGTH_SHORT).show();
                    util.setInt("block_checkpoint", height);
                    getBalance();
                    prepareHistoryData();
                }else{
                    Toast.makeText(getContext(), "Rescan cancelled", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onError(int code, final String message) {
        if(getActivity() == null){
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onScan(final int rescanned_through) {
        if(getActivity() == null){
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pd.show();
                pd.setMessage(getString(R.string.scanning_block)+" "+rescanned_through);
            }
        });

    }

    @Override
    public void onResult(String json) {
        TransactionsResponse response = TransactionsResponse.parse(json);
        if(getActivity() == null){
            return;
        }
        if(response.errorOccurred){
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refresh.setVisibility(View.VISIBLE);
                    if(swipeRefreshLayout.isRefreshing()){
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    recyclerView.setVisibility(View.GONE);
                }
            });
        }
        else if(response.transactions.size() == 0){
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refresh.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    if(swipeRefreshLayout.isRefreshing()){
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }
            });
        }else {
            ///util.setInt(PreferenceUtil.TRANSACTION_HEIGHT, blockHeight);
            final List<Transaction> temp = new ArrayList<>();
            for (int i = 0; i < response.transactions.size(); i++) {
                Transaction transaction = new Transaction();
                TransactionsResponse.TransactionItem item = response.transactions.get(i);
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(item.timestamp * 1000);
                SimpleDateFormat sdf = new SimpleDateFormat(" dd yyyy, hh:mma", Locale.getDefault());
                transaction.setTxDate(calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) + sdf.format(calendar.getTime()).toLowerCase());
                transaction.setTransactionFee(item.fee);
                transaction.setType(item.type);
                transaction.setHash(item.hash);
                transaction.setHeight(item.height);
                transaction.setAmount(item.amount);
                transaction.setTxStatus(item.status);
                ArrayList<String> usedInput = new ArrayList<>();
                for (int j = 0; j < item.debits.size(); j++) {
                    transaction.totalInput += item.debits.get(j).previous_amount;
                    usedInput.add(item.debits.get(j).accountName + "\n" + String.format(Locale.getDefault(), "%f", item.debits.get(j).previous_amount));
                }
                ArrayList<String> output = new ArrayList<>();
                for (int j = 0; j < item.credits.size(); j++) {
                    transaction.totalOutput += item.credits.get(j).amount;
                    output.add(item.credits.get(j).address + "\n" + String.format(Locale.getDefault(), "%f", item.credits.get(j).amount));
                }
                transaction.setUsedInput(usedInput);
                transaction.setWalletOutput(output);
                if (item.status.equalsIgnoreCase("pending")) {
                    System.out.println("Adding pending to top");
                    temp.add(transaction);
                } else {
                    temp.add(transaction);
                }
            }
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Collections.reverse(temp);
                    tempTxList.clear();
                    tempTxList.addAll(0, temp);
                    transactionList.clear();
                    if (tempTxList.size() > 0) {
                        if (tempTxList.size() > 7) {
                            transactionList.addAll(tempTxList.subList(0, 7));
                        } else {
                            transactionList.addAll(tempTxList.subList(0, tempTxList.size() - 1));
                        }
                        if (refresh.isShown()) {
                            refresh.setVisibility(View.INVISIBLE);
                        }
                    }
                    recyclerView.setVisibility(View.VISIBLE);
                    if (swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    transactionAdapter.notifyDataSetChanged();
                    saveTransactions();
                }
            });
        }
    }
}
