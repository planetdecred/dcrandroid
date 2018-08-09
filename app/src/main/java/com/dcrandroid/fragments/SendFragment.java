package com.dcrandroid.fragments;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.dcrandroid.MainActivity;
import com.dcrandroid.activities.ReaderActivity;
import com.dcrandroid.R;
import com.dcrandroid.data.Constants;
import com.dcrandroid.util.AccountResponse;
import com.dcrandroid.util.BlockedSelectionEditText;
import com.dcrandroid.util.DcrConstants;
import com.dcrandroid.util.DecredInputFilter;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.MathContext;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import mobilewallet.ConstructTxResponse;

import static android.app.Activity.RESULT_OK;

/**
 * Created by Macsleven on 28/11/2017.
 */

public class SendFragment extends android.support.v4.app.Fragment implements AdapterView.OnItemSelectedListener{

    private EditText address;
    private BlockedSelectionEditText amount;
    private TextView totalAmountSending,estimateFee,estimateSize,error_label, exchangeRateLabel, exchangeCurrency, inputCurrencyDisplay;
    private Spinner accountSpinner;
    private static final int SCANNER_ACTIVITY_RESULT_CODE = 0;
    private List<String> categories;
    private List<Integer> accountNumbers = new ArrayList<>();
    private ArrayAdapter dataAdapter;
    private ProgressDialog pd;
    private PreferenceUtil util;
    private DcrConstants constants;
    private boolean isSendAll = false, currencyIsDCR = true;
    private String addressError = "", amountError = "";
    private double exchangeRate = -1;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        constants = DcrConstants.getInstance();
        return inflater.inflate(R.layout.content_send, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //you can set the title for your toolbar here for different fragments different titles
        if (getActivity() == null) {
            System.out.println("Activity is null");
            return;
        }
        util = new PreferenceUtil(getActivity());
        getActivity().setTitle(getString(R.string.send));

        address = getActivity().findViewById(R.id.send_dcr_add);
        amount = getActivity().findViewById(R.id.send_dcr_amount);
        totalAmountSending = getActivity().findViewById(R.id.send_dcr_total_amt_sndng);
        estimateSize = getActivity().findViewById(R.id.send_dcr_estimate_size);
        estimateFee = getActivity().findViewById(R.id.send_dcr_estimate_fee);
        accountSpinner = view.findViewById(R.id.send_dropdown);
        error_label = getActivity().findViewById(R.id.send_error_label);
        exchangeRateLabel = getActivity().findViewById(R.id.send_dcr_exchange_rate);
        exchangeCurrency = getActivity().findViewById(R.id.send_dcr_exchange_currency);
        inputCurrencyDisplay = getActivity().findViewById(R.id.input_currency_display);
        accountSpinner.setOnItemSelectedListener(this);
        // Spinner Drop down elements
        categories = new ArrayList<>();
        if (getContext() == null) {
            System.out.println("Context is null");
            return;
        }
        dataAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, categories);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        accountSpinner.setAdapter(dataAdapter);

        amount.setFilters(new InputFilter[]{new DecredInputFilter()});

