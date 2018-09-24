package com.dcrandroid.fragments;


import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.dcrandroid.MainActivity;
import com.dcrandroid.activities.ReaderActivity;
import com.dcrandroid.R;
import com.dcrandroid.data.Account;
import com.dcrandroid.data.Constants;
import com.dcrandroid.dialog.ConfirmTransactionDialog;
import com.dcrandroid.dialog.InfoDialog;
import com.dcrandroid.util.CoinFormat;
import com.dcrandroid.util.DcrConstants;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.math.MathContext;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import mobilewallet.Mobilewallet;
import mobilewallet.UnsignedTransaction;

import static android.app.Activity.RESULT_OK;


/**
 * Created by Macsleven on 28/11/2017.
 */

public class SendFragment extends android.support.v4.app.Fragment implements AdapterView.OnItemSelectedListener {

    public static final String SEND_FRAGMENT = "SendFragment";

    private EditText address, amount, exchangeAmount;
    private TextView estimateSize, error_label, exchangeRateLabel, exchangeUnavailable, equivalentAmount, exchangeCurrency;
    private TextView estimateFee, balanceRemaining, tvSendInDcr, tvSendMax, tvDCR;
    private LinearLayout exchangeDetails, llUSD;
    private Spinner accountSpinner;
    private View border3;
    private static final int SCANNER_ACTIVITY_RESULT_CODE = 0, CONFIRM_TRANSACTION_REQUEST_CODE = 1;
    private List<String> categories;
    private List<Integer> accountNumbers = new ArrayList<>();
    private ArrayAdapter dataAdapter;
    private ProgressDialog pd;
    private PreferenceUtil util;
    private DcrConstants constants = DcrConstants.getInstance();
    private boolean isSendAll = false, currencyIsDCR = true, textChanged = false;
    private String addressError = "", amountError = "";
    private double exchangeRate = -1;
    private BigDecimal exchangeDecimal;
    private DecimalFormat format;
    private Button btnSend;
    private float textSize14;
    private float textSize12;
    private int marginTop35dp;
    private int marginTop48dp;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View vi = inflater.inflate(R.layout.content_send, container, false);

        address = vi.findViewById(R.id.send_dcr_add);
        amount = vi.findViewById(R.id.send_dcr_amount);
        exchangeAmount = vi.findViewById(R.id.send_exchange_amount);
        estimateSize = vi.findViewById(R.id.send_dcr_estimate_size);
        estimateFee = vi.findViewById(R.id.send_dcr_estimate_fee);
        accountSpinner = vi.findViewById(R.id.send_dropdown);
        error_label = vi.findViewById(R.id.send_error_label);
        exchangeRateLabel = vi.findViewById(R.id.send_dcr_exchange_rate);
        balanceRemaining = vi.findViewById(R.id.send_dcr_balance_remain);
        exchangeDetails = vi.findViewById(R.id.exchange_details);
        exchangeUnavailable = vi.findViewById(R.id.rate_unavailable);
        equivalentAmount = vi.findViewById(R.id.equivalent_tv);
        tvSendInDcr = vi.findViewById(R.id.amount_tv);
//        tvTotalSending = vi.findViewById(R.id.tvTotalSending);
        tvSendMax = vi.findViewById(R.id.send_dcr_all);
        btnSend = vi.findViewById(R.id.send_btn_tx);
        tvDCR = vi.findViewById(R.id.tvDCR);
        llUSD = vi.findViewById(R.id.llUSD);
        border3 = vi.findViewById(R.id.border3);
        exchangeCurrency = vi.findViewById(R.id.send_exchange_currency);
        accountSpinner.setOnItemSelectedListener(this);

