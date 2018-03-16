package com.dcrandroid.activities;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.dcrandroid.R;
import com.dcrandroid.data.Constants;
import com.dcrandroid.view.CurrencyTextView;

import java.text.NumberFormat;

/**
 * Created by Macsleven on 28/12/2017.
 *
 */

public class AccountDetailsActivity extends BaseActivity {

    String accountNameTemp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getIntent().getStringExtra(Constants.EXTRA_ACCOUNT_NAME));
        accountNameTemp = getIntent().getStringExtra(Constants.EXTRA_ACCOUNT_NAME);
        setContentView(R.layout.account_details_view);

        Toolbar toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        CurrencyTextView spendable = findViewById(R.id.acc_dts_spendable);
        CurrencyTextView total = findViewById(R.id.acc_dts_total);
        CurrencyTextView immatureRewards = findViewById(R.id.acc_dts_immature_reward);
        CurrencyTextView lockedByTickets = findViewById(R.id.acc_dts_locked_bt_tcks);
        CurrencyTextView votingAuthority = findViewById(R.id.acc_dts_voting_auth);
        CurrencyTextView immatureStakeGeneration = findViewById(R.id.acc_dts_immature_stake_gen);
        TextView accountNumber = findViewById(R.id.acc_dts_acc_number);
        TextView hDPath = findViewById(R.id.acc_dts_hd_path);
        TextView keys = findViewById(R.id.acc_dts_keys);

        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(8);
        spendable.formatAndSetText(nf.format(getIntent().getFloatExtra(Constants.EXTRA_BALANCE_SPENDABLE, 0F)));
        total.formatAndSetText(nf.format(getIntent().getFloatExtra(Constants.EXTRA_BALANCE_TOTAL, 0F)));
        immatureRewards.formatAndSetText(nf.format(getIntent().getFloatExtra(Constants.EXTRA_BALANCE_IMMATURE_REWARDS, 0F)));
        lockedByTickets.formatAndSetText(nf.format(getIntent().getFloatExtra(Constants.EXTRA_BALANCE_LOCKED_BY_TICKETS, 0F)));
        votingAuthority.formatAndSetText(nf.format(getIntent().getFloatExtra(Constants.EXTRA_BALANCE_VOTING_AUTHORITY, 0F)));
        immatureStakeGeneration.formatAndSetText(nf.format(getIntent().getFloatExtra(Constants.EXTRA_BALANCE_IMMATURE_STAKE_GEN, 0F)));
        accountNumber.setText(String.valueOf(getIntent().getIntExtra(Constants.EXTRA_ACCOUNT_NUMBER, 0)));
        hDPath.setText(getIntent().getStringExtra(Constants.EXTRA_HD_PATH));
        keys.setText(getIntent().getStringExtra(Constants.EXTRA_KEYS));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public void showInputAccountNameDialog(final String accountNam,final String messageInfo,final String messagehint) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.input_passphrase_box, null);
        dialogBuilder.setCancelable(false);
        dialogBuilder.setView(dialogView);

        final EditText accountName = dialogView.findViewById(R.id.passphrase_input);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.main, menu);
        super.onCreateOptionsMenu(menu);
//        MenuItem menuOpen = menu.findItem(R.id.action_edit_name);
//        menuOpen.setVisible(true);
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_edit_name) {
            showInputAccountNameDialog(accountNameTemp,getString(R.string.edit_account_name_msg_info),getString(R.string.account_name));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}