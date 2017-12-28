package com.decrediton.Activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.covics.zxingscanner.ScannerView;
import com.decrediton.Util.EncodeQrCode;
import com.google.zxing.integration.android.IntentIntegrator;
import com.decrediton.R;

/**
 * Created by Macsleven on 11/15/2015.
 */
public class ScannerActivity extends AppCompatActivity{
    private ScannerView scannerView;
    ImageView imageView;
    private TextView txtResult;

    TextView amount;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_ui_scan);
       // IntentIntegrator intentIntegrator=new IntentIntegrator(this);
        //intentIntegrator.shareText("this is just a screte");
        imageView=(ImageView) findViewById(R.id.bitm);
        imageView.setImageBitmap(EncodeQrCode.encodeToQrCode("hello",200,200));

    }


    @Override
    protected void onResume() {
        super.onResume();
        //onResume
//        scannerView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //onPause
      //  scannerView.onPause();

    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }
}
