package com.decrediton;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;

import dcrwallet.Dcrwallet;
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new Thread(){
            public void run(){
                try {
                    this.setPriority(MAX_PRIORITY);
                    System.out.println("Wallet Home Dir: "+Dcrwallet.getHomeDir());
                    File path = new File(Dcrwallet.getHomeDir()+"/");
                    path.mkdirs();
                    File file = new File(path,"dcrwallet.conf");
                    if(!file.exists()) {
                        FileOutputStream fout = new FileOutputStream(file);
                        InputStream in = getAssets().open("sample-dcrwallet.conf");
                        int len;
                        byte[] buff = new byte[8192];
                        //read file till end
                        while ((len = in.read(buff)) != -1) {
                            fout.write(buff, 0, len);
                        }
                        fout.flush();
                        fout.close();
                    }
                    Dcrwallet.main();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }.start();
        findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Method method = Dcrwallet.class.getDeclaredMethod("createWallet", String.class);
                    Method callback = MainActivity.this.getClass().getDeclaredMethod("createWalletCallback", String.class);
                    ProgressDialog pd = Utils.getProgressDialog(MainActivity.this, false, false,"Creating Wallet...");
                    new BackgroundWorker(callback, pd, MainActivity.this).execute(method);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
        });
        findViewById(R.id.btn_open).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Method method = Dcrwallet.class.getDeclaredMethod("openWallet");
                    Method callback = MainActivity.this.getClass().getDeclaredMethod("openWalletCallback", String.class);
                    ProgressDialog pd = Utils.getProgressDialog(MainActivity.this, false, false,"Creating Wallet...");
                    new BackgroundWorker(callback, pd, MainActivity.this).execute(method);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
        });

        findViewById(R.id.btn_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Method method = Dcrwallet.class.getDeclaredMethod("closeWallet");
                    Method callback = MainActivity.this.getClass().getDeclaredMethod("closeWalletCallback", String.class);
                    ProgressDialog pd = Utils.getProgressDialog(MainActivity.this, false, false,"Creating Wallet...");
                    new BackgroundWorker(callback, pd, MainActivity.this).execute(method);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    //Called by Method.Invoke
    public void createWalletCallback(String responseJson){
        try {
            DcrResponse response = DcrResponse.parse(responseJson);
            if(response.errorOccurred){
                Toast.makeText(this, "Error Occurred: "+response.content, Toast.LENGTH_SHORT).show();
            }else{
                new AlertDialog.Builder(this)
                        .setMessage(response.content)
                        .setPositiveButton("OK", null)
                        .show();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void openWalletCallback(String responseJson){
        try {
            DcrResponse response = DcrResponse.parse(responseJson);
            if(response.errorOccurred){
                Toast.makeText(this, "Error Occurred: "+response.content, Toast.LENGTH_LONG).show();
            }else{
                if(response.content.equals("true")) {
                    Toast.makeText(this, "Wallet Opened", Toast.LENGTH_LONG).show();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void closeWalletCallback(String responseJson){
        try {
            DcrResponse response = DcrResponse.parse(responseJson);
            if(response.errorOccurred){
                Toast.makeText(this, "Error Occurred: "+response.content, Toast.LENGTH_LONG).show();
            }else{
                if(response.content.equals("true")) {
                    Toast.makeText(this, "Wallet Closed", Toast.LENGTH_LONG).show();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
