package com.decrediton.fragments;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.decrediton.Activities.ReaderActivity;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import com.decrediton.R;

import static android.app.Activity.RESULT_OK;

/**
 * Created by Macsleven on 28/11/2017.
 */

public class SendFragment extends android.support.v4.app.Fragment {
   public EditText address,amount;
   public TextView totalAmountSending,estimateFee,estimateSize,scanAddress;
    Button send;
    final IntentIntegrator integrator = new IntentIntegrator(getActivity());
    private static final int SCANNER_ACTIVITY_RESULT_CODE = 0;

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
        send= getActivity().findViewById(R.id.send_btn_tx);

        scanAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               showToast();
                Intent intent = new Intent(getActivity(), ReaderActivity.class);
                startActivityForResult(intent, SCANNER_ACTIVITY_RESULT_CODE);

            }
        });
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


            }
        });

    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == SCANNER_ACTIVITY_RESULT_CODE) {
            if(resultCode== RESULT_OK) {
                try {
                    String returnString = intent.getStringExtra("keyName");
                    if (returnString.startsWith("T")  && returnString.length()> 25 && returnString.length() < 36){
                        address.setText(returnString);
                    }
                    else {
                        Toast.makeText(getContext(), "This is not a valid DCR wallet address", Toast.LENGTH_LONG).show();
                    }

                } catch (Exception e) {
                    Toast.makeText(getContext(), "This is not a DCR wallet address", Toast.LENGTH_LONG).show();
                    address.setText("");

                }
            }

// Handle successful scan
        }
    }

    public void showToast(){
        Toast.makeText(getContext(),"my test",Toast.LENGTH_LONG).show();
    }

}
