package com.dcrandroid.preference

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager


class PreferenceHelper {
    private var mSharedPreferences: SharedPreferences? = null

    fun PreferenceHelper(context: Context?) {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun putInt(key: String?, value: Int) {
        mSharedPreferences!!.edit().putInt(key, value).apply()
    }

    fun getInt(key: String?, defaultValue: Int): Int {
        return mSharedPreferences!!.getInt(key, defaultValue)
    }

    fun clear() {
        val editor = mSharedPreferences!!.edit()
        editor.clear()
        editor.apply()
    }

}