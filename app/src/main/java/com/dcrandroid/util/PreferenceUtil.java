package com.dcrandroid.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

/**
 * Created by collins on 1/7/18.
 */

public class PreferenceUtil {
    public static final String RESCAN_HEIGHT = "rescan_height";
    public static final String TRANSACTION_HEIGHT = "tx_height";
    public static final String TOTAL_BALANCE = "total_balance";
    public static final String NO_OF_CONFIRMATIONS = "no_of_confirmations";
    Context ctx;

    public PreferenceUtil(@NonNull Context ctx){
        this.ctx = ctx;
    }

    public String get(String key){
        if(ctx == null){
            return "";
        }
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        return sp.getString(key,"");
    }

    public String get(String key, String d){
        if(ctx == null){
            return d;
        }
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        return sp.getString(key,d);
    }

    public void set(String key,String value){
        if(ctx == null){
            return;
        }
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
        editor.putString(key, value);
        editor.commit();
    }

    public float getFloat(String key){
        if(ctx == null){
            return 0;
        }
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        return sp.getFloat(key,0);
    }

    public void setFloat(String key, float n){
        if(ctx == null){
            return;
        }
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
        editor.putFloat(key, n);
        editor.commit();
    }
    public void setInt(String key, int n){
        if(ctx == null){
            return;
        }
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
        editor.putInt(key, n);
        editor.commit();
    }

    public int getInt(String key, int defaultValue){
        if(ctx == null){
            return defaultValue;
        }
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        return sp.getInt(key,defaultValue);
    }

    public void setBoolean(String key, boolean n){
        if(ctx == null){
            return;
        }
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
        editor.putBoolean(key, n);
        editor.commit();
    }

    public boolean getBoolean(String key){
        if(ctx == null){
            return false;
        }
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        return sp.getBoolean(key, false);
    }

    public boolean getBoolean(String key, boolean d){
        if(ctx == null){
            return d;
        }
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        return sp.getBoolean(key, d);
    }

    public void setLong(String key, long l) {
        if(ctx == null){
            return;
        }
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
        editor.putLong(key, l);
        editor.commit();
    }

    public long getLong(String key, long defaultValue) {
        if(ctx == null){
            return defaultValue;
        }
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        return sp.getLong(key,defaultValue);
    }
}
