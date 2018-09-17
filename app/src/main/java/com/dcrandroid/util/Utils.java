package com.dcrandroid.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;

import com.dcrandroid.MainApplication;
import com.dcrandroid.R;
import com.dcrandroid.data.Constants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import mobilewallet.Mobilewallet;

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

    public static String getRemoteCertificate(Context context){
        try {
            File path = new File(context.getFilesDir()+"/savedata");
            if(!path.exists()){
                path.mkdirs();
            }
            File file = new File(path,"remote rpc.cert");
            if(file.exists()){
                FileInputStream fin = new FileInputStream(file);
                StringBuilder sb = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
                String s;
                while ((s = reader.readLine()) != null){
                    sb.append(s);
                    sb.append("\n");
                }
                fin.close();
                //System.out.println("Cert: "+sb.toString());
                return sb.toString();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void setRemoteCetificate(Context context, String certificate){
        try {
            File path = new File(context.getFilesDir()+"/savedata");
            if(!path.exists()){
                path.mkdirs();
            }
            File file = new File(path,"remote rpc.cert");
            if(file.exists()){
                file.delete();
            }
            FileOutputStream fout  = new FileOutputStream(file);
            byte[] buff = certificate.getBytes();
            fout.write(buff, 0, buff.length);
            fout.flush();
            fout.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getNetworkAddress(Context context, MainApplication application){
        PreferenceUtil util = new PreferenceUtil(context);
        if(application.getNetworkMode() != 2){
            System.out.println("Util is using local server");
            //return Dcrwallet.isTestNet() ? context.getString(R.string.dcrd_address_testnet) : context.getString(R.string.dcrd_address);
            return "";
        }else{
            String addr = util.get(Constants.REMOTE_NODE_ADDRESS);
            System.out.println("Util is using remote server: "+addr);
            return addr;
        }
    }

    public static void writeDcrdCertificate(Context context) throws Exception{
        File path = new File(context.getFilesDir().getPath(),"/dcrd");
        path.mkdirs();
        File file = new File(path, "rpc.cert");
        FileOutputStream fout = new FileOutputStream(file);
        System.out.println("Cert: "+getRemoteCertificate(context));
        byte[] buffer = getRemoteCertificate(context).getBytes();
        fout.write(buffer, 0, buffer.length);
        fout.flush();
        fout.close();
    }

    //TODO: Make available for both testnet and mainnet
    public static double estimatedBlocks(){
        Calendar startDate = new GregorianCalendar(2017,3,15);
        Calendar today = new GregorianCalendar();
        today.setTimeInMillis(System.currentTimeMillis());
        long totalDays = (today.getTimeInMillis() - startDate.getTimeInMillis()) / 1000 / 60 / 60 / 24;
        int blocksPerDay = 720;
        return Math.round(totalDays * blocksPerDay * (0.95));
    }

    public static String calculateTime(long seconds) {
        if (seconds > 59) {

            // convert to minutes
            seconds /= 60;

            if (seconds > 59) {

                // convert to hours
                seconds /= 60;

                if (seconds > 23) {

                    // convert to days
                    seconds /= 24;

                    if(seconds > 6){

                        // convert to weeks
                        seconds /= 7;
                        if(seconds > 3){

                            // convert to month
                            seconds /= 4;

                            if(seconds > 11){

                                //Convert to
                                seconds /= 12;

                                return seconds +"y ago";
                            }

                            //months
                            return seconds +"mo ago";
                        }
                        //weeks
                        return seconds + "w ago";
                    }
                    //days
                    return seconds + "d ago";
                }
                //hour
                return seconds + "h ago";
            }

            //minutes
            return seconds + "m ago";
        }

        if(seconds < 0){
            return "now";
        }
        //seconds
        return seconds + "s ago";
    }

    public static String formatDecred(long dcr){
        BigDecimal satoshi = BigDecimal.valueOf(dcr);
        BigDecimal amount = satoshi.divide(BigDecimal.valueOf(1e8), new MathContext(100));
        DecimalFormat format = new DecimalFormat();
        format.applyPattern("#,###,###,##0.########");
        return format.format(amount);
    }

    public static String removeTrailingZeros(double dcr){
        DecimalFormat format = new DecimalFormat();
        format.applyPattern("#,###,###,##0.########");
        return format.format(dcr);
    }

    public static String formatDecredWithoutComma(long dcr){
        BigDecimal atom = new BigDecimal(dcr);
        BigDecimal amount = atom.divide(BigDecimal.valueOf(1e8), new MathContext(100));
        DecimalFormat format = new DecimalFormat();
        format.applyPattern("#########0.########");
        return format.format(amount);
    }
    public static double formatDecredToDobule(long dcr){
        BigDecimal satoshi = BigDecimal.valueOf(dcr);
        BigDecimal amount = satoshi.divide(BigDecimal.valueOf(1e8), new MathContext(100));
        return amount.doubleValue();
    }

    public static long signedSizeToAtom(long signedSize){
        BigDecimal signed = new BigDecimal(signedSize);
        signed = signed.setScale(9, RoundingMode.HALF_UP);

        BigDecimal feePerKb = new BigDecimal(0.01);
        feePerKb = feePerKb.setScale(9, RoundingMode.HALF_UP);

        signed = signed.divide(feePerKb, MathContext.DECIMAL128);

        return signed.longValue();
    }

    public static String getPeerAddress(PreferenceUtil util){
        String ip = util.get("peer_ip");
        if (ip.length() == 0){
            return "";
        }
        if(ip.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}:(\\d){1,5}$")){
            return ip;
        }else{
            return ip+":19108";
        }
    }

    public static void copyToClipboard(Context ctx, String copyText, String successMessage) {
        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            if(clipboard != null) {
                clipboard.setText(copyText);
            }
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                    ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData
                    .newPlainText(ctx.getString(R.string.your_address), copyText);
            if(clipboard != null)
                clipboard.setPrimaryClip(clip);
        }
        Toast toast = Toast.makeText(ctx,
                successMessage, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM | Gravity.END, 50, 50);
        toast.show();
    }

    public static void backupWalletDB(final Context context){
        try {
            long startTime = System.currentTimeMillis();
            //TODO: Mainnet support
            File walletDb = new File(context.getFilesDir() + "/dcrwallet/testnet2/wallet.db");
            File backup = new File(context.getFilesDir() + "/dcrwallet/testnet2/wallet.db.bak");
            if (backup.exists()) {
                backup.delete();
            }
            if(walletDb.exists() && walletDb.isFile()) {
                FileOutputStream out = new FileOutputStream(backup);
                FileInputStream in = new FileInputStream(walletDb);

                byte[] buff = new byte[8192];
                int len;

                while ((len = in.read(buff)) != -1) {
                    out.write(buff, 0, len);
                    out.flush();
                }
                out.close();
                in.close();
                System.out.println("Backup took " + (System.currentTimeMillis() - startTime) + " ms");
            }
        }catch (IOException e){
            System.out.println("Backup Failed");
            e.printStackTrace();
        }
    }

    public static void restoreWalletDB(final Context context){
        try {
            long startTime = System.currentTimeMillis();
            //TODO: Mainnet support
            File walletDb = new File(context.getFilesDir() + "/dcrwallet/testnet2/wallet.db");
            File backup = new File(context.getFilesDir() + "/dcrwallet/testnet2/wallet.db.bak");
            if (walletDb.exists()) {
                walletDb.delete();
            }
            if (walletDb.exists()) {
                FileOutputStream out = new FileOutputStream(walletDb);
                FileInputStream in = new FileInputStream(backup);

                byte[] buff = new byte[8192];
                int len;

                while ((len = in.read(buff)) != -1) {
                    out.write(buff, 0, len);
                    out.flush();
                }
                out.close();
                in.close();
                System.out.println("Restore took " + (System.currentTimeMillis() - startTime) + " ms");
            }
        }catch (IOException e){
            System.out.println("Restore Failed");
            e.printStackTrace();
        }
    }

    public static String translateError(Context ctx, Exception e){
        switch (e.getMessage()){
            case Mobilewallet.ErrInsufficientBalance:
                return ctx.getString(R.string.not_enough_funds);
            case Mobilewallet.ErrEmptySeed:
                return ctx.getString(R.string.empty_seed);
            case Mobilewallet.ErrNotConnected:
                return ctx.getString(R.string.not_connected);
            case Mobilewallet.ErrPassphraseRequired:
                return ctx.getString(R.string.passphrase_required);
            case Mobilewallet.ErrWalletNotLoaded:
                return ctx.getString(R.string.wallet_not_loaded);
            default:
                return e.getMessage();
        }
    }
}