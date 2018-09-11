package com.dcrandroid.adapter;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.dcrandroid.R;
import com.dcrandroid.data.Constants;
import com.dcrandroid.util.CoinFormat;
import com.dcrandroid.util.DcrConstants;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.TransactionsResponse.TransactionItem;
import com.dcrandroid.util.Utils;

import java.util.List;

import mobilewallet.Mobilewallet;

/**
 * Created by Macsleven on 01/01/2018.
 */

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.MyViewHolder> {
    private List<TransactionItem> historyList;
    private LayoutInflater layoutInflater;
    private PreferenceUtil util;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView Amount;
        private TextView txType;
        private TextView status;
        private TextView minus;
        private View view;
        public MyViewHolder(View view) {
            super(view);
            Amount = view.findViewById(R.id.history_amount_transferred);
            txType = view.findViewById(R.id.history_snd_rcv);
            status = view.findViewById(R.id.history_tx_status);
            minus = view.findViewById(R.id.history_minus);
            this.view = view;
        }
    }

    public TransactionAdapter(List<TransactionItem> historyListList , LayoutInflater inflater) {
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
        if(position > historyList.size() - 1){
            return;
        }
        TransactionItem history = historyList.get(position);

        int confirmations = DcrConstants.getInstance().wallet.getBestBlock() - history.getHeight();
        confirmations += 1;
        if(history.getHeight() == -1){
            //No included in block chain, therefore transaction is pending
            holder.status.setTextColor(Color.parseColor("#3d659c"));
            holder.status.setText("pending");
        }else{
            if(util.getBoolean(Constants.SPEND_UNCONFIRMED_FUNDS) || confirmations > 1){
                holder.status.setTextColor(Color.parseColor("#55bb97"));
                holder.status.setText("confirmed");
            }else{
                holder.status.setTextColor(Color.parseColor("#3d659c"));
                holder.status.setText("pending");
            }
        }

        if(history.animate) {
            Animation blinkAnim = AnimationUtils.loadAnimation(holder.view.getContext(), R.anim.anim_blink);
            holder.view.setAnimation(blinkAnim);
        }

        holder.Amount.setText(CoinFormat.Companion.format(Utils.removeTrailingZeros(Mobilewallet.amountCoin(history.getAmount())) + Constants.NBSP + layoutInflater.getContext().getString(R.string.dcr)));
        holder.txType.setText("");

        if(history.getDirection() == 0){
            holder.minus.setVisibility(View.VISIBLE);
            holder.txType.setBackgroundResource(R.drawable.ic_send);
        }else if(history.getDirection() == 1) {
            holder.minus.setVisibility(View.INVISIBLE);
            holder.txType.setBackgroundResource(R.drawable.ic_receive);
        }else if(history.getDirection() == 2){
            holder.minus.setVisibility(View.INVISIBLE);
            holder.txType.setBackgroundResource(R.drawable.ic_tx_transferred);
        }

        if (history.type.equalsIgnoreCase("vote")) {
            holder.Amount.setText("Vote");
        }

    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }


}
