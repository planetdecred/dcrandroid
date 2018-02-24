package com.dcrandroid.fragments;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
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

import dcrwallet.BlockScanResponse;
import dcrwallet.Dcrwallet;

import com.dcrandroid.util.AccountResponse;
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

/**
 * Created by Macsleven on 28/11/2017.
 */

public class OverviewFragment extends Fragment implements BlockScanResponse,SwipeRefreshLayout.OnRefreshListener{
    private List<Transaction> transactionList = new ArrayList<>(), tempTxList = new ArrayList<>();
    private Button reScanBlock;
    private CurrencyTextView tvBalance;
    private SwipeRefreshLayout swipeRefreshLayout;
    TransactionAdapter transactionAdapter;
    ProgressDialog pd;
    TextView refresh;
    PreferenceUtil util;
    RecyclerView recyclerView;
    @Nullable
    @Override
     public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        util = new PreferenceUtil(getContext());
        View rootView = inflater.inflate(R.layout.content_overview, container, false);
        LayoutInflater layoutInflater = LayoutInflater.from(rootView.getContext());
        //progressContainer = rootView.findViewById(R.id.progressContainers);
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
                i.putExtra("Amount",history.getAmount());
                i.putExtra("Fee",history.getTransactionFee());
                i.putExtra("TxDate",history.getTxDate());
                i.putExtra("TxType",history.getType());
                //i.putExtra("AccountName",history.getAccountName());
                i.putExtra("TxStatus",history.getTxStatus());
                i.putExtra("Hash", history.getHash());
                i.putStringArrayListExtra("UsedInput",history.getUsedInput());
                i.putStringArrayListExtra("newWalletOutPut",history.getWalletOutput());
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
                                new Thread(){
                                    public void run(){
                                        try {
                                            Looper.prepare();
                                            Dcrwallet.reScanBlocks(OverviewFragment.this, util.getInt("block_checkpoint"));
                                        }catch (Exception e){
                                            e.printStackTrace();
                                        }
                                    }
                                }.start();
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
        System.out.println("Is Running 4: "+Dcrwallet.isRunning());
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
                    final AccountResponse response = AccountResponse.parse(Dcrwallet.getAccounts());
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
                PreferenceUtil util = new PreferenceUtil(OverviewFragment.this.getContext());
                int blockHeight = Integer.parseInt(util.get(PreferenceUtil.BLOCK_HEIGHT,"0"));
                String result = Dcrwallet.getTransactions(blockHeight, 0);
                TransactionsResponse response = TransactionsResponse.parse(result);
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
                    util.set(PreferenceUtil.TRANSACTION_HEIGHT, String.valueOf(blockHeight));
                    final List<Transaction> temp = new ArrayList<>();
                    for (int i = 0; i < response.transactions.size(); i++) {
                        Transaction transaction = new Transaction();
                        TransactionsResponse.TransactionItem item = response.transactions.get(i);
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(item.timestamp * 1000);
                        SimpleDateFormat sdf = new SimpleDateFormat(" dd yyyy, hh:mma",Locale.getDefault());
                        //transaction.setTxDate(calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) + " " + calendar.get(Calendar.DATE) + " " + calendar.get(Calendar.YEAR) + ", " + calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND));
                        transaction.setTxDate(calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT,Locale.getDefault()) + sdf.format(calendar.getTime()).toLowerCase());
                        transaction.setTransactionFee(String.format(Locale.getDefault(), "%f", item.fee));
                        transaction.setType(item.type);
                        transaction.setHash(item.hash);
                        transaction.setAmount(String.format(Locale.getDefault(), "%f", item.amount));
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
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Collections.reverse(temp);
                            tempTxList.clear();
                            tempTxList.addAll(0, temp);
                            transactionList.clear();
                            if(tempTxList.size() > 0) {
                                if (tempTxList.size() > 7) {
                                    transactionList.addAll(tempTxList.subList(0, 7));
                                } else {
                                    transactionList.addAll(tempTxList.subList(0, tempTxList.size() - 1));
                                }
                                if(refresh.isShown()){
                                    refresh.setVisibility(View.INVISIBLE);
                                }
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
    public void onEnd(final long height) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(pd.isShowing()){
                    pd.dismiss();
                }
                Toast.makeText(getContext(), height+" "+getString(R.string.blocks_scanned), Toast.LENGTH_SHORT).show();
                util.setInt("block_checkpoint", (int) height);
                getBalance();
                prepareHistoryData();
            }
        });
    }

    @Override
    public void onScan(final long rescanned_through) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pd.show();
                PreferenceUtil util = new PreferenceUtil(OverviewFragment.this.getContext());
                //int percentage = (int) ((rescanned_through/Float.parseFloat(util.get(PreferenceUtil.BLOCK_HEIGHT))) * 100);
                pd.setMessage(getString(R.string.scanning_block)+" "+rescanned_through);
            }
        });
    }

    @Override
    public void onRefresh() {
        prepareHistoryData();
    }
}
