package com.decrediton;

import android.os.Bundle;
import android.os.Environment;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.dcrwallet.grpc.VersionRequest;
import com.dcrwallet.grpc.VersionResponse;
import com.dcrwallet.grpc.VersionServiceGrpc;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import dcrwallet.Dcrwallet;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

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
                    File file = new File(Dcrwallet.getHomeDir()+"/dcrwallet.conf");
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
                ManagedChannel channel = ManagedChannelBuilder.forAddress("127.0.0.1",9111)
                        .usePlaintext(true)
                        .build();
                VersionRequest versionRequest = VersionRequest.newBuilder()
                        .build();
                VersionServiceGrpc.VersionServiceBlockingStub stub = VersionServiceGrpc.newBlockingStub(channel);
                VersionResponse response = stub.version(versionRequest);
                System.out.println("Version response: "+response.getVersionString()+" "+response.getMajor());
            }
        });
    }
}