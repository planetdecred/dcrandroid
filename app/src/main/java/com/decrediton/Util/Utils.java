package com.decrediton.Util;

import android.app.ProgressDialog;
import android.content.Context;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

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
}
