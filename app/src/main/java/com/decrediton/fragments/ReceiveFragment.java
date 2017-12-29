package com.decrediton.fragments;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.decrediton.R;
import com.decrediton.Util.EncodeQrCode;

/**
 * Created by Macsleven on 28/11/2017.
 */

public class ReceiveFragment extends android.support.v4.app.Fragment {
    ImageView imageView;
    private TextView address;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //returning our layout file
        //change R.layout.yourlayoutfilename for each of your fragments
        return inflater.inflate(R.layout.content_receive, container, false);
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
         imageView = view.findViewById(R.id.bitm);
         address = view.findViewById(R.id.barcode_address);
        Button buttonGenerate = view.findViewById(R.id.btn_gen_new_addr);
        address.setText("Tw2wedd3tete3re34rfdrr");
        address.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                copyToClipboard(address.getText().toString());
            }
        });
        imageView.setImageBitmap(EncodeQrCode.encodeToQrCode("Tw2wedd3tete3re34rfdrr",200,200));
        buttonGenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imageView.setImageBitmap(EncodeQrCode.encodeToQrCode("T1234365476894tg46eu44jekd66whsgw5",200,200));
                address.setText("T1234365476894tg46eu44jekd66whsgw5");
            }
        });
        //you can set the title for your toolbar here for different fragments different titles
        getActivity().setTitle("Receive");
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
                    .newPlainText("Your OTP", copyText);
            clipboard.setPrimaryClip(clip);
        }
        Toast toast = Toast.makeText(getContext(),
                "Your OTP is copied", Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM | Gravity.RIGHT, 50, 50);
        toast.show();
        //displayAlert("Your OTP is copied");
    }
}
