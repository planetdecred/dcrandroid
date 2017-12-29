package com.decrediton;

import android.app.Application;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import dcrwallet.Dcrwallet;

/**
 * Created by collins on 12/26/17.
 */

public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        new Thread(){
            public void run(){
                try {
                    this.setPriority(MAX_PRIORITY);
                    System.out.println("Wallet Home Dir: "+ Dcrwallet.getHomeDir());
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
    }
}
