package com.decrediton.Util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by collins on 1/7/18.
 */

public class PreferenceUtil {
    public static final String BLOCK_HEIGHT = "block_height";
    Context ctx;
    public PreferenceUtil(Context ctx){
        this.ctx = ctx;
    }

    public String get(String key){
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getPackageName(), Context.MODE_PRIVATE);
        return sp.getString(key,"");
    }

    public void set(String key,String value){
        SharedPreferences.Editor editor = ctx.getSharedPreferences(ctx.getPackageName(), Context.MODE_PRIVATE).edit();
        editor.putString(key, value);
        editor.commit();
    }
}
