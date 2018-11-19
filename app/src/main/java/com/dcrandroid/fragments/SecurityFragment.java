package com.dcrandroid.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.dcrandroid.R;
import com.dcrandroid.activities.EnterPassCode;
import com.dcrandroid.data.Constants;
import com.dcrandroid.util.DcrConstants;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.Utils;

import mobilewallet.LibWallet;
import mobilewallet.Mobilewallet;

import static android.app.Activity.RESULT_OK;

public class SecurityFragment extends Fragment {
    private final int PASSCODE_REQUEST_CODE = 1;
    private EditText etAddress, etMessage, etSignature;
    private TextView tvValidateAddress, tvRequiredMessage, tvRequiredSignature;
    private Button btnSignMessage, btnVerify;
    private LibWallet wallet;
    private PreferenceUtil util;
    private TextWatcher validateAddressWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if (s.toString().trim().equals("")) {
                tvValidateAddress.setText(null);
                return;
            }

            if (wallet.isAddressValid(s.toString().trim())) {
                if (wallet.haveAddress(s.toString().trim())) {
                    tvValidateAddress.setTextColor(getActivity().getApplicationContext().getResources().getColor(R.color.blue));
                    tvValidateAddress.setText(R.string.owned_validate_address);
                    return;
                }
                tvValidateAddress.setTextColor(getActivity().getApplicationContext().getResources().getColor(R.color.greenTextColor));
                tvValidateAddress.setText(R.string.external_validate_address);
            } else {
                tvValidateAddress.setTextColor(getActivity().getApplicationContext().getResources().getColor(R.color.orangeTextColor));
                tvValidateAddress.setText(R.string.invalid_address);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View vi = inflater.inflate(R.layout.fragment_security, container, false);

        etAddress = vi.findViewById(R.id.et_address);
        tvValidateAddress = vi.findViewById(R.id.tv_validate_address);
        etMessage = vi.findViewById(R.id.et_sign_message);
        btnSignMessage = vi.findViewById(R.id.btn_sign_message);
        tvRequiredMessage = vi.findViewById(R.id.tv_required_sign_message);

        // Verify Message
        etSignature = vi.findViewById(R.id.et_verify_signature);
        tvRequiredSignature = vi.findViewById(R.id.tv_required_verify_signature);
        btnVerify = vi.findViewById(R.id.btn_verify_message);

        return vi;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getActivity() == null || getContext() == null)
            return;

        getActivity().setTitle(R.string.security);

        util = new PreferenceUtil(getContext());
        wallet = DcrConstants.getInstance().wallet;
        etAddress.addTextChangedListener(validateAddressWatcher);

        btnVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean requiresValidation = false;
                if (!wallet.isAddressValid(etAddress.getText().toString().trim())) {
                    return;
                }
                if (etSignature.getText().toString().equals("")) {
                    tvRequiredSignature.setText(R.string.this_field_is_required);
                    requiresValidation = true;
                }

                if (etMessage.getText().toString().equals("")) {
                    tvRequiredMessage.setVisibility(View.VISIBLE);
                    requiresValidation = true;
                }

                if (!requiresValidation) {
                    tvRequiredMessage.setVisibility(View.INVISIBLE);
                    tvRequiredSignature.setText(null);
                } else {
                    return;
                }

                String address = etAddress.getText().toString().trim();
                String signature = etSignature.getText().toString();
                String message = etMessage.getText().toString();
                try {
                    boolean valid = wallet.verifyMessage(address, message, signature);
                    Toast.makeText(SecurityFragment.this.getContext(), valid ? R.string.valid : R.string.invalid, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PASSCODE_REQUEST_CODE && resultCode == RESULT_OK) {
            new Thread() {
                public void run() {
                    signMessage(data.getStringExtra(Constants.PIN).getBytes());
                }
            }.start();
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
                    etSignature.setText(Mobilewallet.encodeBase64(signature));
                    tvRequiredSignature.setText(null);
                }
            });
        } catch (final Exception e) {
            e.printStackTrace();
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (e.getMessage()) {
                        case Mobilewallet.ErrInvalidPassphrase:
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
}
