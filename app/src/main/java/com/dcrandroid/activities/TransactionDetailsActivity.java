package com.dcrandroid.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import com.dcrandroid.util.TransactionsResponse;
import com.dcrandroid.util.Utils;
import com.dcrandroid.view.CurrencyTextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import mobilewallet.LibWallet;

/**
 * Created by Macsleven on 02/01/2018.
 *
 */

public class TransactionDetailsActivity extends AppCompatActivity {

    private ExpandableListView expandableListView;
    private PreferenceUtil util;
    private List<String> parentHeaderInformation;
    private String transactionType;
    private Bundle extras;

    private void restartApp(){
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DcrConstants.getInstance().wallet == null){
            restartApp();
            return;
        }
        setTitle(getString(R.string.Transaction_details));
        setContentView(R.layout.transaction_details_view);
        util = new PreferenceUtil(this);
        extras = getIntent().getExtras();
        if (extras == null){
            System.out.println("Extras is null");
            return;
        }

        transactionType = extras.getString(Constants.TYPE);
        transactionType = transactionType.substring(0,1).toUpperCase() + transactionType.substring(1).toLowerCase();

        parentHeaderInformation = new ArrayList<>();

        parentHeaderInformation.add(getString(R.string.inputs));
        parentHeaderInformation.add(getString(R.string.outputs));
        ArrayList<TransactionsResponse.TransactionInput> inputs
                = (ArrayList<TransactionsResponse.TransactionInput>) extras.getSerializable(Constants.Inputs);
        ArrayList<TransactionsResponse.TransactionOutput> outputs
                = (ArrayList<TransactionsResponse.TransactionOutput>) extras.getSerializable(Constants.Inputs);
        HashMap<String, List<String>> allChildItems = returnGroupedChildItems(inputs, outputs);

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
        txHash.setText(extras.getString(Constants.HASH));
        TextView viewOnDcrdata = findViewById(R.id.tx_view_on_dcrdata);

        viewOnDcrdata.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "https://testnet.dcrdata.org/tx/"+txHash.getText().toString();
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
            }
        });

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

        value.formatAndSetText(Utils.formatDecred(extras.getLong(Constants.AMOUNT,0)) +" "+getString(R.string.dcr));
        transactionFee.formatAndSetText(Utils.formatDecred(extras.getLong(Constants.FEE,0)) +" "+getString(R.string.dcr));

        Calendar calendar = new GregorianCalendar(TimeZone.getDefault());
        calendar.setTimeInMillis(extras.getLong(Constants.TIMESTAMP) * 1000);
        SimpleDateFormat sdf = new SimpleDateFormat(" dd yyyy, hh:mma", Locale.getDefault());

        date.setText(calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) + sdf.format(calendar.getTime()).toLowerCase());
        txType.setText(transactionType);
        int height = extras.getInt(Constants.HEIGHT, 0);
        if(height == -1){
            //No included in block chain, therefore transaction is pending
            status.setBackgroundResource(R.drawable.tx_status_pending);
            status.setTextColor(Color.parseColor("#3d659c"));
            status.setText("pending");
            confirmation.setText("unconfirmed");
        }else{
            int confirmations = DcrConstants.getInstance().wallet.getBestBlock() - height;
            confirmations += 1; //+1 confirmation that it exist in a block. best block - height returns 0.
            confirmation.setText(String.valueOf(confirmations));
            if(util.getBoolean(Constants.SPEND_UNCONFIRMED_FUNDS) || confirmations > 1){
                status.setBackgroundResource(R.drawable.tx_status_confirmed);
                status.setTextColor(Color.parseColor("#55bb97"));
                status.setText("confirmed");
            }else{
                status.setBackgroundResource(R.drawable.tx_status_pending);
                status.setTextColor(Color.parseColor("#3d659c"));
                status.setText("pending");
            }
        }
    }

    private HashMap<String, List<String>> returnGroupedChildItems(ArrayList<TransactionsResponse.TransactionInput> usedInput, ArrayList<TransactionsResponse.TransactionOutput> usedOutput){
        ArrayList<String> walletOutput = new ArrayList<>();
        LibWallet wallet = DcrConstants.getInstance().wallet;
        if (transactionType.equalsIgnoreCase("vote")){
            for (int i = 0; i < usedOutput.size(); i++){
                TransactionsResponse.TransactionOutput output = usedOutput.get(i);
                StringBuilder sb = new StringBuilder();
                if (wallet.isAddressMine(output.address)) {
                    sb.append(output.address).append(" (").append(wallet.getAccountByAddress(output.address)).append(")");
                    if (extras.getInt(Constants.DIRECTION, -1) == 0) {
                        sb.append(" (change)");
                    }
                    continue;
                }

                sb.append(output.address).append(" (external)");
                walletOutput.add(sb.toString() + "\n" + Utils.formatDecred(output.amount));
            }
        }else {
            try {
                Bundle b = getIntent().getExtras();
                String rawJson = wallet.decodeTransaction(Utils.getHash(b.getString(Constants.HASH)));
                JSONObject parent = new JSONObject(rawJson);
                JSONArray input = parent.getJSONArray(Constants.Inputs);
                JSONArray outputs = parent.getJSONArray(Constants.OUTPUTS);
                for (int i = 0; i < outputs.length(); i++) {
                    JSONObject output = outputs.getJSONObject(i);
                    StringBuilder sb = new StringBuilder();
                    JSONArray addresses = output.getJSONArray(Constants.ADDRESSES);
                    for (int j = 0; j < addresses.length(); j++) {
                        if (j != 0) {
                            sb.append("\n");
                        }
                        if (wallet.isAddressMine(addresses.getString(j))) {
                            sb.append(addresses.getString(j)).append(" (").append(wallet.getAccountByAddress(addresses.getString(j))).append(")");
                            if (getIntent().getIntExtra(Constants.DIRECTION, -1) == 0) {
                                sb.append(" (change)");
                            }
                            continue;
                        }
                        sb.append(addresses.getString(j)).append(" (external)");
                    }
                    walletOutput.add(sb.toString() + "\n" + Utils.formatDecred(output.getLong("Value")));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        HashMap<String, List<String>> childContent = new HashMap<>();
        ArrayList<String> inputs = new ArrayList<>();
        for (int j = 0; j < usedInput.size(); j++) {
            inputs.add(usedInput.get(j).accountName + "\n" + Utils.formatDecred(usedInput.get(j).previous_amount));
        }
        childContent.put(parentHeaderInformation.get(0), inputs);
        childContent.put(parentHeaderInformation.get(1), walletOutput);
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
