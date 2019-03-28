/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dcrandroid.BuildConfig;
import com.dcrandroid.R;
import com.dcrandroid.adapter.TransactionInfoAdapter;
import com.dcrandroid.data.Constants;
import com.dcrandroid.data.Transaction;
import com.dcrandroid.data.Transaction.TransactionInput;
import com.dcrandroid.data.Transaction.TransactionOutput;
import com.dcrandroid.util.CoinFormat;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.TransactionsParser;
import com.dcrandroid.util.Utils;
import com.dcrandroid.util.WalletData;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import androidx.appcompat.app.AppCompatActivity;
import dcrlibwallet.LibWallet;

/**
 * Created by Macsleven on 02/01/2018.
 */

public class TransactionDetailsActivity extends AppCompatActivity {

    private TextView value, date, status, txType, confirmation, transactionFee, tvHash;
    private ListView lvInput, lvOutput;

    private PreferenceUtil util;

    private String rawTx, transactionType, txHash;

    private LibWallet wallet;

    private Calendar calendar;
    private SimpleDateFormat sdf;

    public static void setListViewHeight(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }

        int totalHeight = 0;

        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            float px = 450 * (listView.getResources().getDisplayMetrics().density);
            listItem.measure(View.MeasureSpec.makeMeasureSpec((int) px, View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight;
        listView.setLayoutParams(params);
        listView.requestLayout();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (WalletData.getInstance().wallet == null) {
            Utils.restartApp(this);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS |
                    View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }

        setTitle(getString(R.string.Transaction_details));
        setContentView(R.layout.transaction_details_view);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        util = new PreferenceUtil(this);
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            System.out.println("Extras is null");
            return;
        }

        wallet = WalletData.getInstance().wallet;

        calendar = new GregorianCalendar(TimeZone.getDefault());
        sdf = new SimpleDateFormat(" dd yyyy, hh:mma", Locale.getDefault());

        lvInput = findViewById(R.id.lvInput);
        lvOutput = findViewById(R.id.lvOutput);
        value = findViewById(R.id.tx_dts_value);
        date = findViewById(R.id.tx_date);
        status = findViewById(R.id.tx_dts__status);
        txType = findViewById(R.id.txtype);
        confirmation = findViewById(R.id.tx_dts_confirmation);
        transactionFee = findViewById(R.id.tx_fee);
        tvHash = findViewById(R.id.tx_hash);

        if (extras.getBoolean(Constants.NO_INFO)) {
            getTransaction();
            return;
        }

        transactionType = extras.getString(Constants.TYPE);
        if (transactionType.equals(Constants.TICKET_PURCHASE)) {
            transactionType = getString(R.string.ticket_purchase);
        } else {
            transactionType = transactionType.substring(0, 1).toUpperCase() + transactionType.substring(1).toLowerCase();
        }

        ArrayList<TransactionInput> inputs
                = (ArrayList<TransactionInput>) extras.getSerializable(Constants.INPUTS);
        ArrayList<TransactionOutput> outputs
                = (ArrayList<TransactionOutput>) extras.getSerializable(Constants.OUTPUTS);

        loadInOut(inputs, outputs);

        rawTx = extras.getString(Constants.RAW);

        txHash = extras.getString(Constants.HASH);
        tvHash.setText(txHash);

        value.setText(CoinFormat.Companion.format(Utils.formatDecredWithComma(extras.getLong(Constants.AMOUNT, 0)) + " " + getString(R.string.dcr)));
        transactionFee.setText(CoinFormat.Companion.format(Utils.formatDecredWithComma(extras.getLong(Constants.FEE, 0)) + " " + getString(R.string.dcr)));

        calendar.setTimeInMillis(extras.getLong(Constants.TIMESTAMP) * 1000);

        date.setText(calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) + sdf.format(calendar.getTime()).toLowerCase());

        txType.setText(transactionType);

        int height = extras.getInt(Constants.HEIGHT, 0);
        if (height == -1) {
            //Not included in block chain, therefore transaction is pending
            status.setBackgroundResource(R.drawable.tx_status_pending);
            status.setTextColor(getApplicationContext().getResources().getColor(R.color.bluePendingTextColor));
            status.setText(R.string.pending);
            confirmation.setText(R.string.unconfirmed);
        } else {
            int confirmations = WalletData.getInstance().wallet.getBestBlock() - height;
            confirmations += 1; //+1 confirmation that it exist in a block. best block - height returns 0.
            confirmation.setText(String.valueOf(confirmations));
            if (util.getBoolean(Constants.SPEND_UNCONFIRMED_FUNDS) || confirmations > 1) {
                status.setBackgroundResource(R.drawable.tx_status_confirmed);
                status.setTextColor(getApplicationContext().getResources().getColor(R.color.greenConfirmedTextColor));
                status.setText(R.string.confirmed);
            } else {
                status.setBackgroundResource(R.drawable.tx_status_pending);
                status.setTextColor(getApplicationContext().getResources().getColor(R.color.bluePendingTextColor));
                status.setText(R.string.pending);
            }
        }

