package com.dcrandroid.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dcrandroid.adapter.ExpandableListViewAdapter;
import com.dcrandroid.R;
import com.dcrandroid.util.DcrConstants;
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
        Intent intent = getIntent();
        parentHeaderInformation = new ArrayList<>();

        parentHeaderInformation.add(getString(R.string.used_inputs));
        parentHeaderInformation.add(getString(R.string.new_wallet_output));
        HashMap<String, List<String>> allChildItems = returnGroupedChildItems(intent.getStringArrayListExtra("UsedInput"),intent.getStringArrayListExtra("newWalletOutPut"));

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
        txHash.setText(intent.getStringExtra("Hash"));
        txHash.setText(intent.getStringExtra(Constants.EXTRA_TRANSACTION_HASH));
        TextView viewOnDcrdata = findViewById(R.id.tx_view_on_dcrdata);
        viewOnDcrdata.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "https://testnet.dcrdata.org/tx/"+txHash.getText().toString();
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
                String url = "https://testnet.dcrdata.org/tx/"+txHash.getText().toString();
//                if(Dcrwallet.isTestNet()){
//                    url = "https://testnet.dcrdata.org/tx/"+txHash.getText().toString();
//                }
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
            }
        });
        try {
            Utils.getHash(intent.getStringExtra(Constants.EXTRA_TRANSACTION_HASH));
        } catch (Exception e) {
            e.printStackTrace();
        }
        txHash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipboard(txHash.getText().toString(),getString(R.string.tx_hash_copy));
            }
        });
        expandableListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick( AdapterView<?> parent, View view, int position, long id) {

                long packedPosition = expandableListView.getExpandableListPosition(position);

                int itemType = ExpandableListView.getPackedPositionType(packedPosition);
                int groupPosition = ExpandableListView.getPackedPositionGroup(packedPosition);
                int childPosition = ExpandableListView.getPackedPositionChild(packedPosition);
                if (itemType == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
                    if(groupPosition == 1){
                        String[] temp =  expandableListView.getExpandableListAdapter().getChild(1,childPosition).toString().split("\\n");
                        String hash = temp[0];
                        copyToClipboard(hash,getString(R.string.your_address_is_copied));
                    }
                }
                return true;
            }
        });

        if(intent.getFloatExtra("Fee",0) > 0){
            value.formatAndSetText("- "+Utils.formatDecred(intent.getFloatExtra(Constants.EXTRA_TRANSACTION_TOTAL_INPUT,0)) +" "+getString(R.string.dcr));
            System.out.println("Formatter: "+Utils.formatDecred(intent.getFloatExtra(Constants.EXTRA_TRANSACTION_FEE,0)));
            transactionFee.formatAndSetText(Utils.formatDecred(intent.getFloatExtra(Constants.EXTRA_TRANSACTION_FEE,0)));
        }
        else{
            value.formatAndSetText(Utils.formatDecred(intent.getFloatExtra(Constants.EXTRA_AMOUNT,0)) +" "+getString(R.string.dcr));
            System.out.println(".2 F is on");
            transactionFee.formatAndSetText(Utils.formatDecred(0)+" DCR");
        }
        date.setText(intent.getStringExtra(Constants.EXTRA_TRANSACTION_DATE));
        String type = intent.getStringExtra(Constants.EXTRA_TRANSACTION_TYPE);
        type = type.substring(0,1).toUpperCase() + type.substring(1).toLowerCase();
        txType.setText(type);
        int height = intent.getIntExtra(Constants.EXTRA_BLOCK_HEIGHT, 0);
        int confirmations = DcrConstants.getInstance().wallet.getBestBlock() - height;
        System.out.println("Height: "+height +" Bestblock: "+ DcrConstants.getInstance().wallet.getBestBlock());
        if(height == 0){
            //No included in block chain, therefore transaction is pending
            status.setBackgroundResource(R.drawable.tx_status_pending);
            status.setTextColor(Color.parseColor("#3d659c"));
            status.setText("pending");
            confirmation.setText("0");
        }else{
            confirmation.setText(String.valueOf(confirmations));
            if(util.getBoolean(Constants.KEY_SPEND_UNCONFIRMED_FUNDS) || confirmations > 1){
                if(confirmations > 1){
                    System.out.println("Confirmation is greater than 1");
                }
                if(util.getBoolean(Constants.KEY_SPEND_UNCONFIRMED_FUNDS)){
                    System.out.println("Unconfirmed funds are spendable");
                }
                status.setBackgroundResource(R.drawable.tx_status_confirmed);
                status.setTextColor(Color.parseColor("#55bb97"));
                status.setText("confirmed");
            }else{
                status.setBackgroundResource(R.drawable.tx_status_pending);
                status.setTextColor(Color.parseColor("#3d659c"));
                status.setText("pending");
            }
        }
//        if(height == 0 || confirmations < 2){
//            status.setBackgroundResource(R.drawable.tx_status_pending);
//            status.setTextColor(Color.parseColor("#3d659c"));
//            status.setText("pending");
//            confirmation.setText(String.valueOf(height < 1 ? 0 : confirmations));
//        }else{
//            status.setBackgroundResource(R.drawable.tx_status_confirmed);
//            status.setTextColor(Color.parseColor("#55bb97"));
//            status.setText("confirmed");
//            confirmation.setText(String.valueOf(confirmations));
//        }
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

    public void copyToClipboard(String copyText,String message) {
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
                message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER , 0, -190);
        toast.show();
    }
}
