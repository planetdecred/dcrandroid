package com.dcrandroid.adapter;

import android.content.Context;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by Macsleven on 01/01/2018.
 */

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.MyViewHolder> {
    private List<TransactionItem> historyList;
    private LayoutInflater layoutInflater;
    private PreferenceUtil util;
    private Context context;


    public TransactionAdapter(List<TransactionItem> historyListList, Context context) {
        this.historyList = historyListList;
        this.context = context;
        this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.util = new PreferenceUtil(context);
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = layoutInflater.inflate(R.layout.history_list_row, parent, false);
        return new TransactionAdapter.MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        if (position > historyList.size() - 1) {
            return;
        }
        TransactionItem history = historyList.get(position);

        int confirmations = 0;
        if (history.getHeight() != -1) {
            confirmations = DcrConstants.getInstance().wallet.getBestBlock() - history.getHeight();
            confirmations += 1;
        }
        if (history.getHeight() == -1) {
            //No included in block chain, therefore transaction is pending
            holder.status.setTextColor(context.getResources().getColor(R.color.bluePendingTextColor));
            holder.status.setText(context.getString(R.string.pending));
        } else {
            if (util.getBoolean(Constants.SPEND_UNCONFIRMED_FUNDS) || confirmations > 1) {
                holder.status.setTextColor(context.getResources().getColor(R.color.greenConfirmedTextColor));
                holder.status.setText(context.getString(R.string.confirmed));
            } else {
                holder.status.setTextColor(context.getResources().getColor(R.color.bluePendingTextColor));
                holder.status.setText(context.getString(R.string.pending));
            }
        }

        if (history.animate) {
            Animation blinkAnim = AnimationUtils.loadAnimation(holder.view.getContext(), R.anim.anim_blink);
            holder.view.setAnimation(blinkAnim);
            history.animate = false;
        }

        Calendar calendar = new GregorianCalendar(TimeZone.getDefault());
        calendar.setTimeInMillis(history.timestamp * 1000);
        SimpleDateFormat sdf = new SimpleDateFormat(" dd yyyy, hh:mma", Locale.getDefault());

        holder.tvDateOfTransaction.setText(calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) + sdf.format(calendar.getTime()).toLowerCase());

        holder.txType.setText("");

        int requiredConfs = util.getBoolean(Constants.SPEND_UNCONFIRMED_FUNDS) ? 0 : 2;

        if (history.type.equals(Constants.REGULAR)) {
            String strAmount = Utils.formatDecredWithComma(history.getAmount());

            holder.Amount.setText(CoinFormat.Companion.format(strAmount + Constants.NBSP + layoutInflater.getContext().getString(R.string.dcr)));
            if (history.getDirection() == 0) {
                holder.minus.setVisibility(View.VISIBLE);
                holder.txType.setBackgroundResource(R.drawable.ic_send);
            } else if (history.getDirection() == 1) {
                holder.minus.setVisibility(View.INVISIBLE);
                holder.txType.setBackgroundResource(R.drawable.ic_receive);
            } else if (history.getDirection() == 2) {
                holder.minus.setVisibility(View.INVISIBLE);
                holder.txType.setBackgroundResource(R.drawable.ic_tx_transferred);
            }
        } else if (history.type.equals(Constants.TICKET_PURCHASE)) {
            holder.minus.setVisibility(View.INVISIBLE);
            holder.Amount.setText(R.string.ticket);
            holder.txType.setBackgroundResource(R.drawable.immature_ticket);
            // TODO: Mainnet
            if (confirmations < requiredConfs) {
                holder.status.setText(context.getString(R.string.pending));
                holder.status.setTextColor(context.getResources().getColor(R.color.bluePendingTextColor));
            } else if (confirmations >= requiredConfs && confirmations < 16) {
                holder.status.setText(R.string.confirmed_immature);
                holder.status.setTextColor(context.getResources().getColor(R.color.orangeTextColor));
            } else if (confirmations > 16) { //TODO: Mainnet
                holder.status.setText(R.string.confirmed_live);
                holder.status.setTextColor(context.getResources().getColor(R.color.greenConfirmedTextColor));
                holder.txType.setBackgroundResource(R.drawable.live_ticket);
            }
        } else if (history.type.equals(Constants.VOTE)) {
            holder.Amount.setText(R.string.vote);
            holder.minus.setVisibility(View.INVISIBLE);
            holder.txType.setBackgroundResource(R.drawable.vote);
        }
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView Amount;
        private TextView txType;
        private TextView status;
        private TextView minus;
        private View view;
        private TextView tvDateOfTransaction;

        public MyViewHolder(View view) {
            super(view);
            Amount = view.findViewById(R.id.history_amount_transferred);
            txType = view.findViewById(R.id.history_snd_rcv);
            status = view.findViewById(R.id.history_tx_status);
            minus = view.findViewById(R.id.history_minus);
            tvDateOfTransaction = view.findViewById(R.id.tvDateOfTransaction);
            this.view = view;
        }
    }


}
