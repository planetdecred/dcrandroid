package com.decrediton.Adapter;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.decrediton.R;
import com.decrediton.Util.AccountResponse;
import com.decrediton.data.Transaction;

import java.util.List;
import java.util.Locale;

/**
 * Created by Macsleven on 01/01/2018.
 */

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.MyViewHolder> {
    private List<Transaction> historyList;
    private LayoutInflater layoutInflater;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView Amount;
        private TextView txDate;
        private TextView txType;
        private TextView status;

        public MyViewHolder(View view) {
            super(view);
            Amount = view.findViewById(R.id.history_amount_transferred);
            txDate = view.findViewById(R.id.history_tx_date);
            txType = view.findViewById(R.id.history_snd_rcv);
            status = view.findViewById(R.id.history_tx_status);
        }
    }

    public TransactionAdapter(List<Transaction> historyListList , LayoutInflater inflater) {
        this.historyList = historyListList;
        this.layoutInflater = inflater;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView =layoutInflater.inflate(R.layout.history_list_row, parent, false);
        return new TransactionAdapter.MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Transaction history = historyList.get(position);
        holder.txType.setText(history.getTxType());
        holder.status.setText(history.getTxStatus());

        if(Double.parseDouble(history.getTransactionFee())>0){
           String temp ="- "+history.getTransactionFee() + " DCR";
            holder.Amount.setText(temp);

            holder.txType.setBackgroundResource(R.drawable.ic_send);
            holder.txType.setText("");
        }
        else {
            String temp =history.getAmount() + " DCR";
            holder.Amount.setText(temp);

            holder.txType.setBackgroundResource(R.drawable.ic_receive);
            holder.txType.setText("");
        }

        holder.txDate.setText(history.getTxDate());
        if(holder.status.getText().toString().equals("pending")){
            holder.status.setBackgroundResource(R.drawable.tx_status_pending);
            holder.status.setTextColor(Color.parseColor("#3d659c"));
        }
        else if(holder.status.getText().toString().equals("confirmed")) {
            holder.status.setBackgroundResource(R.drawable.tx_status_confirmed);
            holder.status.setTextColor(Color.parseColor("#55bb97"));
        }
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }


}
