package com.dcrandroid.adapter;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dcrandroid.R;
import com.dcrandroid.data.Constants;
import com.dcrandroid.data.Transaction;
import com.dcrandroid.util.DcrConstants;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.Utils;
import com.dcrandroid.view.CurrencyTextView;

import java.text.NumberFormat;
import java.util.List;

/**
 * Created by Macsleven on 01/01/2018.
 */

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.MyViewHolder> {
    private List<Transaction> historyList;
    private LayoutInflater layoutInflater;
    private PreferenceUtil util;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private CurrencyTextView Amount;
        private TextView txType;
        private TextView status;
        private CurrencyTextView minus;
        public MyViewHolder(View view) {
            super(view);
            Amount = view.findViewById(R.id.history_amount_transferred);
            txType = view.findViewById(R.id.history_snd_rcv);
            status = view.findViewById(R.id.history_tx_status);
            minus = view.findViewById(R.id.history_minus);
        }
    }

    public TransactionAdapter(List<Transaction> historyListList , LayoutInflater inflater) {
        this.historyList = historyListList;
        this.layoutInflater = inflater;
        this.util = new PreferenceUtil(inflater.getContext());
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView =layoutInflater.inflate(R.layout.history_list_row, parent, false);
        return new TransactionAdapter.MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Transaction history = historyList.get(position);
        holder.txType.setText(history.getType());

        int confirmations = DcrConstants.getInstance().wallet.getBestBlock() - history.getHeight();
        if(history.getHeight() == 0){
            //No included in block chain, therefore transaction is pending
            holder.status.setTextColor(Color.parseColor("#3d659c"));
            holder.status.setText("pending");
        }else{
            if(util.getBoolean(Constants.KEY_SPEND_UNCONFIRMED_FUNDS) || confirmations > 1){
                holder.status.setTextColor(Color.parseColor("#55bb97"));
                holder.status.setText("confirmed");
            }else{
                holder.status.setTextColor(Color.parseColor("#3d659c"));
                holder.status.setText("pending");
            }
        }

        if(history.getTransactionFee() > 0){
            holder.Amount.formatAndSetText(Utils.formatDecred(history.totalInput));
            holder.minus.setVisibility(View.VISIBLE);
            holder.txType.setBackgroundResource(R.drawable.ic_send);
            holder.txType.setText("");
        }else {
            holder.Amount.formatAndSetText(Utils.formatDecred(history.getAmount()));
            holder.minus.setVisibility(View.INVISIBLE);
            holder.txType.setBackgroundResource(R.drawable.ic_receive);
            holder.txType.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }


}
