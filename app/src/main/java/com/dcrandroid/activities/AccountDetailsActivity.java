package com.dcrandroid.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.dcrandroid.R;
import com.dcrandroid.data.Constants;
import com.dcrandroid.util.CoinFormat;
import com.dcrandroid.util.DcrConstants;
import com.dcrandroid.util.Utils;

/**
 * Created by Macsleven on 28/12/2017.
 */

public class AccountDetailsActivity extends AppCompatActivity {

    private boolean accountNameChanged = false;
    private String accountName;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        accountName = getIntent().getStringExtra(Constants.ACCOUNT_NAME);
        setTitle(accountName);

        setContentView(R.layout.account_details_view);

        TextView spendable = findViewById(R.id.acc_dts_spendable);
        spendable.setText(CoinFormat.Companion.format(Utils.formatDecred(getIntent().getLongExtra(Constants.EXTRA_BALANCE_SPENDABLE, 0))  +" "+ getString(R.string.dcr)));

        TextView total = findViewById(R.id.acc_dts_total);
        total.setText(CoinFormat.Companion.format(Utils.formatDecred(getIntent().getLongExtra(Constants.EXTRA_BALANCE_TOTAL, 0))  +" "+ getString(R.string.dcr)));

        TextView immatureRewards = findViewById(R.id.acc_dts_immature_reward);
        immatureRewards.setText(CoinFormat.Companion.format(Utils.formatDecred(getIntent().getLongExtra(Constants.EXTRA_BALANCE_IMMATURE_REWARDS, 0))  +" "+ getString(R.string.dcr)));

        TextView lockedByTickets = findViewById(R.id.acc_dts_locked_bt_tcks);
        lockedByTickets.setText(CoinFormat.Companion.format(Utils.formatDecred(getIntent().getLongExtra(Constants.EXTRA_BALANCE_LOCKED_BY_TICKETS, 0))  +" "+ getString(R.string.dcr)));

        TextView votingAuthority = findViewById(R.id.acc_dts_voting_auth);
        votingAuthority.setText(CoinFormat.Companion.format(Utils.formatDecred(getIntent().getLongExtra(Constants.EXTRA_BALANCE_VOTING_AUTHORITY, 0))  +" "+ getString(R.string.dcr)));

        TextView immatureStakeGeneration = findViewById(R.id.acc_dts_immature_stake_gen);
        immatureStakeGeneration.setText(CoinFormat.Companion.format(Utils.formatDecred(getIntent().getLongExtra(Constants.EXTRA_BALANCE_IMMATURE_STAKE_GEN, 0))  +" "+ getString(R.string.dcr)));

        TextView accountNumber = findViewById(R.id.acc_dts_acc_number);
        accountNumber.setText(String.valueOf(getIntent().getIntExtra(Constants.ACCOUNT_NUMBER, 0)));

        TextView hDPath = findViewById(R.id.acc_dts_hd_path);
        hDPath.setText(getIntent().getStringExtra(Constants.EXTRA_HD_PATH));

        TextView keys = findViewById(R.id.acc_dts_keys);
        keys.setText(getIntent().getStringExtra(Constants.KEYS));
    }

    public void showInputAccountNameDialog() {
        LayoutInflater inflater = getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.change_acount_name, null);

        final EditText newName = dialogView.findViewById(R.id.new_account_name);
        newName.setInputType(EditorInfo.TYPE_CLASS_TEXT);
        newName.setHint(getString(R.string.account_name));

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        dialogBuilder.setCancelable(false);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setTitle(getString(R.string.rename_account));

        dialogBuilder.setPositiveButton(R.string.rename, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if(newName.getText().toString().trim().equals("")){
                    Toast.makeText(getApplicationContext(), "Name cannot be empty",Toast.LENGTH_LONG).show();
                    return;
                }

                try{
                    accountName = newName.getText().toString();
                    DcrConstants.getInstance().wallet.renameAccount(getIntent().getIntExtra(Constants.ACCOUNT_NUMBER, -1), accountName);
                    setTitle(accountName);
                    accountNameChanged = true;
                }catch (Exception e){
                    Toast.makeText(getApplicationContext(), "Error: "+e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });

        dialogBuilder.setNegativeButton(R.string.cancel, null);

        AlertDialog b = dialogBuilder.create();
        b.show();
        b.getButton(b.BUTTON_POSITIVE).setTextColor(Color.BLUE);
    }

    @Override
    public void onBackPressed() {
        if(accountNameChanged){
            Intent intent = new Intent();
            intent.putExtra(Constants.ACCOUNT_NAME, accountName);
            intent.putExtra(Constants.ACCOUNT_NUMBER, getIntent().getIntExtra(Constants.ACCOUNT_NUMBER, -1));
            setResult(RESULT_OK, intent);
            finish();
        }else
            super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem menuOpen = menu.findItem(R.id.action_edit_name);
        menuOpen.setVisible(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_edit_name) {
            showInputAccountNameDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}