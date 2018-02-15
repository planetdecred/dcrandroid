package com.dcrandroid.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.dcrandroid.R;
import com.dcrandroid.view.CurrencyTextView;

/**
 * Created by Macsleven on 28/12/2017.
 */

public class AccountDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getIntent().getStringExtra("AccountName"));
        setContentView(R.layout.account_details_view);

        CurrencyTextView spendable = findViewById(R.id.acc_dts_spendable);
        CurrencyTextView total = findViewById(R.id.acc_dts_total);
        CurrencyTextView immatureRewards = findViewById(R.id.acc_dts_immature_reward);
        CurrencyTextView lockedByTickets = findViewById(R.id.acc_dts_locked_bt_tcks);
        CurrencyTextView votingAuthority = findViewById(R.id.acc_dts_voting_auth);
        CurrencyTextView immatureStakeGeneration = findViewById(R.id.acc_dts_immature_stake_gen);
        TextView accountNumber = findViewById(R.id.acc_dts_acc_number);
        TextView hDPath = findViewById(R.id.acc_dts_hd_path);
        TextView keys = findViewById(R.id.acc_dts_keys);

        spendable.formatAndSetText(getIntent().getStringExtra("Spendable"));
        total.formatAndSetText(getIntent().getStringExtra("total"));
        immatureRewards.formatAndSetText(getIntent().getStringExtra("ImmatureReward"));
        lockedByTickets.formatAndSetText(getIntent().getStringExtra("LockedByTickets"));
        votingAuthority.formatAndSetText(getIntent().getStringExtra("VotingAuthority"));
        immatureStakeGeneration.formatAndSetText(getIntent().getStringExtra("ImmatureStakeGen"));
        accountNumber.setText(getIntent().getStringExtra("AccountNumber"));
        hDPath.setText(getIntent().getStringExtra("HDPath"));
        keys.setText(getIntent().getStringExtra("Keys"));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}