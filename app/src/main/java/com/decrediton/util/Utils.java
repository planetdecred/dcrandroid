package com.decrediton.util;

import android.app.ProgressDialog;
import android.content.Context;

import com.decrediton.R;
import com.decrediton.data.BestBlock;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import dcrwallet.Dcrwallet;

public class Utils {
    public static ProgressDialog getProgressDialog(Context context,boolean cancelable, boolean cancelOnTouchOutside,
                                                   String message){
        ProgressDialog pd = new ProgressDialog(context);
        pd.setCancelable(cancelable);
        pd.setCanceledOnTouchOutside(cancelOnTouchOutside);
        pd.setMessage(message);
        return pd;
    }

    public static String getWordList(Context context){
        try {
            InputStream fin = context.getAssets().open("wordlist.txt");
            StringBuilder wordsList = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(fin));
            String line;
            while ((line = br.readLine()) != null) {
                wordsList.append(" ");
                wordsList.append(line);
            }
            fin.close();
            return wordsList.toString().trim();
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public static String getHash(byte[] mHash){
        List<Byte> hashList = new ArrayList<>();
        for (byte aHash : mHash) {
            hashList.add(aHash);
        }
        Collections.reverse(hashList);
        StringBuilder sb = new StringBuilder();
        for(byte b : hashList){
            sb.append(String.format(Locale.getDefault(),"%02x", b));
        }
        return sb.toString();
    }

    public static byte[] getHash(String hash){
        List<String> hashList = new ArrayList<>();
        String[] split = hash.split("");
        if((split.length-1)%2 == 0) {
            String d = "";
            for(int i = 0; i <  split.length -1; i += 2){
                d = d.concat(split[(split.length - 1)  - (i+1)]
                        + split[(split.length - 1) - i]);
                hashList.add(split[(split.length - 1)  - (i+1)]
                        + split[(split.length - 1) - i]);
            }
            return hexStringToByteArray(d);
        }else {
            System.err.println("Invalid Hash");
        }
        return null;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static String toHex(String arg) {
        return String.format("%040x", new BigInteger(1, arg.getBytes()));
    }

    public static String getConnectionCertificate(Context context){
        PreferenceUtil util = new PreferenceUtil(context);
        if(util.getBoolean(context.getString(R.string.key_connection_local_dcrd), true)){
            return Utils.getDefaultCertificate(context);
        }else{
            return util.get(context.getString(R.string.remote_certificate));
        }
    }

    public static String getDefaultCertificate(Context context){
        try {
            InputStream in = context.getAssets().open("dcrdrpc.cert");
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String s;
            while ((s = reader.readLine()) != null) {
                sb.append(s);
                sb.append("\n");
            }
            return sb.toString();

        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    public static String getDcrdNetworkAddress(Context context){
        PreferenceUtil util = new PreferenceUtil(context);
        if(util.getBoolean(context.getString(R.string.key_connection_local_dcrd), true)){
            System.out.println("Util is using local server");
            return Dcrwallet.isTestNet() ? context.getString(R.string.dcrd_address_testnet) : context.getString(R.string.dcrd_address);
        }else{
            String addr = util.get(context.getString(R.string.remote_dcrd));
            System.out.println("Util is using remote server: "+addr);
            return addr;
        }
    }

    public static void writeDcrwalletFiles(Context context) throws IOException {
        File path = new File(Dcrwallet.getHomeDir()+"/");
        path.mkdirs();
        String[] files = {"dcrwallet.conf","rpc.key","rpc.cert"};
        String[] assetFilesName = {"sample-dcrwallet.conf","rpc.key","rpc.cert"};
        for(int i = 0; i < files.length; i++) {
            File file = new File(path, files[i]);
            if (!file.exists()) {
                file.createNewFile();
                FileOutputStream fout = new FileOutputStream(file);
                InputStream in = context.getAssets().open(assetFilesName[i]);
                int len;
                byte[] buff = new byte[8192];
                //read file till end
                while ((len = in.read(buff)) != -1) {
                    fout.write(buff, 0, len);
                }
                fout.flush();
                fout.close();
            }
        }
    }

    public static void writeDcrdFiles(Context context) throws IOException {
        File path = new File(context.getFilesDir().getPath(),"/dcrd");
        path.mkdirs();
        String[] files = {"rpc.key","dcrd.conf"};
        String[] assetFilesName = {"dcrdrpc.key","dcrd.conf"};
        //String[] assetFilesName = {"dcrdrpc.key","devrpc.cert","dcrd.conf"};
        for(int i = 0; i < files.length; i++) {
            File file = new File(path, files[i]);
            //[Debug] Write the file to the storage if it exists or not
            if (!file.exists() || true) {
                file.createNewFile();
                FileOutputStream fout = new FileOutputStream(file);
                InputStream in = context.getAssets().open(assetFilesName[i]);
                int len;
                byte[] buff = new byte[8192];
                //read file till end
                while ((len = in.read(buff)) != -1) {
                    fout.write(buff, 0, len);
                }
                fout.flush();
                fout.close();
            }
        }
    }

    public static void writeDcrdCertificate(Context context) throws Exception{
        File path = new File(context.getFilesDir().getPath(),"/dcrd");
        path.mkdirs();
        File file = new File(path, "rpc.cert");
        FileOutputStream fout = new FileOutputStream(file);
        byte[] buffer = getConnectionCertificate(context).getBytes();
        fout.write(buffer, 0, buffer.length);
        fout.flush();
        fout.close();
    }

    public static BestBlock parseBestBlock(String json) throws JSONException{
        JSONObject obj = new JSONObject(json);
        return new BestBlock(obj.getString("hash"), obj.getInt("height"));
    }
}