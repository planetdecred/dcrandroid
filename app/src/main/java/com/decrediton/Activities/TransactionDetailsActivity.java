package com.decrediton.Activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import com.decrediton.Adapter.ExpandableListViewAdapter;
import com.decrediton.R;
import com.decrediton.Util.AccountResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Created by Macsleven on 02/01/2018.
 */

public class TransactionDetailsActivity extends AppCompatActivity {

    private ExpandableListView expandableListView;

    private List<String> parentHeaderInformation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.Transaction_details));
        setContentView(R.layout.transaction_details_view);

        parentHeaderInformation = new ArrayList<>();

        parentHeaderInformation.add(getString(R.string.used_inputs));
        parentHeaderInformation.add(getString(R.string.new_wallet_output));
        HashMap<String, List<String>> allChildItems = returnGroupedChildItems(getIntent().getStringArrayListExtra("UsedInput"),getIntent().getStringArrayListExtra("newWalletOutPut"));

        expandableListView = (ExpandableListView)findViewById(R.id.in_out);

        ExpandableListViewAdapter expandableListViewAdapter = new ExpandableListViewAdapter(getApplicationContext(), parentHeaderInformation, allChildItems);

        expandableListView.setAdapter(expandableListViewAdapter);

        TextView value = findViewById(R.id.tx_dts_value);
        TextView date = findViewById(R.id.tx_date);
        TextView status = findViewById(R.id.tx_dts__status);
        TextView confirmation = findViewById(R.id.confirmations);
        TextView transactionFee = findViewById(R.id.tx_fee);
        final TextView txHash = findViewById(R.id.tx_hash);
        TextView viewOnDcrdata = findViewById(R.id.tx_view_on_dcrdata);
        viewOnDcrdata.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), WebviewActivity.class);
                i.putExtra("TxHash","http://www.google.com");
                startActivity(i);
            }
        });
        txHash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipboard(txHash.getText().toString());
            }
        });

        if(Double.parseDouble(getIntent().getStringExtra("Fee")) > 0){
            String temp = "- "+getIntent().getStringExtra("Fee") +getString(R.string.dcr);
            value.setText(temp);
            transactionFee.setText(temp);
        }
        else{
            String temp = getIntent().getStringExtra("Amount")+ getString(R.string.dcr);
            value.setText(temp);

            temp = String.format(Locale.getDefault(),"%f DCR", 0/ AccountResponse.SATOSHI);
            transactionFee.setText(temp);
        }
        date.setText(getIntent().getStringExtra("TxDate"));
        status.setText(getIntent().getStringExtra("TxStatus"));
        confirmation.setText(getIntent().getStringExtra("TxConfirmation"));
        //transactionFee.setText(getIntent().getStringExtra("Fee"));
        if(status.getText().toString().equals("pending")){
            status.setBackgroundResource(R.drawable.tx_status_pending);
            status.setTextColor(Color.parseColor("#3d659c"));
        }
        else if(status.getText().toString().equals("confirmed")) {
            status.setBackgroundResource(R.drawable.tx_status_confirmed);
            status.setTextColor(Color.parseColor("#55bb97"));
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
