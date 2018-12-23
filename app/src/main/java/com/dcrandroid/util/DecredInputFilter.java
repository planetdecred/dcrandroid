package com.dcrandroid.util;

import android.text.InputFilter;
import android.text.Spanned;

/**
 * Created by collins on 3/25/18.
 */

public class DecredInputFilter implements InputFilter {

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        try {
            float input = Float.parseFloat(dest.toString() + source.toString());
            if (isInRange(input))
                return null;
        } catch (NumberFormatException ignored) {
        }
        return "";
    }

    private boolean isInRange(float c) {
        return c <= 21000000000F;
    }

}
