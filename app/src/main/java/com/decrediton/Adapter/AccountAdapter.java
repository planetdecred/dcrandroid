package com.decrediton.Adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.decrediton.data.Account;
import com.decrediton.data.Seed;
import com.decrediton.R;
import com.decrediton.Util.RecyclerTouchListener;

import java.util.List;

/**
 * Created by Macsleven on 28/12/2017.
 */

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.MyViewHolder> {

    private List<Account> accountList;
    private LayoutInflater layoutInflater;
    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView accountName;
        public TextView total;
        public TextView spendable;


        public MyViewHolder(View view) {
            super(view);
            accountName = view.findViewById(R.id.account_row_name);
            total = view.findViewById(R.id.account_row_total);
            spendable = view.findViewById(R.id.account_row_spendable);

        }
    }
    public AccountAdapter(List<Account> accountListList ,LayoutInflater inflater) {
        this.accountList = accountListList;
       this.layoutInflater = inflater;

    }



    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView =layoutInflater.inflate(R.layout.account_list_row, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {

        Account account = accountList.get(position);
        holder.accountName.setText(account.getAccountName()+" DCR");
        holder.spendable.setText("Spendable "+account.getSpendable()+" DCR");
        holder.total.setText(account.getTotal());
    }

    @Override
    public int getItemCount() {
      return  accountList.size();
    }
}
