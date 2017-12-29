package com.decrediton.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.decrediton.R;

import org.json.JSONObject;

/**
 * Created by Macsleven on 11/15/2015.
 */
public class ReaderActivity extends AppCompatActivity {

    String address;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_ui_read);
       // IntentIntegrator intentIntegrator=new IntentIntegrator(this);
        //intentIntegrator.shareText("this is just a secrete");

        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setCaptureActivity(CaptureActivityAnyOrientation.class);
        integrator.setOrientationLocked(false);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        integrator.setPrompt("Scan a barcode to receive payment");
        integrator.setCameraId(0);  // Use a specific camera of the device
        integrator.setBeepEnabled(false);
        integrator.setBarcodeImageEnabled(false);
        integrator.initiateScan();

    }


    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanResult=IntentIntegrator.parseActivityResult(requestCode,resultCode,intent);
        if (scanResult !=null) {
            Toast.makeText(this,scanResult.getContents(),Toast.LENGTH_LONG).show();
            try{
                address = scanResult.getContents();
                intent.putExtra("keyName", address);
                setResult(RESULT_OK, intent);
                finish();

            }catch (Exception e){
                address="";
                finish();
            }
        }
        else {
            finish();
        }
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
