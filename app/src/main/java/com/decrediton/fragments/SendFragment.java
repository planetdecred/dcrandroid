package com.decrediton.fragments;


import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
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

import com.decrediton.Activities.ReaderActivity;
import com.decrediton.Activities.WebviewActivity;
import com.decrediton.Util.AccountResponse;
import com.decrediton.Util.Utils;
import com.google.zxing.integration.android.IntentIntegrator;

import com.decrediton.R;

import java.util.ArrayList;
import java.util.Arrays;
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
    final IntentIntegrator integrator = new IntentIntegrator(getActivity());
    private static final int SCANNER_ACTIVITY_RESULT_CODE = 0;
    List<String> categories;
    List<Integer> accountNumbers = new ArrayList<>();
    ArrayAdapter dataAdapter;
    ProgressDialog pd;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //returning our layout file
        //change R.layout.yourlayoutfilename for each of your fragments
        return inflater.inflate(R.layout.content_send, container, false);
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //you can set the title for your toolbar here for different fragments different titles
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

        dataAdapter = new ArrayAdapter(getContext(), android.R.layout.simple_spinner_item, categories);
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
                final long amt = (Long.parseLong(amnt) * (long) 1e8);
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
                send.setEnabled(false);
                showInputPassPhraseDialog(amt);
                new Thread(){
                    public void run(){
                        try {
                            System.out.println("Amount: "+amt);
                            final ConstructTxResponse response = Dcrwallet.constructTransaction(destAddress, amt, accountNumbers.get(accountSpinner.getSelectedItemPosition()));
                            byte[] tx = Dcrwallet.signTransaction(response.getUnsignedTransaction(),"c");
                            byte[] serializedTx = Dcrwallet.publishTransaction(tx);
                            List<Byte> hashList = new ArrayList<>();
                            for (byte aSerializedTx : serializedTx) {
                                hashList.add(aSerializedTx);
                            }
                            Collections.reverse(hashList);
                            StringBuilder sb = new StringBuilder();
                            for(byte b : hashList){
                                sb.append(String.format(Locale.getDefault(),"%02x", b));
                            }
                            System.out.println("Hash: "+sb.toString());
                            String string = Dcrwallet.runDcrCommands("gettransaction "+sb.toString());
                            System.out.println("Transaction: "+string);
                            //String hash = Dcrwallet.decodeRawTransaction(serializedTx);
                            //System.out.println("Hash: "+hash);
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    send.setEnabled(true);
                                }
                            });
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }.start();
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
                                    amount.setText(String.format(Locale.getDefault(),"%f",balance.getSpendable()/ AccountResponse.SATOSHI));
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
        final long amt = (Long.parseLong(amnt) * (long) 1e8);
        if(destAddress.equals("")){
            return;
        }else if(!validateAddress(destAddress)){
            return;
        }else if(amt <= 0){
            estimateSize.setText("0 bytes");
            totalAmountSending.setText("0.000000000 DCR");
            estimateFee.setText("0.00000000 DCR");
            return;
        }
        new Thread(){
            public void run(){
                try{
                    System.out.println("Amount: "+amt);
                    final ConstructTxResponse response = Dcrwallet.constructTransaction(destAddress, amt, accountNumbers.get(accountSpinner.getSelectedItemPosition()));
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            double totalAmount = (amt + (response.getEstimatedSignedSize() / 0.001)) / 1e8;
                            System.out.println("Fee: "+(response.getEstimatedSignedSize() / 0.001));
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
        pd = Utils.getProgressDialog(SendFragment.this.getContext(), false,false,"Getting Accounts...");
        pd.show();
        new Thread(){
            public void run(){
                try{
                    final AccountResponse response = AccountResponse.parse(Dcrwallet.getAccounts());
                    if(response.errorOccurred){
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(pd.isShowing()){
                                    pd.dismiss();
                                }
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
                        categories.add(i, response.items.get(i).name);
                        accountNumbers.add(response.items.get(i).number);
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(pd.isShowing()){
                                pd.dismiss();
                            }
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

    public void showInputPassPhraseDialog(float amt) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.input_passphrase_box, null);
        dialogBuilder.setCancelable(false);
        dialogBuilder.setView(dialogView);

        final EditText passphrase = (EditText) dialogView.findViewById(R.id.passphrase_input);

        dialogBuilder.setMessage(getString(R.string.transaction_confirmation)+String.format(Locale.getDefault(),"%.8f DCR", amt/1e8));
        dialogBuilder.setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                passphrase.getText().toString();

                //do something with edt.getText().toString();
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
}
