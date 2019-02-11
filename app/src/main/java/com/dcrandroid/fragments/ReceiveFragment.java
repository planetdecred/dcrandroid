/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.dcrandroid.BuildConfig;
import com.dcrandroid.R;
import com.dcrandroid.data.Account;
import com.dcrandroid.data.Constants;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.Utils;
import com.dcrandroid.util.WalletData;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import net.glxn.qrgen.android.MatrixToImageConfig;
import net.glxn.qrgen.android.MatrixToImageWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

/**
 * Created by Macsleven on 28/11/2017.
 */
public class ReceiveFragment extends Fragment implements AdapterView.OnItemSelectedListener, View.OnTouchListener {

    ImageView imageView;
    LinearLayout ReceiveContainer;
    ArrayAdapter dataAdapter;
    List<String> categories;
    PreferenceUtil preferenceUtil;
    List<Integer> accountNumbers = new ArrayList<>();
    private TextView address;
    private WalletData constants;
    private Map<EncodeHintType, Object> qrHints = new HashMap<>();
    private Spinner accountSpinner;
    private Bitmap generatedQR;
    private Uri generatedUri;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getContext() == null) {
            return null;
        }
        constants = WalletData.getInstance();
        preferenceUtil = new PreferenceUtil(getContext());
        View rootView = inflater.inflate(R.layout.content_receive, container, false);

        ReceiveContainer = rootView.findViewById(R.id.receive_container);
        imageView = rootView.findViewById(R.id.bitm);
        address = rootView.findViewById(R.id.barcode_address);

        accountSpinner = rootView.findViewById(R.id.recieve_dropdown);
        accountSpinner.setOnItemSelectedListener(this);
        // Spinner Drop down elements
        categories = new ArrayList<>();

        dataAdapter = new ArrayAdapter(getContext(), R.layout.spinner_list_item, categories);
        dataAdapter.setDropDownViewResource(R.layout.dropdown_item_1);
        accountSpinner.setAdapter(dataAdapter);

        address.setOnTouchListener(this);

        imageView.setOnTouchListener(this);

        getActivity().setTitle(getString(R.string.receive));

        qrHints.put(EncodeHintType.MARGIN, 0);
        qrHints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);

        prepareAccounts();

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.receive_page_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.generate_address:
                try {
                    int position = accountSpinner.getSelectedItemPosition();
                    String oldAddress = address.getText().toString();
                    String newAddress = constants.wallet.nextAddress(accountNumbers.get(position));
                    if (oldAddress.equals(newAddress)) {
                        newAddress = constants.wallet.nextAddress(accountNumbers.get(position));
                    }
                    setAddress(newAddress);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            case R.id.share_qr_code:
                shareImageToApps();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void prepareAccounts() {
        try {
            final ArrayList<Account> accounts = Account.parse(constants.wallet.getAccounts(preferenceUtil.getBoolean(Constants.SPEND_UNCONFIRMED_FUNDS) ? 0 : Constants.REQUIRED_CONFIRMATIONS));
            accountNumbers.clear();
            categories.clear();
            for (int i = 0; i < accounts.size(); i++) {
                if (accounts.get(i).getAccountName().trim().equalsIgnoreCase(Constants.IMPORTED)) {
                    continue;
                }
                categories.add(i, accounts.get(i).getAccountName());
                accountNumbers.add(accounts.get(i).getAccountNumber());
            }
            dataAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setAddress(String accountAddress) {
        try {
            address.setText(accountAddress);
            QRCodeWriter qrWriter = new QRCodeWriter();
            BitMatrix matrix = qrWriter.encode("decred:" + accountAddress,
                    BarcodeFormat.QR_CODE,
                    300,
                    300,
                    qrHints);

            System.out.println("Image Width: " + imageView.getWidth() + " Image Height: " + imageView.getHeight());

            generatedQR = MatrixToImageWriter.toBitmap(matrix, new MatrixToImageConfig(Color.BLACK, Color.TRANSPARENT));

            imageView.setImageBitmap(generatedQR);
            Bitmap tempBitmap = getBitmapFromView(imageView);
            generatedUri = getUriFromBitmap(tempBitmap);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(ReceiveFragment.this.getContext(), getString(R.string.error_occurred_getting_address), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        try {
            String address = constants.wallet.currentAddress(accountNumbers.get(position));
            setAddress(address);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (getContext() == null) {
                    return false;
                }
                Utils.copyToClipboard(getContext(), address.getText().toString(), getString(R.string.address_copy_text));
                return true;
        }
        return false;
    }

    public void shareImageToApps() {
        if (generatedUri != null) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            shareIntent.setDataAndType(generatedUri, getContext().getContentResolver().getType(generatedUri));
            shareIntent.putExtra(Intent.EXTRA_STREAM, generatedUri);
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_address_via)));
        } else {
            Toast.makeText(getActivity(), R.string.address_not_found, Toast.LENGTH_SHORT).show();
        }
    }

    private Uri getUriFromBitmap(Bitmap image) {
        Uri uri = null;
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date now = new Date();
            String fileName = formatter.format(now) + ".png";
            File cachePath = new File(getActivity().getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "wallet_address: " + fileName);
            FileOutputStream stream = new FileOutputStream(cachePath);
            image.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(getActivity().getApplicationContext(), BuildConfig.APPLICATION_ID + ".fileprovider", cachePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return uri;
    }

    private Bitmap getBitmapFromView(View view) {
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        Drawable bgDrawable = view.getBackground();
        if (bgDrawable != null) {
            bgDrawable.draw(canvas);
        } else {
            canvas.drawColor(Color.WHITE);
        }
        view.draw(canvas);
        return returnedBitmap;
    }

}
