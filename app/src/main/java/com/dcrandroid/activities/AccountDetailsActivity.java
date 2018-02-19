package com.dcrandroid.activities;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.dcrandroid.R;
import com.dcrandroid.view.CurrencyTextView;

import java.util.Locale;

/**
 * Created by Macsleven on 28/12/2017.
 */

public class AccountDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       final String accountNameTemp =getIntent().getStringExtra("AccountName");
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
        Button editName= findViewById(R.id.edit_acc_name_btn);

        spendable.formatAndSetText(getIntent().getStringExtra("Spendable"));
        total.formatAndSetText(getIntent().getStringExtra("total"));
        immatureRewards.formatAndSetText(getIntent().getStringExtra("ImmatureReward"));
        lockedByTickets.formatAndSetText(getIntent().getStringExtra("LockedByTickets"));
        votingAuthority.formatAndSetText(getIntent().getStringExtra("VotingAuthority"));
        immatureStakeGeneration.formatAndSetText(getIntent().getStringExtra("ImmatureStakeGen"));
        accountNumber.setText(getIntent().getStringExtra("AccountNumber"));
        hDPath.setText(getIntent().getStringExtra("HDPath"));
        keys.setText(getIntent().getStringExtra("Keys"));
        editName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInputAccountNameDialog(accountNameTemp,getString(R.string.edit_account_name_msg_info),getString(R.string.account_name));
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public void showInputAccountNameDialog(final String accountNam,final String messageInfo,final String messagehint) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getApplicationContext());
        LayoutInflater inflater = getParent().getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.input_passphrase_box, null);
        dialogBuilder.setCancelable(false);
        dialogBuilder.setView(dialogView);

        final EditText accountName = (EditText) dialogView.findViewById(R.id.passphrase_input);
        accountName.setText(accountNam);
        accountName.setHint(messagehint);

        dialogBuilder.setMessage(messageInfo);
        dialogBuilder.setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if(accountName.getText().toString().trim().equals("")){
                    Toast.makeText(getApplicationContext(), "Name cannot be empty",Toast.LENGTH_LONG).show();return;
                }
                try{
                    String pass = accountName.getText().toString();
                }
                catch (Exception e){
                    Toast.makeText(getApplicationContext(), "invalid Name",Toast.LENGTH_LONG).show();
                }

            }
        });

        dialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialogBuilder.setCancelable(true);
            }
        });
        AlertDialog b = dialogBuilder.create();
        b.show();
        b.getButton(b.BUTTON_POSITIVE).setTextColor(Color.BLUE);
    }
}