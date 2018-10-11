package com.dcrandroid.activities;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.dcrandroid.R;
import com.dcrandroid.adapter.TransactionInfoAdapter;
import com.dcrandroid.data.Constants;
import com.dcrandroid.util.CoinFormat;
import com.dcrandroid.util.DcrConstants;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.TransactionsResponse;
import com.dcrandroid.util.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import mobilewallet.LibWallet;

/**
 * Created by Macsleven on 02/01/2018.
 */

public class TransactionDetailsActivity extends AppCompatActivity {

    private ListView lvInput, lvOutput;
    private PreferenceUtil util;
    private String txHash;
    private String rawTx;


    private void restartApp() {
        PackageManager packageManager = getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(getPackageName());
        if (intent != null) {
            ComponentName componentName = intent.getComponent();
            Intent mainIntent = Intent.makeRestartActivityTask(componentName);
            startActivity(mainIntent);
            Runtime.getRuntime().exit(0);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DcrConstants.getInstance().wallet == null) {
            restartApp();
            return;
        }
        setTitle(getString(R.string.Transaction_details));
        setContentView(R.layout.transaction_details_view);
        util = new PreferenceUtil(this);
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            System.out.println("Extras is null");
            return;
        }

        lvInput = findViewById(R.id.lvInput);
        lvOutput = findViewById(R.id.lvOutput);

        String transactionType = extras.getString(Constants.TYPE);
        transactionType = transactionType.substring(0, 1).toUpperCase() + transactionType.substring(1).toLowerCase();

        ArrayList<TransactionsResponse.TransactionInput> inputs
                = (ArrayList<TransactionsResponse.TransactionInput>) extras.getSerializable(Constants.INPUTS);
        ArrayList<TransactionsResponse.TransactionOutput> outputs
                = (ArrayList<TransactionsResponse.TransactionOutput>) extras.getSerializable(Constants.OUTPUTS);

        loadInOut(inputs, outputs);

        TextView value = findViewById(R.id.tx_dts_value);
        TextView date = findViewById(R.id.tx_date);
        TextView status = findViewById(R.id.tx_dts__status);
        TextView txType = findViewById(R.id.txtype);
        TextView confirmation = findViewById(R.id.tx_dts_confirmation);
        TextView transactionFee = findViewById(R.id.tx_fee);
        final TextView tvHash = findViewById(R.id.tx_hash);


