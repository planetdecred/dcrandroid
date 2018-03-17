package com.dcrandroid.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dcrandroid.adapter.ExpandableListViewAdapter;
import com.dcrandroid.R;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.data.Constants;
import com.dcrandroid.util.Utils;
import com.dcrandroid.view.CurrencyTextView;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

//import dcrwallet.Dcrwallet;

/**
 * Created by Macsleven on 02/01/2018.
 *
 */

public class TransactionDetailsActivity extends AppCompatActivity {

    private ExpandableListView expandableListView;
    private PreferenceUtil util;
    private List<String> parentHeaderInformation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.Transaction_details));
        setContentView(R.layout.transaction_details_view);
        util = new PreferenceUtil(this);
        parentHeaderInformation = new ArrayList<>();

        parentHeaderInformation.add(getString(R.string.used_inputs));
        parentHeaderInformation.add(getString(R.string.new_wallet_output));
        HashMap<String, List<String>> allChildItems = returnGroupedChildItems(getIntent().getStringArrayListExtra("UsedInput"),getIntent().getStringArrayListExtra("newWalletOutPut"));

        expandableListView = findViewById(R.id.in_out);

        ExpandableListViewAdapter expandableListViewAdapter = new ExpandableListViewAdapter(getApplicationContext(), parentHeaderInformation, allChildItems);

        expandableListView.setAdapter(expandableListViewAdapter);

        CurrencyTextView value = findViewById(R.id.tx_dts_value);
        TextView date = findViewById(R.id.tx_date);
        TextView status = findViewById(R.id.tx_dts__status);
        TextView txType = findViewById(R.id.txtype);
        TextView confirmation = findViewById(R.id.tx_dts_confirmation);
        CurrencyTextView transactionFee = findViewById(R.id.tx_fee);
        final TextView txHash = findViewById(R.id.tx_hash);
        txHash.setText(getIntent().getStringExtra("Hash"));
        txHash.setText(getIntent().getStringExtra(Constants.EXTRA_TRANSACTION_HASH));
        TextView viewOnDcrdata = findViewById(R.id.tx_view_on_dcrdata);
        viewOnDcrdata.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "https://explorer.dcrdata.org/tx/"+txHash.getText().toString();
//                if(Dcrwallet.isTestNet()){
//                    url = "https://testnet.dcrdata.org/tx/"+txHash.getText().toString();
//                }
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
            }
        });
        txHash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "https://explorer.dcrdata.org/tx/"+txHash.getText().toString();
//                if(Dcrwallet.isTestNet()){
//                    url = "https://testnet.dcrdata.org/tx/"+txHash.getText().toString();
//                }
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
            }
        });
        try {
            Utils.getHash(getIntent().getStringExtra(Constants.EXTRA_TRANSACTION_HASH));
        } catch (Exception e) {
            e.printStackTrace();
        }
        txHash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipboard(txHash.getText().toString());
            }
        });

        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(8);
        if(getIntent().getFloatExtra("Fee",0) > 0){
            value.formatAndSetText("- "+nf.format(getIntent().getFloatExtra(Constants.EXTRA_TRANSACTION_TOTAL_INPUT,0)) +" "+getString(R.string.dcr));
            System.out.println("Formatter: "+nf.format(getIntent().getFloatExtra("Fee",0)));
            transactionFee.formatAndSetText(nf.format(getIntent().getFloatExtra("Fee",0)));
        }
        else{
            value.formatAndSetText(nf.format(getIntent().getFloatExtra("Amount",0)) +" "+getString(R.string.dcr));
            System.out.println(".2 F is on");
            transactionFee.formatAndSetText(nf.format(0)+" DCR");
        }
        date.setText(getIntent().getStringExtra("TxDate"));
        status.setText(getIntent().getStringExtra("TxStatus"));
        String type = getIntent().getStringExtra("TxType");
        type = type.substring(0,1).toUpperCase() + type.substring(1).toLowerCase();
        txType.setText(type);
        if(status.getText().toString().equals("pending")){
            status.setBackgroundResource(R.drawable.tx_status_pending);
            status.setTextColor(Color.parseColor("#3d659c"));
            confirmation.setText("0");
        }else if(status.getText().toString().equals("confirmed")) {
            status.setBackgroundResource(R.drawable.tx_status_confirmed);
            status.setTextColor(Color.parseColor("#55bb97"));
            confirmation.setText(String.format(Locale.getDefault(),"%d",util.getInt(PreferenceUtil.BLOCK_HEIGHT) - getIntent().getIntExtra("Height",0)));
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private HashMap<String, List<String>> returnGroupedChildItems(ArrayList<String> usedInput,ArrayList<String> output){
        HashMap<String, List<String>> childContent = new HashMap<String, List<String>>();
        childContent.put(parentHeaderInformation.get(0), usedInput);
        childContent.put(parentHeaderInformation.get(1), output);
        return childContent;
    }

    public void copyToClipboard(String copyText) {
        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(copyText);
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                    getApplication().getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData
                    .newPlainText(getString(R.string.your_address), copyText);
            clipboard.setPrimaryClip(clip);
        }
        Toast toast = Toast.makeText(getApplicationContext(),
                R.string.tx_hash_copy, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM | Gravity.RIGHT, 50, 50);
        toast.show();
    }
}
