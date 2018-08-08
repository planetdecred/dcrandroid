package com.dcrandroid.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dcrandroid.data.Account;
import com.dcrandroid.R;
import com.dcrandroid.data.Balance;
import com.dcrandroid.util.Utils;
import com.dcrandroid.view.CurrencyTextView;

import java.util.List;

/**
 * Created by Macsleven on 28/12/2017.
 */

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.MyViewHolder> {

    private List<Account> accountList;
    private LayoutInflater layoutInflater;
    private Context context;
    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView accountName;
        public CurrencyTextView total;
        public CurrencyTextView spendable;

        public MyViewHolder(View view) {
            super(view);
            accountName = view.findViewById(R.id.account_row_name);
            total = view.findViewById(R.id.account_row_total);
            spendable = view.findViewById(R.id.account_row_spendable);
        }
    }
    public AccountAdapter(List<Account> accountListList ,LayoutInflater inflater, Context context) {
        this.accountList = accountListList;
        this.layoutInflater = inflater;
        this.context = context;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView =layoutInflater.inflate(R.layout.account_list_row, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Account account = accountList.get(position);
        Balance balance = account.getBalance();
        holder.accountName.setText(account.getAccountName());
        holder.spendable.formatAndSetText(Utils.formatDecred(balance.getSpendable()) +" "+ context.getString(R.string.dcr));
        holder.total.formatAndSetText(Utils.formatDecred(balance.getTotal()) +" "+ context.getString(R.string.dcr));
    }

    @Override
    public int getItemCount() {
      return  accountList.size();
    }
}
