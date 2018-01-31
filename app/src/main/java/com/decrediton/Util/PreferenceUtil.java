package com.decrediton.Util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by collins on 1/7/18.
 */

public class PreferenceUtil {
    public static final String BLOCK_HEIGHT = "block_height";
    public static final String TRANSACTION_HEIGHT = "tx_height";
    public static final String TOTAL_BALANCE = "total_balance";
    public static final String NO_OF_CONFIRMATIONS = "no_of_confirmations";
    Context ctx;
    public PreferenceUtil(Context ctx){
        this.ctx = ctx;
    }

    public String get(String key){
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getPackageName(), Context.MODE_PRIVATE);
        return sp.getString(key,"");
    }

    public String get(String key, String d){
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getPackageName(), Context.MODE_PRIVATE);
        return sp.getString(key,d);
    }

    public void set(String key,String value){
        SharedPreferences.Editor editor = ctx.getSharedPreferences(ctx.getPackageName(), Context.MODE_PRIVATE).edit();
        editor.putString(key, value);
        editor.commit();
    }

    public float getFloat(String key){
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getPackageName(), Context.MODE_PRIVATE);
        return sp.getFloat(key,0);
    }

    public void setFloat(String key, float n){
        SharedPreferences.Editor editor = ctx.getSharedPreferences(ctx.getPackageName(), Context.MODE_PRIVATE).edit();
        editor.putFloat(key, n);
        editor.commit();
    }
    public void setInt(String key, int n){
        SharedPreferences.Editor editor = ctx.getSharedPreferences(ctx.getPackageName(), Context.MODE_PRIVATE).edit();
        editor.putInt(key, n);
        editor.commit();
    }
    public int getInt(String key){
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getPackageName(), Context.MODE_PRIVATE);
        return sp.getInt(key,0);
    }
}
