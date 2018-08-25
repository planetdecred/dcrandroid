package com.dcrandroid.view;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatTextView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.TextAppearanceSpan;
import android.util.AttributeSet;
import android.widget.TextView;

import com.dcrandroid.util.Utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mobilewallet.Mobilewallet;

/**
 * Created by collins on 1/23/18.
 */

public class CurrencyTextView extends AppCompatTextView {
    public CurrencyTextView(Context context) {
        super(context);
        this.setTypeface(Typeface.createFromAsset(context.getAssets(),"fonts/Inconsolata-Regular.ttf"));
    }

    public CurrencyTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.setTypeface(Typeface.createFromAsset(context.getAssets(),"fonts/Inconsolata-Regular.ttf"));
    }

    public CurrencyTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.setTypeface(Typeface.createFromAsset(context.getAssets(),"fonts/Inconsolata-Regular.ttf"));
    }

    public void formatAndSetText(long amount){
        formatAndSetText(Mobilewallet.amountCoin(amount));
    }

    public void formatAndSetText(double amount){
        formatAndSetText(Utils.removeTrailingZeros(amount) + " DCR");
    }

    public void formatAndSetText(String str){
        Matcher doubleOrMoreDecimalPlaces = Pattern.compile("(([0-9]{1,3},*)+\\.)\\d{2,}").matcher(str);
        Matcher oneDecimalPlace = Pattern.compile("(([0-9]{1,3},*)+\\.)\\d").matcher(str);
        Matcher wholeNumber = Pattern.compile("\\d+").matcher(str);

        Spannable spannable = new SpannableString(str);
        TextAppearanceSpan span = new TextAppearanceSpan(getContext(), android.R.style.TextAppearance_Large);
        AbsoluteSizeSpan span2 = new AbsoluteSizeSpan((int) getTextSize() + 9);
        int startIndex, endIndex;
        if (doubleOrMoreDecimalPlaces.find()){
            startIndex = doubleOrMoreDecimalPlaces.start();
            endIndex = str.indexOf(".", startIndex) + 3;
        }else if (oneDecimalPlace.find()){
            startIndex = oneDecimalPlace.start();
            endIndex = str.indexOf(".", startIndex) + 2;
        }else if(wholeNumber.find()){
            startIndex = wholeNumber.start();
            endIndex = wholeNumber.end();
        }else{
            setText(str);
            return;
        }

        spannable.setSpan(span, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(span2, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        setText(spannable);
    }

}