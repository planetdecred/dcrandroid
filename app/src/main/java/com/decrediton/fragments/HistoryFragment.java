package com.decrediton.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.decrediton.Activities.TransactionDetailsActivity;
import com.decrediton.Adapter.TransactionAdapter;
import com.decrediton.R;
import com.decrediton.Util.RecyclerTouchListener;
import com.decrediton.data.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Macsleven on 28/11/2017.
 */

public class HistoryFragment extends Fragment{
        private List<Transaction> transactionList = new ArrayList<>();
        TransactionAdapter transactionAdapter;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.content_history, container, false);
        LayoutInflater layoutInflater = LayoutInflater.from(rootView.getContext());
        RecyclerView recyclerView = rootView.getRootView().findViewById(R.id.history_recycler_view);
        transactionAdapter = new TransactionAdapter(transactionList, layoutInflater);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(rootView.getContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration( getContext(), LinearLayoutManager.VERTICAL));
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Transaction history = transactionList.get(position);
                Intent i = new Intent(getContext(), TransactionDetailsActivity.class);
                i.putExtra("Amount",history.getAmount());
                i.putExtra("Fee",history.getTransactionFee());
                i.putExtra("TxDate",history.getTxDate());
                i.putExtra("TxType",history.getTxType());
                i.putExtra("AccountName",history.getAccountName());
                i.putExtra("TxStatus",history.getTxStatus());
                i.putStringArrayListExtra("UsedIput",history.getUsedIput());
                i.putStringArrayListExtra("newWalletOutPut",history.getWalletOutput());
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
        getActivity().setTitle("History");
    }
    private void prepareHistoryData(){
        ArrayList<String> usedInput = new ArrayList<>();
        usedInput.add("TXFHUEKFHDUWKDLFHEJWIDFLDJFJSHERHDAS:  1.00 DCR");
        ArrayList<String> output = new ArrayList<>();
        output.add("TXFJFHEJDUFHWQIMCNVHFKRHFUCIFNDHFJH:     3.22 DCR");
        output.add("TXJKFJFUVNDJFKVNFJEJFKFJVCXJFKGJNFKJE:   3.54 DCR");
        Transaction transaction= new Transaction("0.0000000","0.02","jan 1 2018, 20:19:45","pending","default","send", usedInput, output);
        transactionList.add(transaction);
         usedInput = new ArrayList<>();
        usedInput.add("TXFHUEKFHDUWKDLFHEJWIDFLDJFJSHERHDAS:  5.00 DCR");
        output = new ArrayList<>();
        output.add("TXFJFHEJDUFHWQIMCNVHFKRHFUCIFNDHFJH:     6.22 DCR");
        output.add("TXJKFJFUVNDJFKVNFJEJFKFJVCXJFKGJNFKJE:   3.54 DCR");
        transaction= new Transaction("120.0000000","0.00","jan 1 2018, 11:17:25","pending","default","receive", usedInput, output);
        transactionList.add(transaction);
    }
}
