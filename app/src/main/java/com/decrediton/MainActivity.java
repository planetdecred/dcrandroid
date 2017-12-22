package com.decrediton;

import android.os.Bundle;
import android.os.Environment;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.dcrwallet.grpc.CreateWalletRequest;
import com.dcrwallet.grpc.CreateWalletResponse;
import com.dcrwallet.grpc.VersionRequest;
import com.dcrwallet.grpc.VersionResponse;
import com.dcrwallet.grpc.VersionServiceGrpc;
import com.dcrwallet.grpc.WalletLoaderServiceGrpc;
import com.dcrwallet.grpc.WalletServiceGrpc;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.x500.X500Principal;

import dcrwallet.Dcrwallet;
import io.grpc.ManagedChannel;
import io.grpc.okhttp.NegotiationType;
import io.grpc.okhttp.OkHttpChannelBuilder;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        File sdcard = new File("/sdcard");
        File homeDir = new File(Dcrwallet.getHomeDir());
        if(sdcard.exists()){
            System.out.println("SDCard Exist");
        }else{
            System.out.println("SDCard does not exist");
        }
        if(homeDir.exists()){
            System.out.println("Home Dir Exists");
        }else{
            System.out.println("Home Dir Does not exist");
        }
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
                    //file.delete();
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
                    SSLSocketFactory factory = newSslSocketFactoryForCa(loadCert("rpc.pem"));
                    ManagedChannel channel = OkHttpChannelBuilder.forAddress("127.0.0.1", 9111)
                            .negotiationType(NegotiationType.TLS)
                            .sslSocketFactory(factory)
                            .usePlaintext(true)
                            .build();
                    CreateWalletRequest walletRequest = CreateWalletRequest.newBuilder()
                            .build();
                    WalletLoaderServiceGrpc.WalletLoaderServiceBlockingStub walletLoaderServiceBlockingStub = WalletLoaderServiceGrpc.newBlockingStub(channel);
                    CreateWalletResponse walletResponse = walletLoaderServiceBlockingStub.createWallet(walletRequest);
                    System.out.println("Wallet Response is here");
//                    VersionRequest versionRequest = VersionRequest.newBuilder()
//                            .build();
//                    VersionServiceGrpc.VersionServiceBlockingStub stub = VersionServiceGrpc.newBlockingStub(channel);
//                    VersionResponse response = stub.version(versionRequest);
//                    System.out.println("Version response: " + response.getVersionString() + " " + response.getMajor());
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
    public FileInputStream loadCert(String name) throws IOException {
        InputStream in = getAssets().open("rpc.pem");
        File tmpFile = File.createTempFile(name, "");
        tmpFile.deleteOnExit();

        BufferedWriter writer = new BufferedWriter(new FileWriter(tmpFile));
        try {
            int b;
            while ((b = in.read()) != -1) {
                writer.write(b);
            }
        } finally {
            writer.close();
        }
        return new FileInputStream(tmpFile);
    }

    public SSLSocketFactory newSslSocketFactoryForCa(InputStream certChain) throws Exception {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) cf.generateCertificate(
                new BufferedInputStream(certChain));
        X500Principal principal = cert.getSubjectX500Principal();
        ks.setCertificateEntry(principal.getName("RFC2253"), cert);

        // Set up trust manager factory to use our key store.
        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(ks);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, trustManagerFactory.getTrustManagers(), null);
        return context.getSocketFactory();
    }


}