        vi.findViewById(R.id.send_dcr_scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), ReaderActivity.class);
                startActivityForResult(intent, SCANNER_ACTIVITY_RESULT_CODE);
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String destAddress = address.getText().toString();

                if (destAddress.equals("")) {
                    addressError = "Destination Address can not be empty";
                } else if (!constants.wallet.isAddressValid(destAddress)) {
                    addressError = "Destination Address is not valid";
                } else if (amount.getText().toString().equals("")) {
                    amountError = "Amount cannot be empty";
                }

                if (addressError.length() > 0 || amountError.length() > 0) {
                    displayError(false);
                    return;
                }

                if (!validateAmount(true)) return;

                final long amt = getAmount();

                displayError(true);

                showInputPassPhraseDialog(destAddress, amt);
            }
        });

        //amount.setFilters(new InputFilter[]{new DecredInputFilter()});

        amount.addTextChangedListener(amountWatcher);
        exchangeAmount.addTextChangedListener(exchangeWatcher);
        textSize14 = new RelativeSizeSpan(14F).getSizeChange();
        textSize12 = new RelativeSizeSpan(12F).getSizeChange();


        Resources r = getResources();
        marginTop35dp = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 35, r.getDisplayMetrics()));
        marginTop48dp = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, r.getDisplayMetrics()));

        address.addTextChangedListener(addressWatcher);
        btnSend.setClickable(false);
        exchangeUnavailable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exchangeUnavailable.setText(null);
                new GetExchangeRate(SendFragment.this).execute();
            }
        });


        return vi;
    }

    private TextWatcher addressWatcher = new TextWatcher() {
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
                displayError(false);
            } else if (!constants.wallet.isAddressValid(s.toString())) {
                addressError = "Destination Address is not valid";
                displayError(false);
            } else {
                addressError = "";
                displayError(false);
                constructTransaction();
            }
        }
    };

    private TextWatcher amountWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            textChanged = true;
            isSendAll = false;

            if (s.length() > 0) {
                if (s.toString().substring(0, 1).equals(".")) {
                    s.replace(0, 1, "0.");
                }
                dotCounter(s);


                BigDecimal currentAmount = new BigDecimal(s.toString());
                currentAmount = currentAmount.setScale(9, RoundingMode.HALF_UP);

                String inputDcrFromUser = s.toString();
                Spannable spanAmount = CoinFormat.Companion.format(inputDcrFromUser);

                amount.removeTextChangedListener(amountWatcher);
                amount.setTextSize(textSize14);

                amount.setText(spanAmount);
                amount.setSelection(spanAmount.length());
                amount.addTextChangedListener(amountWatcher);
                changeViewParameters(true);

                if (exchangeDecimal != null) {
                    llUSD.setVisibility(View.VISIBLE);
                    BigDecimal convertedAmount = currentAmount.multiply(exchangeDecimal);
                    String formattedExchangeAmount = format.format(convertedAmount.doubleValue()).replace(",", ".");
                    Spannable spanExchangeAmount = CoinFormat.Companion.format(formattedExchangeAmount);

                    exchangeAmount.removeTextChangedListener(exchangeWatcher);
                    exchangeAmount.setTextSize(textSize14);
                    exchangeAmount.setText(spanExchangeAmount);
                    exchangeAmount.addTextChangedListener(exchangeWatcher);

                }

            } else {
                exchangeAmount.removeTextChangedListener(exchangeWatcher);
                exchangeAmount.setText(null);
                exchangeAmount.addTextChangedListener(exchangeWatcher);
                changeViewParameters(false);
            }

            constructTransaction();
        }
    };

    private TextWatcher exchangeWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (exchangeDecimal == null) {
                return;
            }
            isSendAll = false;
            if (s.length() > 0) {
                if (s.toString().substring(0, 1).equals(".")) {
                    s.replace(0, 1, "0.");
                }
                dotCounter(s);

                BigDecimal currentAmount = new BigDecimal(s.toString());
                currentAmount = currentAmount.setScale(9, RoundingMode.HALF_UP);

                String inputUsdFromUser = s.toString();
                Spannable spanAmount = CoinFormat.Companion.format(inputUsdFromUser);

                exchangeAmount.removeTextChangedListener(exchangeWatcher);
                exchangeAmount.setText(spanAmount);
                exchangeAmount.setTextSize(textSize14);
                exchangeAmount.setSelection(spanAmount.length());
                exchangeAmount.addTextChangedListener(exchangeWatcher);


                BigDecimal convertedAmount = currentAmount.divide(exchangeDecimal, MathContext.DECIMAL128);
                String formattedAmount = format.format(convertedAmount.doubleValue()).replace(",", ".");
                Spannable spanExchangeAmount = CoinFormat.Companion.format(formattedAmount);

                amount.removeTextChangedListener(amountWatcher);
                amount.setText(spanExchangeAmount);
                amount.addTextChangedListener(amountWatcher);
                amount.setTextSize(textSize14);
                changeViewParameters(true);
                btnSend.setClickable(true);
            } else {
                amount.removeTextChangedListener(amountWatcher);
                amount.setText(null);
                btnSend.setClickable(false);
                amount.addTextChangedListener(amountWatcher);
                changeViewParameters(false);
            }
            constructTransaction();
        }
    };

    @SuppressLint("SetTextI18n")
    private void changeViewParameters(boolean textChanged) {
        if (textChanged) {
//            double dAmount = Double.parseDouble(amount.getText().toString());
//            tvTotalSending.setText(String.valueOf(dAmount) + " DCR");
            tvSendMax.setTextColor(getResources().getColor(R.color.orange));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                btnSend.setBackground(ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.btn_shape));
            } else {
                btnSend.setBackgroundDrawable(ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.btn_shape));
            }
            btnSend.setTextColor(getResources().getColor(R.color.buttonWhiteText));

            exchangeCurrency.setVisibility(View.VISIBLE);
            tvDCR.setVisibility(View.VISIBLE);
            tvSendInDcr.setTextColor(getResources().getColor(R.color.secondaryTextColor));
            equivalentAmount.setTextColor(getResources().getColor(R.color.secondaryTextColor));
            btnSend.setClickable(true);
        } else {
//            tvTotalSending.setText(R.string._0_00_dcr);
            tvSendMax.setTextColor(getResources().getColor(R.color.secondaryTextColor54));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                btnSend.setBackground(ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.btn_shape1));
            } else {
                btnSend.setBackgroundDrawable(ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.btn_shape1));
            }
            btnSend.setClickable(false);
            btnSend.setTextColor(getResources().getColor(R.color.black38));
            tvSendInDcr.setTextColor(getResources().getColor(R.color.secondaryTextColor38));
            equivalentAmount.setTextColor(getResources().getColor(R.color.secondaryTextColor38));
            exchangeCurrency.setVisibility(View.GONE);
            tvDCR.setVisibility(View.GONE);
        }

    }

    private void dotCounter(Editable s) {
        int dotCounter = 0;

        for (int i = 0; i < s.toString().length(); i++) {
            char oneLetter = s.toString().charAt(i);

            if (oneLetter == '.') {
                dotCounter++;
            } else if (oneLetter == ',') {
                s.replace(i, i + 1, "");
            }
            if (dotCounter > 1) {
                s.replace(i, i + 1, "");
            }
        }

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FragmentActivity activity = getActivity();
        if (activity == null) {
            System.out.println("Activity is null");
            return;
        }

        util = new PreferenceUtil(activity);
        activity.setTitle(getString(R.string.send));

        // Spinner Drop down elements
        categories = new ArrayList<>();

        if (getContext() == null) {
            System.out.println("Context is null");
            return;
        }
        dataAdapter = new ArrayAdapter<>(getContext(), R.layout.custom_spinner_item, categories);
        dataAdapter.setDropDownViewResource(R.layout.custom_dropdown_item);
        accountSpinner.setAdapter(dataAdapter);

        if (Integer.parseInt(util.get(Constants.CURRENCY_CONVERSION, "0")) != 0) {
            new GetExchangeRate(this).execute();
        }

        tvSendMax.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSendAll = true;
                try {
                    long spendableBalance = getSpendableForSelectedAccount();
                    BigDecimal currentAmount = new BigDecimal(Mobilewallet.amountCoin(spendableBalance));
                    currentAmount = currentAmount.setScale(9, RoundingMode.HALF_UP);
                    if(currentAmount.doubleValue() > 0.00) {
                        changeViewParameters(true);
//                        BigDecimal convertedAmountToDCR = convertedAmountToUSD.divide(exchangeDecimal, MathContext.DECIMAL128);
                        String amountInDCR = format.format(currentAmount.doubleValue()).replace(",", ".");
                        amount.removeTextChangedListener(amountWatcher);
                        amount.setText(amountInDCR);
                        amount.addTextChangedListener(amountWatcher);

                        if (exchangeDecimal != null) {
                            BigDecimal convertedAmountToUSD = currentAmount.multiply(exchangeDecimal, MathContext.DECIMAL128);
                            String amountInUSD = format.format(convertedAmountToUSD.doubleValue()).replace(",", ".");
                            exchangeAmount.removeTextChangedListener(exchangeWatcher);
                            exchangeAmount.setText(amountInUSD);
                            exchangeAmount.addTextChangedListener(exchangeWatcher);

                        }
                        constructTransaction();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        prepareAccounts();

        format = new DecimalFormat();
        format.applyPattern("#.########");
    }

    private void setInvalid() {
        estimateSize.setText(R.string._0_bytes);
        estimateFee.setText(R.string._0_00_dcr);
        balanceRemaining.setText(R.string._0_00_dcr);
    }

    private String getDestinationAddress() {
        String destAddress = address.getText().toString();
        if (destAddress.equals(Constants.EMPTY_STRING)) {
            destAddress = util.get(Constants.RECENT_ADDRESS);
            if (destAddress.equals(Constants.EMPTY_STRING)) {
                try {
                    destAddress = constants.wallet.addressForAccount(0);
                    util.set(Constants.RECENT_ADDRESS, destAddress);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return destAddress;
    }

    private long getAmount() {
        long amt;
        if (currencyIsDCR) {
            amt = Mobilewallet.amountAtom(Double.parseDouble(amount.getText().toString()));
        } else {
            BigDecimal currentAmount = new BigDecimal(amount.getText().toString());
            currentAmount = currentAmount.setScale(9, RoundingMode.HALF_UP);

            BigDecimal exchangeDecimal = new BigDecimal(exchangeRate);
            exchangeDecimal = exchangeDecimal.setScale(9, RoundingMode.HALF_UP);
            BigDecimal convertedAmount = currentAmount.divide(exchangeDecimal, MathContext.DECIMAL128);
            amt = Mobilewallet.amountAtom(convertedAmount.doubleValue());
        }
        return amt;
    }

    private long getSpendableForSelectedAccount() throws Exception {
        return constants.wallet.spendableForAccount(accountNumbers.get(accountSpinner.getSelectedItemPosition()), util.getBoolean(Constants.SPEND_UNCONFIRMED_FUNDS) ? 0 : Constants.REQUIRED_CONFIRMATIONS);
    }

    private void constructTransaction() {
        displayError(true);

        if (!validateAmount(false)) {
            setInvalid();
            return;
        }

        try {
            String destAddress = getDestinationAddress();
            long amt = getAmount();

            UnsignedTransaction transaction = constants.wallet.constructTransaction(destAddress, amt, accountNumbers.get(accountSpinner.getSelectedItemPosition()), util.getBoolean(Constants.SPEND_UNCONFIRMED_FUNDS) ? 0 : Constants.REQUIRED_CONFIRMATIONS, isSendAll);

            double estFee = Mobilewallet.amountCoin(Utils.signedSizeToAtom(transaction.getEstimatedSignedSize()));
            double doubleAmount = Utils.formatDecredToDobule(amt - Utils.signedSizeToAtom(transaction.getEstimatedSignedSize()));

            estimateFee.setText(CoinFormat.Companion.format(estFee));
            estimateSize.setText(String.format(Locale.getDefault(), "%d bytes", transaction.getEstimatedSignedSize()));

            if (isSendAll && doubleAmount > 0.00) {
                balanceRemaining.setText(CoinFormat.Companion.format(getSpendableForSelectedAccount() - transaction.getTotalPreviousOutputAmount()));

                String strAmount = format.format(doubleAmount).replace(",", ".");
                Spannable spanAmount = CoinFormat.Companion.format(strAmount);

                amount.removeTextChangedListener(amountWatcher);
                amount.setText(spanAmount);
                amount.setTextSize(textSize14);
                amount.setSelection(amount.getText().length());
                amount.addTextChangedListener(amountWatcher);

                if (exchangeDecimal != null) {
                    BigDecimal currentAmount = new BigDecimal(Mobilewallet.amountCoin(transaction.getTotalPreviousOutputAmount()
                            - Utils.signedSizeToAtom(transaction.getEstimatedSignedSize())));

                    currentAmount = currentAmount.setScale(9, RoundingMode.HALF_UP);

                    BigDecimal convertedAmount = currentAmount.multiply(exchangeDecimal);

                    String strExchangeAmount = format.format(convertedAmount.doubleValue()).replace(",", ".");
                    Spannable spanExchangeAmount = CoinFormat.Companion.format(strExchangeAmount);

                    exchangeAmount.removeTextChangedListener(exchangeWatcher);
                    exchangeAmount.setText(spanExchangeAmount);
                    exchangeAmount.setSelection(exchangeAmount.getText().length());
                    exchangeAmount.setTextSize(textSize14);
                    exchangeAmount.addTextChangedListener(exchangeWatcher);
                }

            } else {
                balanceRemaining.setText(CoinFormat.Companion.format(getSpendableForSelectedAccount() - amt));
            }

        } catch (final Exception e) {
            setInvalid();
            error_label.setText(Utils.translateError(getContext(), e));
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == SCANNER_ACTIVITY_RESULT_CODE) {
            if (resultCode == RESULT_OK) {
                try {
                    String returnString = intent.getStringExtra(Constants.ADDRESS);
                    System.out.println("Code: " + returnString);
                    if (returnString.startsWith("decred:"))
                        returnString = returnString.replace("decred:", "");
                    if (returnString.length() < 25) {
                        Toast.makeText(SendFragment.this.getContext(), R.string.wallet_add_too_short, Toast.LENGTH_SHORT).show();
                        return;
                    } else if (returnString.length() > 36) {
                        Toast.makeText(SendFragment.this.getContext(), R.string.wallet_addr_too_long, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    //TODO: Make available for mainnet
                    if (returnString.startsWith("T")) {
                        address.setText(returnString);
                    } else {
                        Toast.makeText(SendFragment.this.getContext(), R.string.invalid_address_prefix, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(getContext(), R.string.error_not_decred_address, Toast.LENGTH_LONG).show();
                    address.setText("");
                }
            }
        } else if (requestCode == CONFIRM_TRANSACTION_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                displayOverview();
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (isSendAll) {
            try {
                amount.setText(Utils.formatDecred(constants.wallet.spendableForAccount(accountNumbers.get(accountSpinner.getSelectedItemPosition()), 0)));
                amount.setEnabled(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        constructTransaction();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    private void prepareAccounts() {
        new Thread() {
            public void run() {
                try {
                    final ArrayList<Account> accounts = Account.parse(constants.wallet.getAccounts(util.getBoolean(Constants.SPEND_UNCONFIRMED_FUNDS) ? 0 : Constants.REQUIRED_CONFIRMATIONS));
                    accountNumbers.clear();
                    categories.clear();
                    for (int i = 0; i < accounts.size(); i++) {
                        if (accounts.get(i).getAccountName().trim().equalsIgnoreCase("imported")) {
                            continue;
                        }

                        String coinsInWallet = Utils.formatDecred(accounts.get(i).getBalance().getSpendable()).replace(",", ".");
                        Log.d(SEND_FRAGMENT, "coinsInWallet: " + coinsInWallet);

                        Spannable spanCoinsInWallet = CoinFormat.Companion.format(coinsInWallet);
                        Log.d(SEND_FRAGMENT, "spanCoinsInWallet: " + spanCoinsInWallet);

                        categories.add(i, accounts.get(i).getAccountName() + " [" + spanCoinsInWallet + " DCR" + "]");
                        accountNumbers.add(accounts.get(i).getAccountNumber());
                    }
                    if (getActivity() == null) {
                        System.out.println("Activity is null");
                        return;
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dataAdapter.notifyDataSetChanged();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void startTransaction(final String passphrase, final String destAddress, final long amt) {
        pd = Utils.getProgressDialog(getContext(), false, false, "Processing...");
        pd.show();
        new Thread() {
            public void run() {
                try {
                    int accountNumber = accountNumbers.get(accountSpinner.getSelectedItemPosition());
                    int requiredConfs = util.getBoolean(Constants.SPEND_UNCONFIRMED_FUNDS) ? 0 : Constants.REQUIRED_CONFIRMATIONS;
                    byte[] serializedTx = constants.wallet.sendTransaction(passphrase.getBytes(), destAddress, amt, accountNumber, requiredConfs, isSendAll);
                    List<Byte> hashList = new ArrayList<>();
                    for (byte aSerializedTx : serializedTx) {
                        hashList.add(aSerializedTx);
                    }
                    Collections.reverse(hashList);
                    final StringBuilder sb = new StringBuilder();
                    for (byte b : hashList) {
                        sb.append(String.format(Locale.getDefault(), "%02x", b));
                    }
                    System.out.println("Hash: " + sb.toString());
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (pd.isShowing()) {
                                    pd.dismiss();
                                }
                                addressError = "";
                                prepareAccounts();
                                showTxConfirmDialog(sb.toString());
                            }
                        });
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (pd.isShowing()) {
                                    pd.dismiss();
                                }
                                error_label.setText(Utils.translateError(getContext(), e));
                            }
                        });
                    }
                }
            }
        }.start();
    }

    public void showInputPassPhraseDialog(final String destAddress, final long amt) {
        if (getContext() == null) {
            return;
        }
        Spannable dialogTitle = CoinFormat.Companion.format(
                String.format(Locale.getDefault(), "Sending %s DCR", Utils.removeTrailingZeros(Mobilewallet.amountCoin(amt)))
        );
        InfoDialog infoDialog = new InfoDialog(getContext());
        infoDialog.setDialogTitle(dialogTitle)
                .setMessage("Please confirm to send funds")
                .setIcon(R.drawable.np_amount_withdrawal)
                .setTitleTextColor(Color.parseColor("#2DD8A3"))
                .setMessageTextColor(Color.parseColor("#617386"))
                .setPositiveButton("CONFIRM", null)
                .setNegativeButton("CANCEL", null);
        //.show(); Needs UI Design for send confirmation with passphrase input

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.input_passphrase_box, null);
        dialogBuilder.setCancelable(false);
        dialogBuilder.setView(dialogView);

        final EditText passphrase = dialogView.findViewById(R.id.passphrase_input);

        dialogBuilder.setMessage(String.format(Locale.getDefault(), "%s %s DCR", getString(R.string.transaction_confirmation), Utils.formatDecred(amt)));

        dialogBuilder.setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                final String pass = passphrase.getText().toString();
                if (pass.length() > 0) {
                    int srcAccount = accountNumbers.get(accountSpinner.getSelectedItemPosition());
                    int confs = util.getBoolean(Constants.SPEND_UNCONFIRMED_FUNDS) ? 0 : Constants.REQUIRED_CONFIRMATIONS;
                    try {
                        UnsignedTransaction unsignedTransaction = constants.wallet.constructTransaction(destAddress, getAmount(), srcAccount, confs, isSendAll);
                        ConfirmTransactionDialog transactionDialog = new ConfirmTransactionDialog(getContext())
                                .setAddress(getDestinationAddress())
                                .setAmount(getAmount())
                                .setFee(unsignedTransaction.getEstimatedSignedSize())
                                .setPositiveButton("SEND", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        startTransaction(pass, getDestinationAddress(), getAmount());
                                    }
                                });
                        transactionDialog.show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        dialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });

        AlertDialog b = dialogBuilder.create();
        b.show();
        b.getButton(b.BUTTON_POSITIVE).setTextColor(Color.BLUE);
    }

    public void showTxConfirmDialog(final String txHash) {
        if (getActivity() == null) {
            System.out.println("Activity is null");
            return;
        }
        if (getContext() == null) {
            System.out.println("Context is null");
            return;
        }
        new InfoDialog(getContext())
                .setDialogTitle("Transaction Was Successful")
                .setMessage("HASH:\n" + txHash)
                .setIcon(R.drawable.np_amount_withdrawal)
                .setTitleTextColor(Color.parseColor("#2DD8A3"))
                .setMessageTextColor(Color.parseColor("#2970FF"))
                .setPositiveButton("VIEW", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        displayOverview();
                    }
                }).setNegativeButton("CLOSE", null)
                .setMessageClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        copyToClipboard(txHash);
                    }
                }).show();
        amount.setText(null);
        address.setText("");
        addressError = "";
        amountError = "";
        displayError(false);
    }

    public void copyToClipboard(String copyText) {
        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setText(copyText);
            }
        } else {
            if (getContext() == null) {
                System.out.println("Context is null");
                return;
            }
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                    getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData
                    .newPlainText(getString(R.string.your_address), copyText);
            if (clipboard != null)
                clipboard.setPrimaryClip(clip);
        }
        Toast toast = Toast.makeText(getContext(),
                R.string.tx_hash_copy, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM | Gravity.END, 50, 50);
        toast.show();
    }

    private boolean validateAmount(boolean sending) {
        String s = amount.getText().toString().replace(",", "");

        if (!exchangeAmount.getText().toString().equals("")) {
            String strExchange = exchangeAmount.getText().toString();
            if (strExchange.indexOf('.') != -1) {
                String atoms = strExchange.substring(strExchange.indexOf('.'));
                if (atoms.length() > 9) {
                    amountError = "Amount has more then 8 decimal places";
                    exchangeAmount.setTextSize(textSize12);
                    displayError(false);
                    return false;
                }
            }
        }

        if (s.length() == 0) {
            if (sending) {
                amountError = "Amount is empty";
                displayError(false);
            }
            return false;
        }
        if (s.indexOf('.') != -1) {
            String atoms = s.substring(s.indexOf('.'));
            if (atoms.length() > 9) {
                amountError = "Amount has more then 8 decimal places";
                amount.setTextSize(textSize12);
                displayError(false);
                return false;
            }
        }
        if (Double.parseDouble(s) == 0) {
            if (sending) {
                amountError = "Amount is not valid";
                displayError(false);
            }
            return false;
        }
        if (!amount.getText().toString().equals("")) {
            double dAmountInWallet = 0.0;

            try {
                long spendableBalance = constants.wallet.spendableForAccount(accountNumbers.get(accountSpinner.getSelectedItemPosition()), 0);
                BigDecimal currentAmount = new BigDecimal(Mobilewallet.amountCoin(spendableBalance));
                currentAmount = currentAmount.setScale(9, RoundingMode.HALF_UP);
                dAmountInWallet = currentAmount.doubleValue();

            } catch (Exception e) {
                e.printStackTrace();
            }
            if (dAmountInWallet < Double.parseDouble(s)) {
                amountError = "Insufficient balance";
                displayError(false);
                return false;
            }

        }

        amountError = "";
        displayError(false);
        return true;
    }

    private void displayError(boolean empty) {
        if (empty) {
            error_label.setText(null);
            return;
        }
        String error = addressError + "\n" + amountError;
        error_label.setText(error.trim());
    }

    private static class GetExchangeRate extends AsyncTask<Void, String, String> {

        private SendFragment sendFragment;

        public GetExchangeRate(SendFragment sendFragment) {
            this.sendFragment = sendFragment;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                URL url = new URL(sendFragment.getContext().getString(R.string.dcr_to_usd_exchange_url));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.setReadTimeout(7000);
                connection.setConnectTimeout(7000);
                connection.setRequestProperty("user-agent", sendFragment.util.get("user_agent", ""));
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                StringBuilder result = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    result.append(line);
                }
                br.close();
                connection.disconnect();
                return result.toString();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (sendFragment.getContext() == null) {
                return;
            }

            int index = Integer.parseInt(sendFragment.util.get(Constants.CURRENCY_CONVERSION, "0"));
            String currency = sendFragment.getContext().getResources().getStringArray(R.array.currency_conversion_abbrv)[index];
            String source = sendFragment.getContext().getResources().getStringArray(R.array.currency_conversion_source)[index];

            if (s == null) {
                sendFragment.exchangeUnavailable.setVisibility(View.VISIBLE);
                sendFragment.exchangeUnavailable.setText(String.format("%s rate unavailable (tap to retry)", source));
                return;
            }
            try {
                sendFragment.exchangeUnavailable.setVisibility(View.GONE);
                JSONObject apiResult = new JSONObject(s);
                if (apiResult.getBoolean("success")) {
                    JSONObject result = apiResult.getJSONObject("result");
                    sendFragment.exchangeRate = result.getDouble("Last");
                    sendFragment.exchangeDecimal = new BigDecimal(sendFragment.exchangeRate)
                            .setScale(9, RoundingMode.HALF_UP);
                    sendFragment.exchangeRateLabel.setText(String.format(Locale.getDefault(), "%s %s/DCR (%s)", sendFragment.format.format(sendFragment.exchangeDecimal.doubleValue()), currency, source));
                    sendFragment.exchangeDetails.setVisibility(View.VISIBLE);
                    sendFragment.llUSD.setVisibility(View.VISIBLE);

                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) sendFragment.tvSendMax.getLayoutParams();
                    params.setMargins(0, sendFragment.marginTop35dp, 0, 0);

                    RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) sendFragment.border3.getLayoutParams();
                    layoutParams.setMargins(0, sendFragment.marginTop48dp, 0, 0);


                    if (sendFragment.amount.getText().length() != 0) {
                        BigDecimal currentAmount = new BigDecimal(sendFragment.amount.getText().toString());
                        currentAmount = currentAmount.setScale(9, RoundingMode.HALF_UP);

                        BigDecimal convertedAmount = currentAmount.multiply(sendFragment.exchangeDecimal);
                        sendFragment.exchangeAmount.removeTextChangedListener(sendFragment.exchangeWatcher);
                        sendFragment.exchangeAmount.setText(sendFragment.format.format(convertedAmount.doubleValue()));
                        sendFragment.exchangeAmount.addTextChangedListener(sendFragment.exchangeWatcher);
                    }

                } else {
                    Toast.makeText(sendFragment.getContext(), "Exchange failed with error: " + apiResult.getString("message"), Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void displayOverview() {
        if (getActivity() != null && getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.displayOverview();
        }
    }
}
