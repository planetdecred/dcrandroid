package com.decrediton;

import android.os.Bundle;
import android.os.Environment;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.google.protobuf.ByteString;
import com.squareup.okhttp.OkHttpClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import dcrwallet.Dcrwallet;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.AbstractStub;
import walletrpc.Api;

public class MainActivity extends AppCompatActivity {
    static {
        System.out.println("Static system");
//        System.loadLibrary("gojni");
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("Oncreate system");
        setContentView(R.layout.activity_main);
        ManagedChannel channel = ManagedChannelBuilder.forAddress("127.0.0.1",19111)
                .usePlaintext(true)
                .build();
        new Thread(){
            public void run(){
                try {
                    this.setPriority(MAX_PRIORITY);
                    System.out.println("Wallet Home Dir: "+Dcrwallet.getHomeDir());
                    File file = new File(Dcrwallet.getHomeDir()+"/dcrwallet.conf");
                    FileOutputStream fout = new FileOutputStream(file);
                    InputStream in = getAssets().open("sample-dcrwallet.conf");
                    int len;
                    byte[] buff = new byte[8192];
                     //read file till end
                    while((len = in.read(buff)) != -1){
                        fout.write(buff, 0, len);
                    }
                    fout.flush();
                    fout.close();
//                    file.delete();
                    Dcrwallet.main();
                    System.out.println("NIGGA");
                }catch (Exception e){
                    e.printStackTrace();
                }
                //Dcrwallet.main();
            }
        };
        Dcrwallet.main();
        Api.CreateWalletRequest request1 = Api.CreateWalletRequest.getDefaultInstance();
        ByteString bytes = request1.getPublicPassphrase();
        System.out.println("Wallet String: "+new String(bytes.toByteArray()));
        //VersionServiceGrpc f;
    }
}