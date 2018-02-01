package com.decrediton.fragments;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.decrediton.activities.ReaderActivity;
import com.decrediton.R;
import com.decrediton.util.AccountResponse;
import com.decrediton.util.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import dcrwallet.Balance;
import dcrwallet.ConstructTxResponse;
import dcrwallet.Dcrwallet;

import static android.app.Activity.RESULT_OK;

/**
 * Created by Macsleven on 28/11/2017.
 */

public class SendFragment extends android.support.v4.app.Fragment implements AdapterView.OnItemSelectedListener{

    public EditText address,amount;
    public TextView totalAmountSending,estimateFee,estimateSize,sendAll;
    public ImageView scanAddress;
    Button send;
    Spinner accountSpinner;
    private static final int SCANNER_ACTIVITY_RESULT_CODE = 0;
    List<String> categories;
    List<Integer> accountNumbers = new ArrayList<>();
    ArrayAdapter dataAdapter;
    ProgressDialog pd;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //returning our layout file
        //change R.layout.yourlayoutfilename for each of your fragments
        return inflater.inflate(R.layout.content_send, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //you can set the title for your toolbar here for different fragments different titles
        if(getActivity() == null){
            System.out.println("Activity is null");
            return;
        }
        getActivity().setTitle(getString(R.string.send));

        address = getActivity().findViewById(R.id.send_dcr_add);
        amount = getActivity().findViewById(R.id.send_dcr_amount);
        totalAmountSending = getActivity().findViewById(R.id.send_dcr_total_amt_sndng);
        scanAddress = getActivity().findViewById(R.id.send_dcr_scan);
        estimateSize = getActivity().findViewById(R.id.send_dcr_estimate_size);
        estimateFee = getActivity().findViewById(R.id.send_dcr_estimate_fee);
        sendAll = getActivity().findViewById(R.id.send_dcr_all);
        send = getActivity().findViewById(R.id.send_btn_tx);
        amount.addTextChangedListener(watcher);
        address.addTextChangedListener(watcher);
        accountSpinner = view.findViewById(R.id.send_dropdown);
        accountSpinner.setOnItemSelectedListener(this);
        // Spinner Drop down elements
        categories = new ArrayList<>();
        if(getContext() == null){
            System.out.println("Context is null");
            return;
        }
        dataAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, categories);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        accountSpinner.setAdapter(dataAdapter);

        scanAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), ReaderActivity.class);
                startActivityForResult(intent, SCANNER_ACTIVITY_RESULT_CODE);
            }
        });
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String amnt = amount.getText().toString();
                if(amnt.equals("")){
                    amnt = "0";
                }
                final String destAddress = address.getText().toString();
                final long amt = Math.round(Double.parseDouble(amnt) * 1e8);
                if(destAddress.equals("")){
                    Toast.makeText(getContext(), "Destination Address cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }else if(!validateAddress(destAddress)){
                    Toast.makeText(getContext(), "Destination Address is not valid", Toast.LENGTH_SHORT).show();
                    return;
                }else if(amt <= 0){
                    Toast.makeText(getContext(), "Amount is not valid", Toast.LENGTH_SHORT).show();
                    return;
                }
                showInputPassPhraseDialog(destAddress, amt);
            }
        });
        sendAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ProgressDialog pd = Utils.getProgressDialog(SendFragment.this.getContext(),false,false,"Calculating total spendable...");
                pd.show();
                new Thread(){
                    public void run(){
                        try{
                            final Balance balance = Dcrwallet.getBalance(accountNumbers.get(accountSpinner.getSelectedItemPosition()));
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    amount.setText(String.format(Locale.getDefault(),"%.8f",balance.getSpendable()/ AccountResponse.SATOSHI));
                                    if(pd.isShowing()){
                                        pd.dismiss();
                                    }
                                }
                            });
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        });
        prepareAccounts();
    }

    TextWatcher watcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            constructTransaction();
        }
    };

    private void constructTransaction(){
        String amnt = amount.getText().toString();
        if(amnt.equals("")){
            amnt = "0";
        }
        final String destAddress = address.getText().toString();
        final double amt;
        try {
            amt = (Double.parseDouble(amnt) * 1e8);
        }catch (Exception e){
            e.printStackTrace();
            return;
        }
        if(destAddress.equals("")){
            return;
        }else if(!validateAddress(destAddress)){
            return;
        }else if(amt <= 0){
            estimateSize.setText(R.string.zero_bytes);
            totalAmountSending.setText(R.string.zero_decred);
            estimateFee.setText(R.string.zero_decred);
            return;
        }
        new Thread(){
            public void run(){
                try{
                    final ConstructTxResponse response = Dcrwallet.constructTransaction(destAddress, Math.round(amt), accountNumbers.get(accountSpinner.getSelectedItemPosition()));
                    if(getActivity() == null){
                        System.out.println("Activity is null");
                        return;
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            double totalAmount = (amt + (response.getEstimatedSignedSize() / 0.001)) / 1e8;
                            double estFee = ((response.getEstimatedSignedSize() / 0.001) / 1e8);
                            estimateSize.setText(String.format(Locale.getDefault(),"%d bytes",response.getEstimatedSignedSize()));
                            totalAmountSending.setText(String.format(Locale.getDefault(),"%.8f DCR", totalAmount));
                            estimateFee.setText(String.format(Locale.getDefault(),"%.8f DCR", estFee));
                        }
                    });
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private boolean validateAddress(String address){
        if(address.startsWith("decred:"))
            address = address.replace("decred:","");
        if(address.length() < 25){
            return true;
        }else if(address.length() > 36){
            return true;
        }
        if(address.startsWith("D")){
            return true;
        }else{
            return true;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == SCANNER_ACTIVITY_RESULT_CODE) {
            if(resultCode== RESULT_OK) {
                try {
                    String returnString = intent.getStringExtra("keyName");
                    if(returnString.startsWith("decred:"))
                        returnString = returnString.replace("decred:","");
                    if(returnString.length() < 25){
                        Toast.makeText(SendFragment.this.getContext(), R.string.wallet_add_too_short, Toast.LENGTH_SHORT).show();
                        return;
                    }else if(returnString.length() > 36){
                        Toast.makeText(SendFragment.this.getContext(), R.string.wallet_addr_too_long, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if(returnString.startsWith("D")){
                        address.setText(returnString);
                    }else{
                        Toast.makeText(SendFragment.this.getContext(), R.string.invalid_address_prefix, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(getContext(), R.string.error_not_decred_address, Toast.LENGTH_LONG).show();
                    address.setText("");
                }
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        constructTransaction();
    }

    private void prepareAccounts(){
        new Thread(){
            public void run(){
                try{
                    final AccountResponse response = AccountResponse.parse(Dcrwallet.getAccounts());
                    if(response.errorOccurred){
                        if(getActivity() == null){
                            System.out.println("Activity is null");
                            return;
                        }
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
//                                if(pd.isShowing()){
//                                    pd.dismiss();
//                                }
                                Toast.makeText(SendFragment.this.getContext(),response.errorMessage,Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    }
                    accountNumbers.clear();
                    categories.clear();
                    for(int i = 0; i < response.items.size(); i++){
                        if(response.items.get(i).name.trim().equals("imported")){
                            continue;
                        }
                        categories.add(i, response.items.get(i).name + String.format(Locale.getDefault(), " [%.8f]",response.items.get(i).balance.spendable));
                        accountNumbers.add(response.items.get(i).number);
                    }
                    if(getActivity() == null){
                        System.out.println("Activity is null");
                        return;
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
//                            if(pd.isShowing()){
//                                pd.dismiss();
//                            }
                            dataAdapter.notifyDataSetChanged();
                            //Default Account
                            //getTransactionFee(0);
                        }
                    });
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }.start();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public void startTransaction(final String passphrase, final String destAddress,final long amt){
        pd = Utils.getProgressDialog(getContext(),false,false,"Processing...");
        pd.show();
        new Thread(){
            public void run(){
                try {
                    final ConstructTxResponse response = Dcrwallet.constructTransaction(destAddress, amt, accountNumbers.get(accountSpinner.getSelectedItemPosition()));
                    byte[] tx = Dcrwallet.signTransaction(response.getUnsignedTransaction(),passphrase);
                    byte[] serializedTx = Dcrwallet.publishTransaction(tx);
                    List<Byte> hashList = new ArrayList<>();
                    for (byte aSerializedTx : serializedTx) {
                        hashList.add(aSerializedTx);
                    }
                    Collections.reverse(hashList);
                    final StringBuilder sb = new StringBuilder();
                    for(byte b : hashList){
                        sb.append(String.format(Locale.getDefault(),"%02x", b));
                    }
                    System.out.println("Hash: "+sb.toString());
                    if(getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(pd.isShowing()){
                                    pd.dismiss();
                                }
                                showTxConfirmDialog(sb.toString());
                                send.setEnabled(true);
                                }
                            });
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void showInputPassPhraseDialog(final String destAddress, final long amt) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.input_passphrase_box, null);
        dialogBuilder.setCancelable(false);
        dialogBuilder.setView(dialogView);

        final EditText passphrase = (EditText) dialogView.findViewById(R.id.passphrase_input);

        dialogBuilder.setMessage(getString(R.string.transaction_confirmation)+String.format(Locale.getDefault()," %.8f DCR", amt/1e8));
        dialogBuilder.setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String pass = passphrase.getText().toString();
                if(pass.length() > 0){
                    startTransaction(pass, destAddress, amt);
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

    public void showTxConfirmDialog(final String txHash) {
        if(getActivity() == null){
            System.out.println("Activity is null");
            return;
        }
        if(getContext() == null){
            System.out.println("Context is null");
            return;
        }
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.tx_confrimation_display, null);
        dialogBuilder.setCancelable(false);
        dialogBuilder.setView(dialogView);

        final TextView txHashtv = dialogView.findViewById(R.id.tx_hash_confirm_view);
        txHashtv.setText(txHash);
        txHashtv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipboard(txHashtv.getText().toString());
            }
        });

        dialogBuilder.setTitle("Transaction was successful");
        dialogBuilder.setPositiveButton("OKAY", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialogBuilder.setCancelable(true);
                //do something with edt.getText().toString();
            }
        });

        dialogBuilder.setNeutralButton("VIEW ON DCRDATA", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String url = "https://explorer.dcrdata.org/tx/"+txHash;
                if(Dcrwallet.isTestNet()){
                    url = "https://testnet.dcrdata.org/tx/"+txHash;
                }
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
            }
        });

        AlertDialog b = dialogBuilder.create();
        b.show();
        b.getButton(DialogInterface.BUTTON_NEUTRAL).setTextColor(Color.BLUE);
        amount.setText("0");
        address.setText("");
    }
    public void copyToClipboard(String copyText) {
        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if(clipboard != null) {
                clipboard.setText(copyText);
            }
        } else {
            if(getContext() == null){
                System.out.println("Context is null");
                return;
            }
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                    getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData
                    .newPlainText(getString(R.string.your_address), copyText);
            if(clipboard != null)
            clipboard.setPrimaryClip(clip);
        }
        Toast toast = Toast.makeText(getContext(),
                R.string.tx_hash_copy, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM | Gravity.END, 50, 50);
        toast.show();
    }
}
