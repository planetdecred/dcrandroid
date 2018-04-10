package com.dcrandroid.util;

import android.text.InputFilter;
import android.text.Spanned;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by collins on 3/25/18.
 */

public class DecredInputFilter implements InputFilter {
    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        Pattern pattern = Pattern.compile("[0-9]{0,9}+((\\.[0-9]{0,7})?)||(\\.)?");
        Matcher matcher = pattern.matcher(dest);
        if(!matcher.matches()){
            return "";
        }
        return null;
    }

}
