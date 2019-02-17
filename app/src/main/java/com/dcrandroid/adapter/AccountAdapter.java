/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dcrandroid.MainActivity;
import com.dcrandroid.R;
import com.dcrandroid.data.Account;
import com.dcrandroid.data.Balance;
import com.dcrandroid.data.Constants;
import com.dcrandroid.util.CoinFormat;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.Utils;
import com.dcrandroid.util.WalletData;

import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by Macsleven on 28/12/2017.
 */

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.MyViewHolder> {

    private List<Account> accountList;
    private LayoutInflater layoutInflater;
    private PreferenceUtil preferenceUtil;
    private Activity context;

    public AccountAdapter(List<Account> accountListList, Activity context) {
        this.accountList = accountListList;
        this.context = context;
        this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
        if (hidden) {
            holder.accountName.setText(
                    String.format(Locale.getDefault(), "%s (%s)", account.getAccountName(), context.getString(R.string.hidden))
            );
            holder.view.setBackgroundColor(context.getResources().getColor(R.color.colorPrimary));
            holder.icon.setBackgroundResource(R.drawable.account_default);
            holder.arrowRight.setBackgroundResource(R.drawable.arrow_right_disabled);
            holder.spendable.setTextColor(context.getResources().getColor(R.color.lightGray));
            holder.total.setTextColor(context.getResources().getColor(R.color.lightGray));
            holder.accountName.setTextColor(context.getResources().getColor(R.color.lightGray));
        } else {
            holder.accountName.setText(account.getAccountName());

            holder.view.setBackgroundColor(context.getResources().getColor(R.color.white));
            holder.icon.setBackgroundResource(R.drawable.account_activated);
            holder.arrowRight.setBackgroundResource(R.drawable.arrow_right_enabled);
            holder.spendable.setTextColor(context.getResources().getColor(R.color.greenLightTextColor));
            holder.total.setTextColor(context.getResources().getColor(R.color.darkBlueTextColor));
            holder.accountName.setTextColor(context.getResources().getColor(R.color.darkBlueTextColor));

            if (balance.getSpendable() == 0) {
                holder.spendable.setTextColor(context.getResources().getColor(R.color.lightGrayTextColor));
            }
        }

        // Balance
        if (!WalletData.getInstance().syncing) {
            holder.spendable.setText(CoinFormat.Companion.format(Utils.formatDecredWithComma(balance.getSpendable())));
            holder.total.setText(CoinFormat.Companion.format(Utils.formatDecredWithComma(balance.getTotal())));
            holder.immatureRewards.setText(CoinFormat.Companion.format(balance.getImmatureReward()));
            holder.lockedByTickets.setText(CoinFormat.Companion.format(balance.getLockedByTickets()));
            holder.votingAuthority.setText(CoinFormat.Companion.format(balance.getVotingAuthority()));
            holder.immatureStakeGeneration.setText(CoinFormat.Companion.format(balance.getImmatureStakeGeneration()));
            holder.spendableLabel.setVisibility(View.VISIBLE);
            if (holder.syncIndicator.getBackground() != null) {
                AnimationDrawable syncAnimation = (AnimationDrawable) holder.syncIndicator.getBackground();
                syncAnimation.stop();
            }
            holder.syncIndicator.setVisibility(View.GONE);
            holder.total.setVisibility(View.VISIBLE);
        } else {
            holder.spendable.setText("-");
            holder.total.setText("-");
            holder.immatureRewards.setText("-");
            holder.lockedByTickets.setText("-");
            holder.votingAuthority.setText("-");
            holder.immatureStakeGeneration.setText("-");
            holder.spendableLabel.setVisibility(View.GONE);
            holder.syncIndicator.setBackgroundResource(R.drawable.sync_animation);
            holder.total.setVisibility(View.GONE);
            holder.syncIndicator.setVisibility(View.VISIBLE);
            holder.syncIndicator.post(new Runnable() {
                @Override
                public void run() {
                    AnimationDrawable syncAnimation = (AnimationDrawable) holder.syncIndicator.getBackground();
                    syncAnimation.start();
                }
            });
        }

        // Account Number
        holder.accountNumber.setText(
                String.format(Locale.getDefault(), "%d", account.getAccountNumber())
        );

        // HD Path
        holder.hdPath.setText(account.getHDPath());

        holder.keys.setText(
                String.format(Locale.getDefault(), "%d %s, %d %s, %d %s", account.getExternalKeyCount(), context.getString(R.string.external),
                        account.getInternalKeyCount(), context.getString(R.string.internal), account.getImportedKeyCount(), context.getString(R.string.imported))
        );

        int emptyColor = context.getResources().getColor(R.color.lightGray);

        if (balance.getImmatureReward() == 0) {
            holder.immatureRewards.setTextColor(emptyColor);
            holder.labelImmatureRewards.setTextColor(emptyColor);
        }

        if (balance.getLockedByTickets() == 0) {
            holder.lockedByTickets.setTextColor(emptyColor);
            holder.labelLockedByTickets.setTextColor(emptyColor);
        }

        if (balance.getVotingAuthority() == 0) {
            holder.votingAuthority.setTextColor(emptyColor);
            holder.labelVotingAuthority.setTextColor(emptyColor);
        }

        if (balance.getImmatureStakeGeneration() == 0) {
            holder.immatureStakeGeneration.setTextColor(emptyColor);
            holder.labelImmatureStakeGeneration.setTextColor(emptyColor);
        }

        double stakeSum = balance.getImmatureReward() + balance.getLockedByTickets() + balance.getVotingAuthority() + balance.getImmatureStakeGeneration();
        if (stakeSum != 0) {
            holder.staking_data.setVisibility(View.VISIBLE);
        }

        if (account.getAccountNumber() == Integer.MAX_VALUE) {
            // Imported Account
            holder.hideWallet.setChecked(false);
            holder.defaultWallet.setChecked(false);
            holder.hideWallet.setEnabled(false);
            holder.defaultWallet.setEnabled(false);
        } else {
            holder.hideWallet.setEnabled(true);

            holder.hideWallet.setChecked(hidden);

            if (preferenceUtil.getInt(Constants.DEFAULT_ACCOUNT, 0) == account.getAccountNumber()) {
                holder.defaultWallet.setChecked(true);
                holder.defaultWallet.setEnabled(false); // Default account should not be unchecked. There has to be a default account.
                holder.hideWallet.setChecked(false);
                holder.hideWallet.setEnabled(false);
            } else {
                holder.defaultWallet.setChecked(false);
                holder.defaultWallet.setEnabled(true);
            }
        }

        // Using OnClickListener because OnCheckedChangeListener gets called when calling setChecked
        holder.defaultWallet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.hideWallet.setChecked(false);
                preferenceUtil.setBoolean(Constants.HIDE_WALLET + account.getAccountNumber(), holder.hideWallet.isChecked());
                // This listener should no longer be called
                holder.defaultWallet.setOnClickListener(null);
                holder.hideWallet.setOnClickListener(null);

                holder.defaultWallet.setEnabled(false);
                holder.hideWallet.setEnabled(false);

                preferenceUtil.setInt(Constants.DEFAULT_ACCOUNT, account.getAccountNumber());

                if (context instanceof MainActivity) {
                    ((MainActivity) context).displayBalance();
                }

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

                if (context instanceof MainActivity) {
                    ((MainActivity) context).displayBalance();
                }

                notifyDataSetChanged();
            }
        });

        holder.account.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder.detailsLayout.getVisibility() == View.GONE) {
                    holder.arrowRight.setRotation(90);
                    holder.detailsLayout.setVisibility(View.VISIBLE);
                } else {
                    holder.arrowRight.setRotation(0);
                    holder.detailsLayout.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return accountList.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView accountName, total, spendable, labelImmatureRewards, immatureRewards, labelLockedByTickets,
                lockedByTickets, labelVotingAuthority, votingAuthority, labelImmatureStakeGeneration, immatureStakeGeneration,
                accountNumber, hdPath, keys, spendableLabel;
        private SwitchCompat hideWallet, defaultWallet;
        private ImageView syncIndicator;

        private LinearLayout detailsLayout, icon, arrowRight, account, staking_data;
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
            spendableLabel = view.findViewById(R.id.spendable_label);

            // Switches
            hideWallet = view.findViewById(R.id.switch_hide_wallet);
            defaultWallet = view.findViewById(R.id.switch_default_wallet);

            // LinearLayouts
            detailsLayout = view.findViewById(R.id.layout_account_details);
            icon = view.findViewById(R.id.icon);
            arrowRight = view.findViewById(R.id.arrow_right);
            account = view.findViewById(R.id.account);
            staking_data = view.findViewById(R.id.account_staking_data);

            // ImageView
            syncIndicator = view.findViewById(R.id.account_sync_indicator);
        }
    }
}
