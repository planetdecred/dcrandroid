package com.dcrandroid.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

/**
 * Created by collins on 1/7/18.
 */

public class PreferenceUtil {
    public static final String BLOCK_HEIGHT = "block_height";
    public static final String TRANSACTION_HEIGHT = "tx_height";
    public static final String TOTAL_BALANCE = "total_balance";
    public static final String NO_OF_CONFIRMATIONS = "no_of_confirmations";
    public static final String PREF_DARK_THEME = "dark_theme";
    Context ctx;

    public PreferenceUtil(@NonNull Context ctx){
        this.ctx = ctx;
    }

    public String get(String key){
        if(ctx == null){
            return "";
        }
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getPackageName(), Context.MODE_PRIVATE);
        return sp.getString(key,"");
    }

    public String get(String key, String d){
        if(ctx == null){
            return d;
        }
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getPackageName(), Context.MODE_PRIVATE);
        return sp.getString(key,d);
    }

    public void set(String key,String value){
        if(ctx == null){
            return;
        }
        SharedPreferences.Editor editor = ctx.getSharedPreferences(ctx.getPackageName(), Context.MODE_PRIVATE).edit();
        editor.putString(key, value);
        editor.commit();
    }

    public float getFloat(String key){
        if(ctx == null){
            return 0;
        }
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getPackageName(), Context.MODE_PRIVATE);
        return sp.getFloat(key,0);
    }

    public void setFloat(String key, float n){
        if(ctx == null){
            return;
        }
        SharedPreferences.Editor editor = ctx.getSharedPreferences(ctx.getPackageName(), Context.MODE_PRIVATE).edit();
        editor.putFloat(key, n);
        editor.commit();
    }
    public void setInt(String key, int n){
        if(ctx == null){
            return;
        }
        SharedPreferences.Editor editor = ctx.getSharedPreferences(ctx.getPackageName(), Context.MODE_PRIVATE).edit();
        editor.putInt(key, n);
        editor.commit();
    }
    public int getInt(String key){
        if(ctx == null){
            return 0;
        }
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getPackageName(), Context.MODE_PRIVATE);
        return sp.getInt(key,0);
    }

    public void setBoolean(String key, boolean n){
        if(ctx == null){
            return;
        }
        SharedPreferences.Editor editor = ctx.getSharedPreferences(ctx.getPackageName(), Context.MODE_PRIVATE).edit();
        editor.putBoolean(key, n);
        editor.commit();
    }

    public boolean getBoolean(String key){
        if(ctx == null){
            return false;
        }
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getPackageName(), Context.MODE_PRIVATE);
        return sp.getBoolean(key, false);
    }
    public boolean getBoolean(String key, boolean d){
        if(ctx == null){
            return d;
        }
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getPackageName(), Context.MODE_PRIVATE);
        return sp.getBoolean(key, d);
    }
}
