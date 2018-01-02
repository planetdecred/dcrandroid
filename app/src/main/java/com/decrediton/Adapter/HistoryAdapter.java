package com.decrediton.Adapter;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.decrediton.R;
import com.decrediton.data.Account;
import com.decrediton.data.Transaction;

import java.util.List;

/**
 * Created by Macsleven on 01/01/2018.
 */

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.MyViewHolder> {
    private List<Transaction> historyList;
    private LayoutInflater layoutInflater;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView Amount;
        private TextView txAddress;
        private TextView txDate;
        private TextView txType;
        private TextView status;


        public MyViewHolder(View view) {
            super(view);
            Amount = view.findViewById(R.id.history_amount_transfered);
            txAddress = view.findViewById(R.id.history_addr_transfered);
            txDate = view.findViewById(R.id.history_tx_date);
            txType = view.findViewById(R.id.history_snd_rcv);
            status = view.findViewById(R.id.history_tx_status);


        }
    }

    public HistoryAdapter(List<Transaction> historyListList , LayoutInflater inflater) {
        this.historyList = historyListList;
        this.layoutInflater = inflater;

    }
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView =layoutInflater.inflate(R.layout.history_list_row, parent, false);
        return new HistoryAdapter.MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {

        Transaction history = historyList.get(position);
        holder.txAddress.setText(history.getAddress());
        holder.txType.setText(history.getTxType());
        holder.status.setText(history.getTxStatus());
        holder.Amount.setText(history.getAmount());
        holder.txDate.setText(history.getTxDate());
        if(holder.status.getText().toString().equals("pending")){
            holder.status.setBackgroundResource(R.drawable.tx_status_pending);
            holder.status.setTextColor(Color.parseColor("#3d659c"));
        }
        else if(holder.status.getText().toString().equals("confirmed")) {
            holder.status.setBackgroundResource(R.drawable.tx_status_confirmed);
            holder.status.setTextColor(Color.parseColor("#55bb97"));
        }
        if(holder.txType.getText().equals("send")){
            holder.txType.setBackgroundResource(R.drawable.ic_menu_send);
            holder.txType.setText("");

        }
        else {
            holder.txType.setBackgroundResource(R.drawable.ic_menu_send);
            holder.txType.setText("");

        }
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }


}
