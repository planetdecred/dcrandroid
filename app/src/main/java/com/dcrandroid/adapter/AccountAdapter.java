package com.dcrandroid.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dcrandroid.data.Account;
import com.dcrandroid.R;
import com.dcrandroid.data.Balance;
import com.dcrandroid.data.Constants;
import com.dcrandroid.util.CoinFormat;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.Utils;

import java.util.List;
import java.util.Locale;

/**
 * Created by Macsleven on 28/12/2017.
 */

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.MyViewHolder> {

    private List<Account> accountList;
    private LayoutInflater layoutInflater;
    private PreferenceUtil preferenceUtil;

    class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView accountName, total, spendable, labelImmatureRewards, immatureRewards, labelLockedByTickets,
                lockedByTickets, labelVotingAuthority, votingAuthority, labelImmatureStakeGeneration, immatureStakeGeneration,
                accountNumber, hdPath, keys;
        private SwitchCompat hideWallet, defaultWallet;

        private LinearLayout detailsLayout, icon, arrowRight;
        private View view;

        private MyViewHolder(View view) {
            super(view);
            this.view = view;

            // TextViews
            accountName = view.findViewById(R.id.account_name);
            total = view.findViewById(R.id.total_balance);
            spendable = view.findViewById(R.id.spendable_balance);
            labelImmatureRewards = view.findViewById(R.id.tv_immature_rewards_label);
            immatureRewards = view.findViewById(R.id.tv_immature_rewards);
            labelLockedByTickets = view.findViewById(R.id.tv_locked_by_tickets_label);
            lockedByTickets = view.findViewById(R.id.tv_locked_by_tickets);
            labelVotingAuthority = view.findViewById(R.id.tv_voting_authority_label);
            votingAuthority = view.findViewById(R.id.tv_voting_authority);
            labelImmatureStakeGeneration = view.findViewById(R.id.tv_immature_stake_generation_label);
            immatureStakeGeneration = view.findViewById(R.id.tv_immature_stake_generation);
            accountNumber = view.findViewById(R.id.tv_account_number);
            hdPath = view.findViewById(R.id.tv_hd_path);
            keys = view.findViewById(R.id.tv_keys);

            // Switch
            hideWallet = view.findViewById(R.id.switch_hide_wallet);
            defaultWallet = view.findViewById(R.id.switch_default_wallet);

            // LinearLayouts
            detailsLayout = view.findViewById(R.id.layout_account_details);
            icon = view.findViewById(R.id.icon);
            arrowRight = view.findViewById(R.id.arrow_right);
        }
    }

    public AccountAdapter(List<Account> accountListList ,LayoutInflater inflater, Context context) {
        this.accountList = accountListList;
        this.layoutInflater = inflater;
        preferenceUtil = new PreferenceUtil(context);
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = layoutInflater.inflate(R.layout.account_list_row, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull final MyViewHolder holder, int position) {
        final Account account = accountList.get(position);
        Balance balance = account.getBalance();
        boolean hidden = preferenceUtil.getBoolean(Constants.HIDE_WALLET + account.getAccountNumber(), false);
        if(hidden){
            holder.accountName.setText(
                    String.format(Locale.getDefault(), "%s (hidden)", account.getAccountName())
            );
            holder.view.setBackgroundColor(Color.parseColor("#F4F6F6"));
            holder.icon.setBackgroundResource(R.drawable.accounts_disabled);
            holder.arrowRight.setBackgroundResource(R.drawable.arrow_right_disabled);
            holder.spendable.setTextColor(Color.parseColor("#C4CBD2"));
            holder.total.setTextColor(Color.parseColor("#C4CBD2"));
            holder.accountName.setTextColor(Color.parseColor("#C4CBD2"));
        }else{
            holder.accountName.setText(account.getAccountName());

            holder.view.setBackgroundColor(Color.parseColor("#FFFFFF"));
            holder.icon.setBackgroundResource(R.drawable.accounts_activated);
            holder.arrowRight.setBackgroundResource(R.drawable.arrow_right_enabled);
            holder.spendable.setTextColor(Color.parseColor("#2DD8A3"));
            holder.total.setTextColor(Color.parseColor("#091440"));
            holder.accountName.setTextColor(Color.parseColor("#091440"));

            if(balance.getSpendable() == 0){
                holder.spendable.setTextColor(Color.parseColor("#8997A5"));
            }
        }

        // Balance
        holder.spendable.setText(CoinFormat.Companion.format(Utils.formatDecred(balance.getSpendable())));
        holder.total.setText(CoinFormat.Companion.format(balance.getTotal()));
        holder.immatureRewards.setText(CoinFormat.Companion.format(balance.getImmatureReward()));
        holder.lockedByTickets.setText(CoinFormat.Companion.format(balance.getLockedByTickets()));
        holder.votingAuthority.setText(CoinFormat.Companion.format(balance.getVotingAuthority()));
        holder.immatureStakeGeneration.setText(CoinFormat.Companion.format(balance.getImmatureStakeGeneration()));

        // Account Number
        holder.accountNumber.setText(
                String.format(Locale.getDefault(), "%d", account.getAccountNumber())
        );

        // HD Path
        // TODO: MAINNET
        holder.hdPath.setText(account.getHDPath());

        holder.keys.setText(
                String.format(Locale.getDefault(), "%d external, %d internal, %d imported", account.getExternalKeyCount(), account.getInternalKeyCount(), account.getImportedKeyCount())
        );

        int emptyColor = Color.parseColor("#C4CBD2");

        if(balance.getImmatureReward() == 0){
            holder.immatureRewards.setTextColor(emptyColor);
            holder.labelImmatureRewards.setTextColor(emptyColor);
        }

        if(balance.getLockedByTickets() == 0){
            holder.lockedByTickets.setTextColor(emptyColor);
            holder.labelLockedByTickets.setTextColor(emptyColor);
        }

        if(balance.getVotingAuthority() == 0){
            holder.votingAuthority.setTextColor(emptyColor);
            holder.labelVotingAuthority.setTextColor(emptyColor);
        }

        if(balance.getImmatureStakeGeneration() == 0){
            holder.immatureStakeGeneration.setTextColor(emptyColor);
            holder.labelImmatureStakeGeneration.setTextColor(emptyColor);
        }

        if(account.getAccountNumber() == Integer.MAX_VALUE){
            // Imported Account
            holder.hideWallet.setChecked(false);
            holder.defaultWallet.setChecked(false);
            holder.hideWallet.setEnabled(false);
            holder.defaultWallet.setEnabled(false);

        }else{

            holder.hideWallet.setEnabled(true);

            holder.hideWallet.setChecked(hidden);

            if(preferenceUtil.getInt(Constants.DEFAULT_ACCOUNT, 0) == account.getAccountNumber()){
                holder.defaultWallet.setChecked(true);
                holder.defaultWallet.setEnabled(false); // Default account should not be unchecked. There has to be a default account.
            }else{
                holder.defaultWallet.setChecked(false);
                holder.defaultWallet.setEnabled(true);
            }
        }


        // Using OnClickListener because OnCheckedChangeListener gets called when calling setChecked
        holder.defaultWallet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // This listener should no longer be called
                holder.defaultWallet.setOnClickListener(null);
                holder.hideWallet.setOnClickListener(null);

                holder.defaultWallet.setEnabled(false);
                holder.hideWallet.setEnabled(false);

                preferenceUtil.setInt(Constants.DEFAULT_ACCOUNT, account.getAccountNumber());

                notifyDataSetChanged();
            }
        });

        holder.hideWallet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // This listener should no longer be called
                holder.defaultWallet.setOnClickListener(null);
                holder.hideWallet.setOnClickListener(null);

                holder.defaultWallet.setEnabled(false);
                holder.hideWallet.setEnabled(false);

                preferenceUtil.setBoolean(Constants.HIDE_WALLET + account.getAccountNumber(), holder.hideWallet.isChecked());

                notifyDataSetChanged();
            }
        });

        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(holder.detailsLayout.getVisibility() == View.GONE){
                    holder.detailsLayout.setVisibility(View.VISIBLE);
                }else{
                    holder.detailsLayout.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
      return accountList.size();
    }
}
