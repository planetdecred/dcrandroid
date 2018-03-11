package com.dcrandroid.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.dcrandroid.R;
import com.dcrandroid.util.PreferenceUtil;

/**
 * Created by afomic on 3/6/18.
 *
 */

public abstract class BaseActivity extends AppCompatActivity {
    boolean darkTheme;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        PreferenceUtil preferenceUtil=new PreferenceUtil(this);
        darkTheme = preferenceUtil.getBoolean(getString(R.string.key_dark_theme),false);
        setTheme(darkTheme?R.style.darkTheme: R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
    }
}
