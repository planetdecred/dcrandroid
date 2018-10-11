package com.dcrandroid.fragments;

import android.content.DialogInterface;
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
import com.dcrandroid.util.DcrConstants;
import com.dcrandroid.util.Utils;

import mobilewallet.LibWallet;
import mobilewallet.Mobilewallet;

public class SecurityFragment extends Fragment implements View.OnClickListener {
    private EditText etValidateAddress, etSignMessageAddress, etSignMessage, etVerifyAddress, etVerifySignature, etVerifyMessage;
    private TextView tvValidateAddress, tvCopySignature, tvSignature, tvInvalidSignMessageAddress, tvRequiredSignMessage,
            tvInvalidVerifyAddress, tvInvalidVerifySignature, tvInvalidVerifyMessage;
    private Button btnSignMessage, btnVerify;
    private LibWallet wallet;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View vi = inflater.inflate(R.layout.fragment_security, container, false);

        //Validate Address
        etValidateAddress = vi.findViewById(R.id.et_validate_address);
        tvValidateAddress = vi.findViewById(R.id.tv_validate_address);

        //Sign Message
        etSignMessageAddress = vi.findViewById(R.id.et_sign_message_address);
        etSignMessage = vi.findViewById(R.id.et_sign_message);
        tvCopySignature = vi.findViewById(R.id.tv_copy_on_tap);
        tvSignature = vi.findViewById(R.id.tv_sign_message_signature);
        btnSignMessage = vi.findViewById(R.id.btn_sign_message);
        tvInvalidSignMessageAddress = vi.findViewById(R.id.tv_invalid_sign_message_address);
        tvRequiredSignMessage = vi.findViewById(R.id.tv_required_sign_message);

        //Verify Message
        etVerifyAddress = vi.findViewById(R.id.et_verify_address);
        etVerifySignature = vi.findViewById(R.id.et_verify_signature);
        etVerifyMessage = vi.findViewById(R.id.et_verify_message);
        tvInvalidVerifyAddress = vi.findViewById(R.id.tv_invalid_verify_message_address);
        tvInvalidVerifySignature = vi.findViewById(R.id.tv_required_verify_signature);
        tvInvalidVerifyMessage = vi.findViewById(R.id.tv_required_verify_message);
        btnVerify = vi.findViewById(R.id.btn_verify_message);

        return vi;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if(getActivity() != null)
            getActivity().setTitle(R.string.security);

        wallet = DcrConstants.getInstance().wallet;
        etValidateAddress.addTextChangedListener(validateAddressWatcher);
        etSignMessageAddress.addTextChangedListener(signMessageAddressWatcher);
        etVerifyAddress.addTextChangedListener(verifyMessageAddressWatcher);
        tvSignature.setOnClickListener(this);

        btnVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean requiresValidation = false;
                if(!wallet.isAddressValid(etVerifyAddress.getText().toString().trim())){
                    return;
                }
                if(etVerifySignature.getText().toString().equals("")){
                    tvInvalidVerifySignature.setVisibility(View.VISIBLE);
                    requiresValidation = true;
                }

                if(etVerifyMessage.getText().toString().equals("")){
                    tvInvalidVerifyMessage.setVisibility(View.VISIBLE);
                    requiresValidation = true;
                }

                if(!requiresValidation){
                    tvInvalidVerifyMessage.setVisibility(View.GONE);
                    tvInvalidVerifySignature.setVisibility(View.GONE);
                }else{
                    return;
                }

                String address = etVerifyAddress.getText().toString().trim();
                String signature = etVerifySignature.getText().toString();
                String message = etVerifyMessage.getText().toString();
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
                if(!wallet.isAddressValid(etSignMessageAddress.getText().toString().trim())){
                    return;
                }else if(etSignMessage.getText().toString().equals("")){
                    tvRequiredSignMessage.setVisibility(View.VISIBLE);
                }

                tvRequiredSignMessage.setVisibility(View.GONE);

                if(getContext() == null){
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
                        if(passphrase.getText().toString().equals("")){
                            Toast.makeText(SecurityFragment.this.getContext(), R.string.passphrase_is_empty, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        final byte[] passphraseByte = passphrase.getText().toString().getBytes();
                        final String address = etSignMessageAddress.getText().toString().trim();
                        final String message = etSignMessage.getText().toString();
                        try {
                            byte[] signature = wallet.signMessage(passphraseByte, address, message);
                            tvSignature.setText(Mobilewallet.encodeBase64(signature));
                            tvCopySignature.setVisibility(View.VISIBLE);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

                dialogBuilder.setNegativeButton(android.R.string.cancel, null);

                AlertDialog b = dialogBuilder.create();
                b.show();
                b.getButton(b.BUTTON_POSITIVE).setTextColor(Color.BLUE);
            }
        });
    }

    private TextWatcher validateAddressWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if(s.toString().trim().equals("")){
                tvValidateAddress.setText(null);
                return;
            }

            if(wallet.isAddressValid(s.toString().trim())){
                if(wallet.isAddressMine(s.toString().trim())){
                    tvValidateAddress.setTextColor(getActivity().getApplicationContext().getResources().getColor(R.color.blue));
                    tvValidateAddress.setText(R.string.owned_validate_address);
                    return;
                }
                tvValidateAddress.setTextColor(getActivity().getApplicationContext().getResources().getColor(R.color.greenTextColor));
                tvValidateAddress.setText(R.string.external_validate_address);
            }else{
                tvValidateAddress.setTextColor(getActivity().getApplicationContext().getResources().getColor(R.color.orangeTextColor));
                tvValidateAddress.setText(R.string.invalid_address);
            }
        }
    };

    private TextWatcher signMessageAddressWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if(s.toString().trim().equals("")){
                tvInvalidSignMessageAddress.setVisibility(View.GONE);
            }else if(wallet.isAddressValid(s.toString().trim())){
                tvInvalidSignMessageAddress.setVisibility(View.GONE);
            }else{
                tvInvalidSignMessageAddress.setVisibility(View.VISIBLE);
            }
        }
    };

    private TextWatcher verifyMessageAddressWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if(s.toString().trim().equals("")){
                tvInvalidVerifyAddress.setVisibility(View.GONE);
            }else if(wallet.isAddressValid(s.toString().trim())){
                tvInvalidVerifyAddress.setVisibility(View.GONE);
            }else{
                tvInvalidVerifyAddress.setVisibility(View.VISIBLE);
            }
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tv_sign_message_signature:
                Utils.copyToClipboard(getContext(), tvSignature.getText().toString(), getString(R.string.signature_copied_successfully));
                break;
        }
    }
}
