/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.fragments;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dcrandroid.R;
import com.dcrandroid.activities.EnterPassCode;
import com.dcrandroid.data.Constants;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.Utils;
import com.dcrandroid.util.WalletData;

import org.jetbrains.annotations.Nullable;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import dcrlibwallet.Dcrlibwallet;
import dcrlibwallet.LibWallet;

import static android.app.Activity.RESULT_OK;

public class SecurityFragment extends Fragment {
    private final int PASSCODE_REQUEST_CODE = 1;
    private EditText etAddress, etMessage, etSignature;
    private TextView tvValidateAddress, tvRequiredMessage, tvRequiredSignature;
    private Button btnSignMessage, btnCopy;
    private LinearLayout layout;
    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals(Constants.SYNCED)) {
                if (!WalletData.getInstance().syncing) {
                    layout.setVisibility(View.VISIBLE);
                } else {
                    layout.setVisibility(View.GONE);
                }
            }
        }
    };
    private LibWallet wallet;
    private PreferenceUtil util;
    private ProgressDialog pd;
    private TextWatcher validateAddressWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            checkCopyButton();
            verifyMessage();
            if (s.toString().trim().equals("")) {
                tvValidateAddress.setText(null);
                toggleMessageButton(false);
                return;
            }

            if (wallet.isAddressValid(s.toString().trim())) {
                if (wallet.haveAddress(s.toString().trim())) {
                    tvValidateAddress.setTextColor(getActivity().getApplicationContext().getResources().getColor(R.color.bluePendingTextColor));
                    tvValidateAddress.setText(R.string.owned_validate_address);
                    toggleMessageButton(true);
                    return;
                }
                tvValidateAddress.setTextColor(getActivity().getApplicationContext().getResources().getColor(R.color.greenTextColor));
                tvValidateAddress.setText(Html.fromHtml(getString(R.string.external_validate_address)));
                toggleMessageButton(false);
            } else {
                tvValidateAddress.setTextColor(Color.RED);
                tvValidateAddress.setText(R.string.invalid_address);
                toggleMessageButton(false);
            }
        }
    };
    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            checkCopyButton();
            verifyMessage();

            if (etSignature.getText().toString().equals("") && wallet.isAddressValid(etAddress.getText().toString())) {
                toggleMessageButton(true);
            } else {
                toggleMessageButton(false);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View vi = inflater.inflate(R.layout.fragment_security, container, false);

        btnCopy = vi.findViewById(R.id.btn_copy);
        btnSignMessage = vi.findViewById(R.id.btn_sign_message);

        etAddress = vi.findViewById(R.id.et_address);
        etAddress.setTextIsSelectable(true);
        etMessage = vi.findViewById(R.id.et_sign_message);
        etSignature = vi.findViewById(R.id.et_verify_signature);

        tvValidateAddress = vi.findViewById(R.id.tv_validate_address);
        tvRequiredMessage = vi.findViewById(R.id.tv_required_sign_message);
        tvRequiredSignature = vi.findViewById(R.id.tv_required_verify_signature);

        layout = vi.findViewById(R.id.security_layout);

        return vi;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getActivity() == null || getContext() == null)
            return;

        getActivity().setTitle(R.string.security);

        util = new PreferenceUtil(getContext());
        wallet = WalletData.getInstance().wallet;
        etAddress.addTextChangedListener(validateAddressWatcher);
        etMessage.addTextChangedListener(textWatcher);
        etSignature.addTextChangedListener(textWatcher);

        btnCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!wallet.isAddressValid(etAddress.getText().toString().trim())) {
                    return;
                } else if (etSignature.getText().toString().equals("")) {
                    tvRequiredSignature.setTextColor(Color.RED);
                    tvRequiredSignature.setText(R.string.this_field_is_required);
                    return;
                }

                String text = getString(R.string.address_colon) + " " + etAddress.getText().toString().trim()
                        + "\n" + getString(R.string.message_colon) + " " + etMessage.getText().toString()
                        + "\n" + getString(R.string.signature_colon) + " " + etSignature.getText().toString();

                Utils.copyToClipboard(v.getContext(), text, getString(R.string.copied_successfully));
            }
        });

        btnSignMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!wallet.isAddressValid(etAddress.getText().toString().trim())) {
                    return;
                } else if (etMessage.getText().toString().equals("")) {
                    tvRequiredMessage.setVisibility(View.VISIBLE);
                }

                tvRequiredMessage.setVisibility(View.INVISIBLE);

                if (!etSignature.getText().toString().trim().equals("")) {
                    verifyMessage();
                    return;
                }

                if (getContext() == null) {
                    return;
                }

                if (util.get(Constants.SPENDING_PASSPHRASE_TYPE).equals(Constants.PIN)) {
                    Intent intent = new Intent(getContext(), EnterPassCode.class);
                    startActivityForResult(intent, PASSCODE_REQUEST_CODE);
                    return;
                }

                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
                LayoutInflater inflater = getActivity().getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.input_passphrase_box, null);
                dialogBuilder.setCancelable(false);
                dialogBuilder.setView(dialogView);
                dialogBuilder.setTitle(R.string.confirmation_required);

                final EditText passphrase = dialogView.findViewById(R.id.passphrase_input);

                dialogBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (passphrase.getText().toString().equals("")) {
                            Toast.makeText(SecurityFragment.this.getContext(), R.string.passphrase_is_empty, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        pd = Utils.getProgressDialog(getContext(), false, false, "Signing...");
                        pd.show();
                        new Thread() {
                            public void run() {
                                signMessage(passphrase.getText().toString().getBytes());
                            }
                        }.start();

                    }
                });

                dialogBuilder.setNegativeButton(android.R.string.cancel, null);

                AlertDialog b = dialogBuilder.create();
                b.show();
                b.getButton(b.BUTTON_POSITIVE).setTextColor(Color.BLUE);
            }
        });

        if (WalletData.getInstance().syncing) {
            layout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PASSCODE_REQUEST_CODE && resultCode == RESULT_OK) {
            pd = Utils.getProgressDialog(getContext(), false, false, "Signing...");
            pd.show();
            new Thread() {
                public void run() {
                    signMessage(data.getStringExtra(Constants.PASSPHRASE).getBytes());
                }
            }.start();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.security_page_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.clear_fields) {
            etSignature.setText(null);
            etAddress.setText(null);
            etMessage.setText(null);
            tvValidateAddress.setText(null);
            tvRequiredSignature.setText(null);
            toggleMessageButton(false);
        }
        return super.onOptionsItemSelected(item);
    }

    private void verifyMessage() {
        String address = etAddress.getText().toString().trim();
        String signature = etSignature.getText().toString();
        String message = etMessage.getText().toString();

        if (signature.equals("")) {
            tvRequiredSignature.setText(null);
            return;
        }

        if (wallet.isAddressValid(address)) {
            try {
                boolean valid = wallet.verifyMessage(address, message, signature);
                if (valid) {
                    tvRequiredSignature.setTextColor(getContext().getResources().getColor(R.color.bluePendingTextColor));
                    tvRequiredSignature.setText(R.string.valid_signature);
                } else {
                    tvRequiredSignature.setTextColor(Color.RED);
                    tvRequiredSignature.setText(R.string.invalid_signature);
                }
            } catch (Exception e) {
                e.printStackTrace();
                tvRequiredSignature.setTextColor(Color.RED);
                tvRequiredSignature.setText(R.string.invalid_signature);
            }
        } else {
            tvRequiredSignature.setTextColor(Color.RED);
            tvRequiredSignature.setText(R.string.invalid_signature);
        }
    }

    private void signMessage(byte[] passphrase) {
        final String address = etAddress.getText().toString().trim();
        final String message = etMessage.getText().toString();
        try {
            final byte[] signature = wallet.signMessage(passphrase, address, message);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    etSignature.setText(Dcrlibwallet.encodeBase64(signature));
                    tvRequiredSignature.setText(null);
                    pd.dismiss();
                    verifyMessage();
                }
            });
        } catch (final Exception e) {
            e.printStackTrace();
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pd.dismiss();
                    tvRequiredSignature.setTextColor(Color.RED);
                    switch (e.getMessage()) {
                        case Dcrlibwallet.ErrInvalidPassphrase:
                            if (util.get(Constants.SPENDING_PASSPHRASE_TYPE).equals(Constants.PIN)) {
                                tvRequiredSignature.setText(R.string.invalid_pin);
                            } else {
                                tvRequiredSignature.setText(R.string.invalid_password);
                            }
                            break;
                        default:
                            tvRequiredSignature.setText(Utils.translateError(getContext(), e));
                    }
                }
            });
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getContext() != null) {
            getContext().unregisterReceiver(receiver);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getContext() != null) {
            IntentFilter filter = new IntentFilter(Constants.SYNCED);
            getContext().registerReceiver(receiver, filter);
        }
    }

    private void checkCopyButton() {
        if (!wallet.isAddressValid(etAddress.getText().toString().trim()) || etSignature.getText().toString().equals("")) {
            btnCopy.setTextColor(ContextCompat.getColor(requireContext(), R.color.blackTextColor38pc));
            btnCopy.setBackgroundColor(Color.parseColor("#e6eaed"));
            btnCopy.setEnabled(false);
            return;
        }

        btnCopy.setTextColor(Color.WHITE);
        btnCopy.setBackgroundResource(R.drawable.btn_blue);
        btnCopy.setEnabled(true);
    }

    private void toggleMessageButton(boolean enable) {
        if (enable) {
            btnSignMessage.setTextColor(Color.WHITE);
            btnSignMessage.setBackgroundResource(R.drawable.btn_blue);
            btnSignMessage.setEnabled(true);
        } else {
            btnSignMessage.setTextColor(ContextCompat.getColor(requireContext(), R.color.blackTextColor38pc));
            btnSignMessage.setBackgroundColor(Color.parseColor("#e6eaed"));
            btnSignMessage.setEnabled(false);
        }
    }
}
