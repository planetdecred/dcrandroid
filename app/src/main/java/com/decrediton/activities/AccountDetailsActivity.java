package com.decrediton.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.decrediton.R;

/**
 * Created by Macsleven on 28/12/2017.
 */

public class AccountDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getIntent().getStringExtra("AccountName"));
        setContentView(R.layout.account_details_view);

        TextView spendable = findViewById(R.id.acc_dts_spendable);
        TextView total = findViewById(R.id.acc_dts_total);
        TextView immatureRewards = findViewById(R.id.acc_dts_immature_reward);
        TextView lockedByTickets = findViewById(R.id.acc_dts_locked_bt_tcks);
        TextView votingAuthority = findViewById(R.id.acc_dts_voting_auth);
        TextView immatureStakeGeneration = findViewById(R.id.acc_dts_immature_stake_gen);
        TextView accountNumber = findViewById(R.id.acc_dts_acc_number);
        TextView hDPath = findViewById(R.id.acc_dts_hd_path);
        TextView keys = findViewById(R.id.acc_dts_keys);

        spendable.setText(getIntent().getStringExtra("Spendable"));
        total.setText(getIntent().getStringExtra("total"));
        immatureRewards.setText(getIntent().getStringExtra("ImmatureReward"));
        lockedByTickets.setText(getIntent().getStringExtra("LockedByTickets"));
        votingAuthority.setText(getIntent().getStringExtra("VotingAuthority"));
        immatureStakeGeneration.setText(getIntent().getStringExtra("ImmatureStakeGen"));
        accountNumber.setText(getIntent().getStringExtra("AccountNumber"));
        hDPath.setText(getIntent().getStringExtra("HDPath"));
        keys.setText(getIntent().getStringExtra("Keys"));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}