package com.dcrandroid.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.dcrandroid.R;
import com.dcrandroid.util.AccountResponse;
import com.dcrandroid.util.DcrResponse;
import com.dcrandroid.util.EncodeQrCode;
import com.dcrandroid.util.PreferenceUtil;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import dcrwallet.Dcrwallet;

/**
 * Created by Macsleven on 28/11/2017.
 */

public class ReceiveFragment extends android.support.v4.app.Fragment implements AdapterView.OnItemSelectedListener{
    ImageView imageView;
    private TextView address;
    ProgressDialog pd;
    ArrayAdapter dataAdapter;
    List<String> categories;
    PreferenceUtil preferenceUtil;
    List<Integer> accountNumbers = new ArrayList<>();
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //returning our layout file
        preferenceUtil = new PreferenceUtil(getContext());
        //change R.layout.yourlayoutfilename for each of your fragments
        View rootView = inflater.inflate(R.layout.content_receive, container, false);
        LayoutInflater layoutInflater = LayoutInflater.from(rootView.getContext());
        imageView = rootView.findViewById(R.id.bitm);
        address = rootView.findViewById(R.id.barcode_address);
        Button buttonGenerate = rootView.findViewById(R.id.btn_gen_new_addr);
        final Spinner accountSpinner = rootView.findViewById(R.id.recieve_dropdown);
        accountSpinner.setOnItemSelectedListener(this);
        // Spinner Drop down elements
        categories = new ArrayList<>();

        dataAdapter = new ArrayAdapter(getContext(), android.R.layout.simple_spinner_item, categories);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        accountSpinner.setAdapter(dataAdapter);

        address.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                copyToClipboard(address.getText().toString());
            }
        });
        imageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                copyToClipboard(address.getText().toString());
                return true;
            }
        });
        buttonGenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getAddress(accountNumbers.get(accountSpinner.getSelectedItemPosition()));
            }
        });
        //you can set the title for your toolbar here for different fragments different titles
        getActivity().setTitle(getString(R.string.receive));
        prepareAccounts();
        return rootView;
    }

    public void copyToClipboard(String copyText) {
        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(copyText);
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                    getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData
                    .newPlainText(getString(R.string.your_address), copyText);
            clipboard.setPrimaryClip(clip);
        }
        Toast toast = Toast.makeText(getContext(),
                R.string.your_address_is_copied, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER | Gravity.END, 50, 50);
        toast.show();
    }

    private void prepareAccounts(){
        //pd = Utils.getProgressDialog(ReceiveFragment.this.getContext(), false,false,getString(R.string.getting_accounts));
        //pd.show();
        new Thread(){
            public void run(){
                try{
                    final AccountResponse response = AccountResponse.parse(Dcrwallet.getAccounts());
                    if(response.errorOccurred){
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
//                                if(pd.isShowing()){
//                                    pd.dismiss();
//                                }
                                Toast.makeText(ReceiveFragment.this.getContext(),response.errorMessage,Toast.LENGTH_SHORT).show();
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

    private void getAddress(final int accountNumber){
//        pd = Utils.getProgressDialog(ReceiveFragment.this.getContext(), false,false,getString(R.string.getting_address));
//        pd.show();
        new Thread(){
            public void run(){
                try {
                    final DcrResponse response = DcrResponse.parse(Dcrwallet.nextAddress((long) accountNumber));
                    if(getActivity() == null){
                        return;
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(response.errorOccurred){
                                Toast.makeText(ReceiveFragment.this.getContext(),getString(R.string.error_occured_getting_address)+accountNumber+"\n"+response.content,Toast.LENGTH_SHORT).show();
                            }else{
                                //float a = 0/0;
                                String newAddress = response.content;
                                preferenceUtil.set("recent_address",newAddress);
                                address.setText(newAddress);
                                imageView.setImageBitmap(EncodeQrCode.encodeToQrCode("decred:"+newAddress,200,200));
                            }
//                            if(pd.isShowing()){
//                                pd.dismiss();
//                            }
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        //String item = parent.getItemAtPosition(position).toString();
        getAddress(accountNumbers.get(position));
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
