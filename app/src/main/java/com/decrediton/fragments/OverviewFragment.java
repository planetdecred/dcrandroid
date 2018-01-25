package com.decrediton.fragments;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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

import com.decrediton.Activities.TransactionDetailsActivity;
import com.decrediton.Adapter.TransactionAdapter;
import com.decrediton.R;

import dcrwallet.BlockScanResponse;
import dcrwallet.Dcrwallet;

import com.decrediton.Util.AccountResponse;
import com.decrediton.Util.PreferenceUtil;
import com.decrediton.Util.RecyclerTouchListener;
import com.decrediton.Util.TransactionsResponse;
import com.decrediton.Util.Utils;
import com.decrediton.data.Transaction;
import com.decrediton.view.CurrencyTextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Created by Macsleven on 28/11/2017.
 */

public class OverviewFragment extends Fragment implements BlockScanResponse{
    private List<Transaction> transactionList = new ArrayList<>(), tempTxList = new ArrayList<>();
    private Button reScanBlock;
    private CurrencyTextView tvBalance;
    TransactionAdapter transactionAdapter;
    ProgressDialog pd;
    PreferenceUtil util;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        util = new PreferenceUtil(getContext());
        View rootView = inflater.inflate(R.layout.content_overview, container, false);
        LayoutInflater layoutInflater = LayoutInflater.from(rootView.getContext());
        RecyclerView recyclerView = rootView.getRootView().findViewById(R.id.history_recycler_view2);
        transactionAdapter = new TransactionAdapter(transactionList, layoutInflater);
        reScanBlock =  rootView.getRootView().findViewById(R.id.overview_rescan_btn);
        tvBalance = rootView.getRootView().findViewById(R.id.overview_av_balance);
        tvBalance.formatAndSetText(String.format(Locale.getDefault(),"%.8f DCR", util.getFloat(PreferenceUtil.TOTAL_BALANCE)));
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
                pd.show();
                new Thread(){
                    public void run(){
                        try {
                            //final String result = Dcrwallet.runUtil();
                            Looper.prepare();
                            Dcrwallet.reScanBlocks(OverviewFragment.this);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }.start();
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
                    final AccountResponse response = AccountResponse.parse(Dcrwallet.getAccounts());
                    float totalBalance = 0;
                    for(int i = 0; i < response.items.size(); i++){
                        AccountResponse.Balance balance = response.items.get(i).balance;
                        totalBalance += balance.total;
                    }
                    util.setFloat(PreferenceUtil.TOTAL_BALANCE,totalBalance);
                    final float finalTotalBalance = totalBalance;
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvBalance.formatAndSetText(String.format(Locale.getDefault(),"%.8f DCR",finalTotalBalance));
                        }
                    });
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void prepareHistoryData(){
        //loadTransactions();
        new Thread(){
            public void run(){
                PreferenceUtil util = new PreferenceUtil(OverviewFragment.this.getContext());
                int blockHeight = Integer.parseInt(util.get(PreferenceUtil.BLOCK_HEIGHT,"0"));
                int startHeight = Integer.parseInt(util.get(PreferenceUtil.TRANSACTION_HEIGHT,"1"));
                String result = Dcrwallet.getTransactions(blockHeight, 1);
                TransactionsResponse response = TransactionsResponse.parse(result);
                if(response.errorOccurred){

                }else {
                    util.set(PreferenceUtil.TRANSACTION_HEIGHT, String.valueOf(blockHeight));
                    final List<Transaction> temp = new ArrayList<>();
                    for (int i = 0; i < response.transactions.size(); i++) {
                        Transaction transaction = new Transaction();
                        TransactionsResponse.TransactionItem item = response.transactions.get(i);
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(item.timestamp * 1000);
                        transaction.setTxDate(calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) + " " + calendar.get(Calendar.DATE) + " " + calendar.get(Calendar.YEAR) + ", " + calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND));
                        transaction.setTransactionFee(String.format(Locale.getDefault(), "%.8f", item.fee));
                        transaction.setType(item.type);
                        transaction.setHash(item.hash);
                        transaction.setAmount(String.format(Locale.getDefault(), "%.8f", item.amount));
                        transaction.setTxStatus(item.status);
                        ArrayList<String> usedInput = new ArrayList<>();
                        for (int j = 0; j < item.debits.size(); j++) {
                            usedInput.add(item.debits.get(j).accountName + "\n" + String.format(Locale.getDefault(), "%f", item.debits.get(j).previous_amount));
                        }
                        ArrayList<String> output = new ArrayList<>();
                        for (int j = 0; j < item.credits.size(); j++) {
                            output.add(item.credits.get(j).address + "\n" + String.format(Locale.getDefault(), "%f", item.credits.get(j).amount));
                            //System.out.println(credit.address+"\n"+String.format(Locale.getDefault(),"%f",credit.amount));
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
                            tempTxList.addAll(0, temp);
                            transactionList.clear();
                            transactionList.addAll(tempTxList.subList(0, 7));
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
                tempTxList = (List<Transaction>) objectInputStream.readObject();
                transactionList.addAll(tempTxList.subList(0, 7));
                transactionAdapter.notifyDataSetChanged();
                System.out.println("Done: "+transactionList.size());
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
                Toast.makeText(getContext(), height+" "+getString(R.string.blocek_scanned), Toast.LENGTH_SHORT).show();
                getBalance();
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
                int percentage = (int) ((rescanned_through/Float.parseFloat(util.get(PreferenceUtil.BLOCK_HEIGHT))) * 100);
                System.out.println("Rescanned: "+rescanned_through+" Height: "+util.get(PreferenceUtil.BLOCK_HEIGHT)
                        +" Division: "+rescanned_through/Float.parseFloat(util.get(PreferenceUtil.BLOCK_HEIGHT))+" Percentage: "+percentage);
                pd.setMessage(getString(R.string.scanning_block)+" "+percentage+"%");
            }
        });
    }
}
