package com.decrediton.Util;

import android.app.ProgressDialog;
import android.content.Context;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Utils {
    public static ProgressDialog getProgressDialog(Context context,boolean cancelable, boolean cancelOnTouchOutside,
                                                   String message){
        ProgressDialog pd = new ProgressDialog(context);
        pd.setCancelable(cancelable);
        pd.setCanceledOnTouchOutside(cancelOnTouchOutside);
        pd.setMessage(message);
        return pd;
    }

    public static String[] getWordList(){
        try {
            FileInputStream fin = new FileInputStream("words.txt");
            ArrayList<String> wordsList = new ArrayList<>();
            BufferedReader br = new BufferedReader(new InputStreamReader(fin));
            String line;
            while ((line = br.readLine()) != null) {
                wordsList.add(line);
            }
            fin.close();
            return (String[]) wordsList.toArray();
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
}
