package com.decrediton.Activities;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.decrediton.Adapter.ExpandableListViewAdapter;
import com.decrediton.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Macsleven on 02/01/2018.
 */

public class TransactionDetailsActivity extends AppCompatActivity {

    private ExpandableListView expandableListView;

    private List<String> parentHeaderInformation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Transaction details ");
        setContentView(R.layout.transaction_details_view);

        parentHeaderInformation = new ArrayList<String>();

        parentHeaderInformation.add("Used Inputs");
        parentHeaderInformation.add("New Wallet Output");
        HashMap<String, List<String>> allChildItems = returnGroupedChildItems(getIntent().getStringArrayListExtra("UsedIput"),getIntent().getStringArrayListExtra("newWalletOutPut"));

        expandableListView = (ExpandableListView)findViewById(R.id.in_out);

        ExpandableListViewAdapter expandableListViewAdapter = new ExpandableListViewAdapter(getApplicationContext(), parentHeaderInformation, allChildItems);

        expandableListView.setAdapter(expandableListViewAdapter);

        TextView amount = findViewById(R.id.tx_dts_amount);
        TextView account = findViewById(R.id.tx_dts_acc);
        TextView date = findViewById(R.id.tx_date);
        TextView status = findViewById(R.id.tx_dts__status);
        TextView txType = findViewById(R.id.label_tx);
        TextView transactionFee = findViewById(R.id.tx_fee);

        if(Double.parseDouble(getIntent().getStringExtra("Fee"))>0){
            String temp = "-"+getIntent().getStringExtra("Fee") +" DCR";
            amount.setText(temp);
        }
        else{
            String temp= getIntent().getStringExtra("Amount")+ " DCR";
            amount.setText(temp);
        }

        account.setText(getIntent().getStringExtra("AccountName"));
        date.setText(getIntent().getStringExtra("TxDate"));
        status.setText(getIntent().getStringExtra("TxStatus"));
        txType.setText(getIntent().getStringExtra("TxType"));
        transactionFee.setText(getIntent().getStringExtra("Fee"));
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
}