        tvHash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.copyToClipboard(TransactionDetailsActivity.this, txHash, getString(R.string.tx_hash_copy));
            }
        });


        rawTx = extras.getString(Constants.RAW);

        txHash = extras.getString(Constants.HASH);
        tvHash.setText(txHash);

        value.setText(CoinFormat.Companion.format(Utils.formatDecredWithComma(extras.getLong(Constants.AMOUNT, 0)) + " " + getString(R.string.dcr)));
        transactionFee.setText(CoinFormat.Companion.format(Utils.formatDecredWithComma(extras.getLong(Constants.FEE, 0)) + " " + getString(R.string.dcr)));

        Calendar calendar = new GregorianCalendar(TimeZone.getDefault());
        calendar.setTimeInMillis(extras.getLong(Constants.TIMESTAMP) * 1000);
        SimpleDateFormat sdf = new SimpleDateFormat(" dd yyyy, hh:mma", Locale.getDefault());

        date.setText(calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) + sdf.format(calendar.getTime()).toLowerCase());
        txType.setText(transactionType);
        int height = extras.getInt(Constants.HEIGHT, 0);
        if (height == -1) {
            //No included in block chain, therefore transaction is pending
            status.setBackgroundResource(R.drawable.tx_status_pending);
            status.setTextColor(getApplicationContext().getResources().getColor(R.color.bluePendingTextColor));
            status.setText(R.string.pending);
            confirmation.setText(R.string.unconfirmed);
        } else {
            int confirmations = DcrConstants.getInstance().wallet.getBestBlock() - height;
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
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    private void loadInOut(ArrayList<TransactionsResponse.TransactionInput> usedInput, ArrayList<TransactionsResponse.TransactionOutput> usedOutput) {
        LibWallet wallet = DcrConstants.getInstance().wallet;

        ArrayList<TransactionInfoAdapter.TransactionInfoItem> walletInput = new ArrayList<>();
        ArrayList<TransactionInfoAdapter.TransactionInfoItem> walletOutput = new ArrayList<>();
        ArrayList<Integer> walletOutputIndices = new ArrayList<>();
        ArrayList<Integer> walletInputIndices = new ArrayList<>();

        for (int i = 0; i < usedInput.size(); i++) {
            walletInputIndices.add(usedInput.get(i).index);
            walletInput.add(new TransactionInfoAdapter.TransactionInfoItem(Utils.formatDecredWithComma(usedInput.get(i).previous_amount) + " " + getString(R.string.dcr),
                    usedInput.get(i).accountName));
            util.set(Constants.ACCOUNT_NAME, usedInput.get(i).accountName);

        }

        for (int i = 0; i < usedOutput.size(); i++) {
            walletOutputIndices.add(usedOutput.get(i).index);
            walletOutput.add(new TransactionInfoAdapter.TransactionInfoItem(Utils.formatDecredWithComma(usedOutput.get(i).amount) + " " + getString(R.string.dcr),
                    usedOutput.get(i).address));
        }

        try {
            Bundle b = getIntent().getExtras();
            String rawJson = wallet.decodeTransaction(Utils.getHash(b.getString(Constants.HASH)));
            JSONObject parent = new JSONObject(rawJson);

            JSONArray outputs = parent.getJSONArray(Constants.OUTPUTS);

            for (int i = 0; i < outputs.length(); i++) {
                JSONObject output = outputs.getJSONObject(i);

                if (walletOutputIndices.indexOf(i) != -1) {
                    continue;
                }

                JSONArray addresses = output.getJSONArray(Constants.ADDRESSES);

                String address = addresses.length() > 0 ? addresses.getString(0) : getString(R.string.script_bracket);

                boolean nullScript = output.getBoolean(Constants.NULL_SCRIPT);

                walletOutput.add(new TransactionInfoAdapter.TransactionInfoItem(nullScript ? "[null data]" : Utils.formatDecredWithComma(output.getLong("Value"))
                        + " " + getString(R.string.dcr), address));
            }

            JSONArray inputs = parent.getJSONArray(Constants.INPUTS);
            for (int i = 0; i < inputs.length(); i++) {

                JSONObject input = inputs.getJSONObject(i);

                if (walletInputIndices.indexOf(i) != -1) {
                    continue;
                }
                walletInput.add(new TransactionInfoAdapter.TransactionInfoItem(Utils.formatDecredWithComma(input.getLong("AmountIn"))
                        + " " + getString(R.string.dcr), input.getString("PreviousTransactionHash")));
            }

            TransactionInfoAdapter inputItemAdapter = new TransactionInfoAdapter(getApplicationContext(), walletInput);
            lvInput.setAdapter(inputItemAdapter);

            TransactionInfoAdapter outputItemAdapter = new TransactionInfoAdapter(getApplicationContext(), walletOutput);
            lvOutput.setAdapter(outputItemAdapter);
            copyHashFromOutputItem(lvOutput);

            setListViewHeight(lvInput);
            setListViewHeight(lvOutput);
            ViewGroup.LayoutParams params = lvOutput.getLayoutParams();
            params.height += 50;

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


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


    private void copyHashFromOutputItem(final ListView listView) {

        ListAdapter listAdapter = listView.getAdapter();
        final SparseArray<String> walletHashAddress = new SparseArray<>();

        if (listAdapter != null) {
            for (int i = 0; i < listAdapter.getCount(); i++) {
                View listItem = listAdapter.getView(i, null, listView);
                TextView requiredHash = listItem.findViewById(R.id.tvInfo);
                walletHashAddress.put(i, requiredHash.getText().toString());
            }
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Utils.copyToClipboard(getApplicationContext(), walletHashAddress.get(position), getString(R.string.address_copy_text));
                }
            });
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
                String url = "https://testnet.dcrdata.org/tx/" + txHash;
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