        getActivity().findViewById(R.id.send_dcr_scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), ReaderActivity.class);
                startActivityForResult(intent, SCANNER_ACTIVITY_RESULT_CODE);
            }
        });

        address.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().equals("")) {
                    addressError = "Destination Address can not be empty";
                    displayError();
                } else if (!constants.wallet.isAddressValid(s.toString())) {
                    addressError = "Destination Address is not valid";
                    displayError();
                }else{
                    addressError = "";
                    displayError();
                    constructTransaction();
                }
            }
        });

        amount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                constructTransaction();
            }
        });

        getActivity().findViewById(R.id.send_btn_tx).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (address.getText().toString().equals("")) {
                    addressError  = "Destination Address can not be empty";
                }
                if (amount.getText().toString().equals("")) {
                    amountError = "Amount cannot be empty";
                }
                if(addressError.length() > 0 || amountError.length() > 0){
                    displayError();
                    return;
                }
                final String destAddress = address.getText().toString();
                final long amt;
                if(currencyIsDCR){
                    amt = Utils.decredToAtom(amount.getText().toString());
                }else{
                    BigDecimal currentAmount = new BigDecimal(amount.getText().toString());
                    BigDecimal convertedAmount = currentAmount.divide(BigDecimal.valueOf(exchangeRate), new MathContext(7));
                    amt = Utils.decredToAtom(convertedAmount.toString());
                }
                if (!constants.wallet.isAddressValid(destAddress)) {
                    addressError = "Destination Address is not valid";
                }
                if (amt == 0 || !validateAmount()) {
                    amountError = "Amount is not valid";
                }
                if(addressError.length() > 0 || amountError.length() > 0){
                    displayError();
                    return;
                }
                amountError = "";
                addressError = "";
                displayError();
                showInputPassPhraseDialog(destAddress, amt);
            }
        });

        getActivity().findViewById(R.id.send_dcr_all).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isSendAll) {
                    isSendAll = false;
                    amount.setEnabled(true);
                    ((TextView) v).setTextColor(Color.parseColor("#000000"));
                    constructTransaction();
                } else {
                    isSendAll = true;
                    try {
                        if(currencyIsDCR) {
                            amount.setText(Utils.formatDecred(constants.wallet.spendableForAccount(accountNumbers.get(accountSpinner.getSelectedItemPosition()), util.getBoolean(Constants.KEY_SPEND_UNCONFIRMED_FUNDS) ? 0 : Constants.REQUIRED_CONFIRMATIONS)));
                        }else{
                            BigDecimal currentAmount = new BigDecimal(Utils.formatDecred(constants.wallet.spendableForAccount(accountNumbers.get(accountSpinner.getSelectedItemPosition()), util.getBoolean(Constants.KEY_SPEND_UNCONFIRMED_FUNDS) ? 0 : Constants.REQUIRED_CONFIRMATIONS)), new MathContext(7));
                            BigDecimal convertedAmount = currentAmount.multiply(BigDecimal.valueOf(exchangeRate), new MathContext(7));
                            amount.setText(convertedAmount.toString());
                        }
                        amount.setEnabled(false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    ((TextView) v).setTextColor(Color.parseColor("#2970FF"));
                }
            }
        });
        if(Integer.parseInt(util.get(Constants.CURRENCY_CONVERSION, "0") ) != 0 ){
            getActivity().findViewById(R.id.exchange_details).setVisibility(View.VISIBLE);
            Button convertBtn = getActivity().findViewById(R.id.send_btn_convert);
            convertBtn.setVisibility(View.VISIBLE);
            convertBtn.setEnabled(true);
            convertBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    exchangeCurrency();
                }
            });
        }
        else {
            Button convertBtn = getActivity().findViewById(R.id.send_btn_convert);
            convertBtn.setVisibility(View.VISIBLE);
            convertBtn.setEnabled(false);
        }
        prepareAccounts();
    }

    private void constructTransaction(){
        estimateSize.setText("-");
        totalAmountSending.setText("-");
        estimateFee.setText("-");
        addressError = "";
        amountError = "";
        displayError();
        if(amount.getText().toString().length() == 0){
            return;
        }else if(!validateAmount()){
            return;
        }
        
        new Thread(){
            public void run(){
                if(getActivity() == null){
                    System.out.println("Activity is null");
                    return;
                }
                try {
                    String destAddress = address.getText().toString();
                    final long amt;
                    if(currencyIsDCR){
                        amt = Utils.decredToAtom(amount.getText().toString());
                    }else{
                        BigDecimal currentAmount = new BigDecimal(amount.getText().toString());
                        BigDecimal convertedAmount = currentAmount.divide(BigDecimal.valueOf(exchangeRate), new MathContext(7));
                        amt = Utils.decredToAtom(convertedAmount.toString());
                    }
                    if (destAddress.equals("")){
                        destAddress = util.get(Constants.KEY_RECENT_ADDRESS);
                        if(destAddress.equals("")){
                            try {
                                destAddress = constants.wallet.addressForAccount(0);
                                util.set(Constants.KEY_RECENT_ADDRESS, destAddress);
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    }else if(amt <= 0){
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                estimateSize.setText("-");
                                totalAmountSending.setText("-");
                                estimateFee.setText("-");
                            }
                        });
                        return;
                    }
                    final ConstructTxResponse response = constants.wallet.constructTransaction(destAddress, amt, accountNumbers.get(accountSpinner.getSelectedItemPosition()), util.getBoolean(Constants.KEY_SPEND_UNCONFIRMED_FUNDS) ? 0 : Constants.REQUIRED_CONFIRMATIONS, isSendAll);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            double estFee = 0.001 * response.getEstimatedSignedSize() / 1000;
                            estimateSize.setText(String.format(Locale.getDefault(),"%d bytes",response.getEstimatedSignedSize()));
                            totalAmountSending.setText(Utils.calculateTotalAmount(amt, response.getEstimatedSignedSize(), isSendAll).concat(" DCR"));
                            estimateFee.setText(Utils.formatDecred((float) estFee).concat(" DCR"));
                        }
                    });
                }catch (final Exception e){
                    e.printStackTrace();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            error_label.setText(e.getMessage().substring(0, 1).toUpperCase() + e.getMessage().substring(1));
                        }
                    });
                }
            }
        }.start();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == SCANNER_ACTIVITY_RESULT_CODE) {
            if(resultCode== RESULT_OK) {
                try {
                    String returnString = intent.getStringExtra("keyName");
                    System.out.println("Code: "+returnString);
                    if(returnString.startsWith("decred:"))
                        returnString = returnString.replace("decred:","");
                    if(returnString.length() < 25){
                        Toast.makeText(SendFragment.this.getContext(), R.string.wallet_add_too_short, Toast.LENGTH_SHORT).show();
                        return;
                    }else if(returnString.length() > 36){
                        Toast.makeText(SendFragment.this.getContext(), R.string.wallet_addr_too_long, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    //TODO: Make available for mainnet
                    if(returnString.startsWith("T")){
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
        if(isSendAll){
            try {
                amount.setText(Utils.formatDecred(constants.wallet.spendableForAccount(accountNumbers.get(accountSpinner.getSelectedItemPosition()), 0)));
                amount.setEnabled(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        constructTransaction();
    }

    private void prepareAccounts(){
        new Thread(){
            public void run(){
                try{
                    final AccountResponse response = AccountResponse.parse(constants.wallet.getAccounts(util.getBoolean(Constants.KEY_SPEND_UNCONFIRMED_FUNDS) ? 0 : Constants.REQUIRED_CONFIRMATIONS));
                    if(response.errorOccurred){
                        if(getActivity() == null){
                            System.out.println("Activity is null");
                            return;
                        }
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
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
                        categories.add(i, response.items.get(i).name + " " + Utils.formatDecred(response.items.get(i).balance.spendable));
                        accountNumbers.add(response.items.get(i).number);
                    }
                    if(getActivity() == null){
                        System.out.println("Activity is null");
                        return;
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dataAdapter.notifyDataSetChanged();
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
                    final ConstructTxResponse response = constants.wallet.constructTransaction(destAddress, amt, accountNumbers.get(accountSpinner.getSelectedItemPosition()), util.getBoolean(Constants.KEY_SPEND_UNCONFIRMED_FUNDS) ? 0 : Constants.REQUIRED_CONFIRMATIONS, isSendAll);
                    byte[] tx = constants.wallet.signTransaction(response.getUnsignedTransaction(),passphrase.getBytes());
                    byte[] serializedTx = constants.wallet.publishTransaction(tx);
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
                                }addressError = "";
                                showTxConfirmDialog(sb.toString());
                            }
                        });
                    }
                }catch (final Exception e){
                    e.printStackTrace();
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(pd.isShowing()){
                                    pd.dismiss();
                                }
                                error_label.setText(e.getMessage().substring(0, 1).toUpperCase() + e.getMessage().substring(1));
                            }
                        });
                    }
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

        final EditText passphrase = dialogView.findViewById(R.id.passphrase_input);

        dialogBuilder.setMessage(getString(R.string.transaction_confirmation)+String.format(Locale.getDefault()," %f DCR", amt/1e8));
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
                setMainA();
                //do something with edt.getText().toString();
            }
        });

        dialogBuilder.setNeutralButton("VIEW ON DCRDATA", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String url = "https://testnet.dcrdata.org/tx/"+txHash;
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

    private boolean validateAmount(){
        String s = amount.getText().toString();
        if(s.indexOf('.') != -1){
            String atoms = s.substring(s.indexOf('.'));
            if(atoms.length() > 9){
                addressError = "Amount has more then 8 decimal places";
                displayError();
                return false;
            }
        }
        addressError = "";
        displayError();
        return true;
    }

    private void displayError() {
        String error = addressError + "\n" + amountError;
        error_label.setText(error.trim());
    }

    private void exchangeCurrency(){
        if(exchangeRate == -1){
            new GetExchangeRate(Utils.getProgressDialog(getContext(), false, false, "Fetching Data")).execute();
            return;
        }
        String exchangerateTemp = getContext().getResources().getStringArray(R.array.currency_conversion_symbols)[Integer.parseInt(util.get(Constants.CURRENCY_CONVERSION, "0"))]+ exchangeRate + getContext().getResources().getStringArray(R.array.currency_conversion)[Integer.parseInt(util.get(Constants.CURRENCY_CONVERSION, "0"))] + "/DCR";
        exchangeRateLabel.setText(exchangerateTemp);
        exchangeCurrency.setText(getContext().getResources().getStringArray(R.array.currency_conversion)[Integer.parseInt(util.get(Constants.CURRENCY_CONVERSION, "0"))]);
        if(currencyIsDCR){
            //Using if dcr is true because it will be flipped later in the function
            inputCurrencyDisplay.setText(getContext().getResources().getStringArray(R.array.currency_conversion_symbols)[Integer.parseInt(util.get(Constants.CURRENCY_CONVERSION, "0"))]);
        }else{
            inputCurrencyDisplay.setText("");
        }
        if(amount.getText().toString().length() == 0){
            currencyIsDCR = !currencyIsDCR;
            return;
        }
        BigDecimal currentAmount = new BigDecimal(amount.getText().toString());
        DecimalFormat format = new DecimalFormat();
        format.applyPattern("#.########");
        if(currencyIsDCR){
            BigDecimal convertedAmount = currentAmount.multiply(BigDecimal.valueOf(exchangeRate), new MathContext(7));
            currencyIsDCR = !currencyIsDCR;
            amount.setText(format.format(convertedAmount.doubleValue()));
        }else{
            BigDecimal convertedAmount = currentAmount.divide(BigDecimal.valueOf(exchangeRate), new MathContext(7));
            currencyIsDCR = !currencyIsDCR;
            amount.setText(format.format(convertedAmount.doubleValue()));
        }
    }

    private class GetExchangeRate extends AsyncTask<Void, String, String>{

        private ProgressDialog pd;
        public GetExchangeRate(ProgressDialog pd){
            this.pd = pd;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd.show();
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                URL url = new URL(pd.getContext().getString(R.string.dcr_to_usd_exchange_url));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.setReadTimeout(7000);
                connection.setConnectTimeout(7000);
                connection.setRequestProperty("user-agent",util.get("user_agent",""));
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                StringBuilder result = new StringBuilder();
                while((line = br.readLine()) != null){
                    result.append(line);
                }
                br.close();
                connection.disconnect();
                return result.toString();
            } catch (MalformedURLException e) {
                e.printStackTrace();
                publishProgress(e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
                publishProgress(e.getMessage());
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            Toast.makeText(pd.getContext(), values[0], Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if(pd.isShowing()){
                pd.dismiss();
            }
            if(s == null){
                return;
            }
            try {
                JSONObject apiResult = new JSONObject(s);
                if(apiResult.getBoolean("success")){
                    JSONObject result = apiResult.getJSONObject("result");
                    exchangeRate = result.getDouble("Last");
                    exchangeCurrency();
                }else{
                    Toast.makeText(pd.getContext(), "Exchange failed with error: "+apiResult.getString("message"), Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void setMainA() {
        MainActivity mainActivity = (MainActivity) getActivity();
        mainActivity.displaySelectedScreen(R.id.nav_overview);
    }
}
