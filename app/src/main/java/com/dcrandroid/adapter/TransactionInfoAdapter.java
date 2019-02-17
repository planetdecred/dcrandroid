/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.dcrandroid.R;
import com.dcrandroid.util.Utils;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class TransactionInfoAdapter extends ArrayAdapter<TransactionInfoAdapter.TransactionInfoItem> {

    private Context mContext;
    private List<TransactionInfoItem> items;

    public TransactionInfoAdapter(@NonNull Context context, ArrayList<TransactionInfoItem> list) {
        super(context, 0, list);
        mContext = context;
        items = list;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if (listItem == null)
            listItem = LayoutInflater.from(mContext).inflate(R.layout.list_item_adapter, parent, false);

        TransactionInfoItem transactionInfoItem = items.get(position);

        TextView tvAmount = listItem.findViewById(R.id.tvAmount);
        tvAmount.setText(transactionInfoItem.getAmount());

        final TextView tvInfo = listItem.findViewById(R.id.tvInfo);
        if (transactionInfoItem.getInfo() != null && !transactionInfoItem.getInfo().trim().equals("")) {
            tvInfo.setText(transactionInfoItem.getInfo());
            tvInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Utils.copyToClipboard(mContext, tvInfo.getText().toString(), mContext.getString(R.string.copied_to_clipboard));
                }
            });
        } else {
            tvInfo.setVisibility(View.GONE);
        }
        return listItem;
    }

    public static class TransactionInfoItem {
        private String amount;
        private String info;

        public TransactionInfoItem(String amount, String info) {
            this.amount = amount;
            this.info = info;
        }

        public String getAmount() {
            return amount;
        }

        public void setAmount(String amount) {
            this.amount = amount;
        }

        public String getInfo() {
            return info;
        }

        public void setInfo(String info) {
            this.info = info;
        }
    }

}
