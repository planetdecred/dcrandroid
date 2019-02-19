/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.adapter;

import android.content.Context;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.dcrandroid.BuildConfig;
import com.dcrandroid.R;
import com.dcrandroid.data.Constants;
import com.dcrandroid.data.Transaction;
import com.dcrandroid.util.CoinFormat;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.Utils;
import com.dcrandroid.util.WalletData;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by Macsleven on 01/01/2018.
 */

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.MyViewHolder> {
    private List<Transaction> historyList;
    private LayoutInflater layoutInflater;
    private PreferenceUtil util;
    private Context context;

    public TransactionAdapter(List<Transaction> historyListList, Context context) {
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
        Transaction history = historyList.get(position);

        int confirmedTextColor = context.getResources().getColor(R.color.greenConfirmedTextColor);
        int pendingTextColor = context.getResources().getColor(R.color.bluePendingTextColor);
        int grayTextColor = context.getResources().getColor(R.color.grayTextColor);

        int confirmations = 0;
        if (history.getHeight() != -1) {
            confirmations = WalletData.getInstance().wallet.getBestBlock() - history.getHeight();
            confirmations += 1;
        }

        if (history.getHeight() == -1) {
            //No included in block chain, therefore transaction is pending
            holder.status.setTextColor(pendingTextColor);
            holder.status.setText(context.getString(R.string.pending));
        } else {
            if (util.getBoolean(Constants.SPEND_UNCONFIRMED_FUNDS) || confirmations > 1) {
                holder.status.setTextColor(confirmedTextColor);
                holder.status.setText(context.getString(R.string.confirmed));
            } else {
                holder.status.setTextColor(pendingTextColor);
                holder.status.setText(context.getString(R.string.pending));
            }
        }

        if (history.getAnimate()) {
            Animation blinkAnim = AnimationUtils.loadAnimation(holder.view.getContext(), R.anim.anim_blink);
            holder.view.setAnimation(blinkAnim);
            history.setAnimate(false);
        }

        Calendar calendar = new GregorianCalendar(TimeZone.getDefault());
        calendar.setTimeInMillis(history.getTimestamp() * 1000);
        SimpleDateFormat sdf = new SimpleDateFormat(" dd yyyy, hh:mma", Locale.getDefault());

        holder.tvDateOfTransaction.setText(calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) + sdf.format(calendar.getTime()).toLowerCase());

        holder.txType.setText("");

        int requiredConfs = util.getBoolean(Constants.SPEND_UNCONFIRMED_FUNDS) ? 0 : 2;

        if (history.getType().equals(Constants.REGULAR)) {
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
        } else if (history.getType().equals(Constants.TICKET_PURCHASE)) {
            holder.minus.setVisibility(View.INVISIBLE);
            holder.Amount.setText(R.string.ticket);
            holder.txType.setBackgroundResource(R.drawable.immature_ticket);

            Locale currentLocale = context.getResources().getConfiguration().locale;

            if (confirmations < requiredConfs) {
                holder.status.setText(context.getString(R.string.pending));
                holder.status.setTextColor(pendingTextColor);
            } else if (confirmations >= requiredConfs && confirmations < BuildConfig.TicketMaturity) {

                if (currentLocale.equals(Locale.ENGLISH)) {
                    SpannableString confirmedImmature = new SpannableString(context.getString(R.string.confirmed_immature));

                    // Confirmed text color
                    confirmedImmature.setSpan(new ForegroundColorSpan(confirmedTextColor), 0, 9, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                    // Slash text color
                    confirmedImmature.setSpan(new ForegroundColorSpan(grayTextColor), 10, 11, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                    // Immature text color
                    confirmedImmature.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.orangeTextColor)),
                            12, confirmedImmature.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                    holder.status.setText(confirmedImmature);
                }else{
                    holder.status.setText(R.string.confirmed_immature);
                }

            } else if (confirmations > 16) {

                if (currentLocale.equals(Locale.ENGLISH)) {
                    SpannableString confirmedLive = new SpannableString(context.getString(R.string.confirmed_live));

                    // Confirmed text color
                    confirmedLive.setSpan(new ForegroundColorSpan(confirmedTextColor), 0, 9, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                    // Slash text color
                    confirmedLive.setSpan(new ForegroundColorSpan(grayTextColor), 10, 11, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                    // Live text color
                    confirmedLive.setSpan(new ForegroundColorSpan(confirmedTextColor),
                            12, confirmedLive.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                    holder.status.setText(confirmedLive);
                }else{
                    holder.status.setText(R.string.confirmed_live);
                }

                holder.txType.setBackgroundResource(R.drawable.live_ticket);
            }
        } else if (history.getType().equals(Constants.VOTE)) {
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
