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
import com.decrediton.Util.Utils;
import com.decrediton.data.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Macsleven on 28/11/2017.
 */

public class OverviewFragment extends Fragment implements BlockScanResponse{
    private List<Transaction> transactionList = new ArrayList<>();
    private Button reScanBlock;
    private TextView tvBalance;
    TransactionAdapter transactionAdapter;
    //Buy sticky notes
    ProgressDialog pd;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.content_overview, container, false);
        LayoutInflater layoutInflater = LayoutInflater.from(rootView.getContext());
        RecyclerView recyclerView = rootView.getRootView().findViewById(R.id.history_recycler_view2);
        transactionAdapter = new TransactionAdapter(transactionList, layoutInflater);
        reScanBlock =  rootView.getRootView().findViewById(R.id.overview_rescan_btn);
        tvBalance = rootView.getRootView().findViewById(R.id.overview_av_balance);
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
                    final float finalTotalBalance = totalBalance;
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvBalance.setText(String.format(Locale.getDefault(),"%f DCR",finalTotalBalance));
                        }
                    });
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void prepareHistoryData(){
        ArrayList<String> usedInput = new ArrayList<>();
        usedInput.add("TXFHUEKFHDUWKDLFHEJWIDFLDJFJSHERHDAS:  1.00 DCR");
        ArrayList<String> output = new ArrayList<>();
        output.add("TXFJFHEJDUFHWQIMCNVHFKRHFUCIFNDHFJH:     3.22 DCR");
        output.add("TXJKFJFUVNDJFKVNFJEJFKFJVCXJFKGJNFKJE:   3.54 DCR");
        Transaction transaction= new Transaction("0.0000000","0.02","jan 1 2018, 20:19:45","pending","default","send", usedInput, output);
        //transactionList.add(transaction);
        usedInput = new ArrayList<>();
        usedInput.add("TXFHUEKFHDUWKDLFHEJWIDFLDJFJSHERHDAS:  5.00 DCR");
        output = new ArrayList<>();
        output.add("TXFJFHEJDUFHWQIMCNVHFKRHFUCIFNDHFJH:     6.22 DCR");
        output.add("TXJKFJFUVNDJFKVNFJEJFKFJVCXJFKGJNFKJE:   3.54 DCR");
        transaction= new Transaction("120.0000000","0.00","jan 1 2018, 11:17:25","pending","default","receive", usedInput, output);
        //transactionList.add(transaction);
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