        tvHash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.copyToClipboard(TransactionDetailsActivity.this, txHash, getString(R.string.tx_hash_copy));
            }
        });

    }

    private void getTransaction() {
        txHash = getIntent().getStringExtra(Constants.HASH);

        if (txHash == null) {
            return;
        }

        try {
            Transaction transaction = TransactionsParser.parseTransaction(wallet.getTransaction(Utils.getHash(txHash)));

            rawTx = transaction.getRaw();

            tvHash.setText(txHash);
            value.setText(CoinFormat.Companion.format(Utils.formatDecredWithComma(transaction.getAmount()) + " " + getString(R.string.dcr)));
            transactionFee.setText(CoinFormat.Companion.format(Utils.formatDecredWithComma(transaction.getFee()) + " " + getString(R.string.dcr)));

            calendar.setTimeInMillis(transaction.getTimestamp() * 1000);

            date.setText(calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) + sdf.format(calendar.getTime()).toLowerCase());

            transactionType = transaction.getType();
            if (transactionType.equals(Constants.TICKET_PURCHASE)) {
                transactionType = getString(R.string.ticket_purchase);
            } else {
                transactionType = transactionType.substring(0, 1).toUpperCase() + transactionType.substring(1).toLowerCase();
            }

            txType.setText(transactionType);

            int height = transaction.getHeight();
            if (height == -1) {
                //Not included in block chain, therefore transaction is pending
                status.setBackgroundResource(R.drawable.tx_status_pending);
                status.setTextColor(getApplicationContext().getResources().getColor(R.color.bluePendingTextColor));
                status.setText(R.string.pending);
                confirmation.setText(R.string.unconfirmed);
            } else {
                int confirmations = WalletData.getInstance().wallet.getBestBlock() - height;
                confirmations += 1; //+1 confirmation that it exist in a block. best block - height returns 0.
                confirmation.setText(String.valueOf(confirmations));
                if (util.getBoolean(Constants.SPEND_UNCONFIRMED_FUNDS) || confirmations > 1) {
                    status.setBackgroundResource(R.drawable.tx_status_confirmed);
                    status.setTextColor(getApplicationContext().getResources().getColor(R.color.greenConfirmedTextColor));
                    status.setText(R.string.confirmed);
                } else {
                    status.setBackgroundResource(R.drawable.tx_status_pending);
                    status.setTextColor(getApplicationContext().getResources().getColor(R.color.bluePendingTextColor));
                    status.setText(R.string.pending);
                }
            }

            tvHash.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Utils.copyToClipboard(TransactionDetailsActivity.this, txHash, getString(R.string.tx_hash_copy));
                }
            });

            loadInOut(transaction.getInputs(), transaction.getOutputs());

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }

    private void loadInOut(ArrayList<TransactionInput> usedInput, ArrayList<TransactionOutput> usedOutput) {

        ArrayList<TransactionInfoAdapter.TransactionInfoItem> walletInput = new ArrayList<>();
        ArrayList<TransactionInfoAdapter.TransactionInfoItem> walletOutput = new ArrayList<>();
        ArrayList<Integer> walletOutputIndices = new ArrayList<>();
        ArrayList<Integer> walletInputIndices = new ArrayList<>();

        try {
            Bundle b = getIntent().getExtras();
            String rawJson = wallet.decodeTransaction(Utils.getHash(b.getString(Constants.HASH)));
            JSONObject parent = new JSONObject(rawJson);
            JSONArray inputs = parent.getJSONArray(Constants.INPUTS);
            JSONArray outputs = parent.getJSONArray(Constants.OUTPUTS);

            for (int i = 0; i < usedInput.size(); i++) {
                System.out.println("Object: " + usedInput.get(i));
                JSONObject input = inputs.getJSONObject(usedInput.get(i).getIndex());
                walletInputIndices.add(usedInput.get(i).getIndex());

                String hash = input.getString(Constants.PREVIOUS_TRANSACTION_HASH);

                if (hash.equals("0000000000000000000000000000000000000000000000000000000000000000")) {
                    hash = "Stakebase: 0000";
                }

                hash += ":" + input.getInt(Constants.PREVIOUS_TRANSACTION_INDEX);

                walletInput.add(new TransactionInfoAdapter.TransactionInfoItem(
                        Utils.formatDecredWithComma(usedInput.get(i).getPreviousAmount()) + " "
                                + getString(R.string.dcr) + " (" + usedInput.get(i).getAccountName() + ")", hash));
            }

            for (int i = 0; i < usedOutput.size(); i++) {
                walletOutputIndices.add(usedOutput.get(i).getIndex());
                walletOutput.add(new TransactionInfoAdapter.TransactionInfoItem(
                        Utils.formatDecredWithComma(usedOutput.get(i).getAmount()) + " " + getString(R.string.dcr) + " (" + wallet.accountOfAddress(usedOutput.get(i).getAddress()) + ")",
                        usedOutput.get(i).getAddress()));
            }

            if (transactionType.equalsIgnoreCase(Constants.VOTE)) {
                findViewById(R.id.tx_dts_vote_layout).setVisibility(View.VISIBLE);

                TextView version = findViewById(R.id.tx_dts_version);
                TextView lastBlockValid = findViewById(R.id.tx_dts_block_valid);
                TextView voteBits = findViewById(R.id.tx_dts_vote_bits);

                version.setText(
                        String.format(Locale.getDefault(), "%d", parent.getInt(Constants.VOTE_VERSION))
                );

                lastBlockValid.setText(
                        Boolean.toString(parent.getBoolean(Constants.LAST_BLOCK_VALID))
                );

                voteBits.setText(
                        parent.getString(Constants.VOTE_BITS)
                );

            }


            for (int i = 0; i < outputs.length(); i++) {
                JSONObject output = outputs.getJSONObject(i);

                if (walletOutputIndices.indexOf(i) != -1) {
                    continue;
                }

                JSONArray addresses = output.getJSONArray(Constants.ADDRESSES);

                String scriptType = output.getString(Constants.SCRIPT_TYPE);

                String address = addresses.length() > 0 ? addresses.getString(0) : "";

                String amount = Utils.formatDecredWithComma(output.getLong(Constants.VALUE)) + " " + getString(R.string.dcr) + " (external)";

                switch (scriptType) {
                    case "nulldata":
                        amount = "[null data]";
                        address = "[script]";
                        break;
                    case "stakegen":
                        address = "[stakegen]";
                }

                walletOutput.add(new TransactionInfoAdapter.TransactionInfoItem(amount, address));
            }

            for (int i = 0; i < inputs.length(); i++) {

                JSONObject input = inputs.getJSONObject(i);

                if (walletInputIndices.indexOf(i) != -1) {
                    continue;
                }

                String amount = Utils.formatDecredWithComma(input.getLong(Constants.AMOUNT_IN))
                        + " " + getString(R.string.dcr) + " (external)";
                String hash = input.getString(Constants.PREVIOUS_TRANSACTION_HASH);

                if (hash.equals("0000000000000000000000000000000000000000000000000000000000000000")) {
                    hash = "Stakebase: 0000";
                }

                hash += ":" + input.getInt(Constants.PREVIOUS_TRANSACTION_INDEX);

                walletInput.add(new TransactionInfoAdapter.TransactionInfoItem(amount, hash));
            }

            TransactionInfoAdapter inputItemAdapter = new TransactionInfoAdapter(getApplicationContext(), walletInput);
            lvInput.setAdapter(inputItemAdapter);

            TransactionInfoAdapter outputItemAdapter = new TransactionInfoAdapter(getApplicationContext(), walletOutput);
            lvOutput.setAdapter(outputItemAdapter);

            setListViewHeight(lvInput);
            setListViewHeight(lvOutput);
            ViewGroup.LayoutParams params = lvOutput.getLayoutParams();
            params.height += 50;

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.transaction_details_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.tx_details_tx_hash:
                Utils.copyToClipboard(this, txHash, getString(R.string.tx_hash_copy));
                break;
            case R.id.tx_details_raw_tx:
                Utils.copyToClipboard(this, rawTx, getString(R.string.raw_tx_copied));
                break;
            case R.id.tx_viewOnDcrData:
                String url;
                if (BuildConfig.IS_TESTNET){
                    url = "https://testnet.dcrdata.org/tx/" + txHash;
                }else{
                    url = "https://mainnet.dcrdata.org/tx/" +txHash;
                }

                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
