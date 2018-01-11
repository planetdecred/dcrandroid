package com.decrediton.fragments;


import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.decrediton.Activities.ReaderActivity;
import com.decrediton.Util.Utils;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import com.decrediton.R;

import java.util.ArrayList;

import dcrwallet.Balance;
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
    final IntentIntegrator integrator = new IntentIntegrator(getActivity());
    private static final int SCANNER_ACTIVITY_RESULT_CODE = 0;
    ArrayList<String>  categories;

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
        getActivity().setTitle("Send");

        address = getActivity().findViewById(R.id.send_dcr_add);
        amount = getActivity().findViewById(R.id.send_dcr_amount);
        totalAmountSending= getActivity().findViewById(R.id.send_dcr_total_amt_sndng);
        scanAddress = getActivity().findViewById(R.id.send_dcr_scan);
        estimateSize = getActivity().findViewById(R.id.send_dcr_estimate_size);
        estimateFee = getActivity().findViewById(R.id.send_dcr_estimate_fee);
        sendAll = getActivity().findViewById(R.id.send_dcr_all);
        send= getActivity().findViewById(R.id.send_btn_tx);
        Spinner accountSpinner = view.findViewById(R.id.send_dropdown);
        accountSpinner.setOnItemSelectedListener(this);
        // Spinner Drop down elements
        categories = new ArrayList<>();

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
                            final Balance balance = Dcrwallet.getBalance(0);
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    amount.setText(String.valueOf(balance.getSpendable()));
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
                        Toast.makeText(SendFragment.this.getContext(), "Wallet Address Is Too Short", Toast.LENGTH_SHORT).show();
                        return;
                    }else if(returnString.length() > 36){
                        Toast.makeText(SendFragment.this.getContext(), "Wallet Address Is to Long", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if(returnString.startsWith("D")){
                        address.setText(returnString);
                    }else{
                        Toast.makeText(SendFragment.this.getContext(), "Invalid address prefix", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(getContext(), "This is not a DCR wallet address", Toast.LENGTH_LONG).show();
                    address.setText("");
                }
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String item = parent.getItemAtPosition(position).toString();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
