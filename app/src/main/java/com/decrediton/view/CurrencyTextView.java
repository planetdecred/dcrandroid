package com.decrediton.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatTextView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.TextAppearanceSpan;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Created by collins on 1/23/18.
 */

public class CurrencyTextView extends AppCompatTextView {
    public CurrencyTextView(Context context) {
        super(context);
    }

    public CurrencyTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CurrencyTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void formatAndSetText(String str){
        Spannable spannable = new SpannableString(str);
        TextAppearanceSpan span = new TextAppearanceSpan(getContext(),android.R.style.TextAppearance_Large);
        AbsoluteSizeSpan span2 = new AbsoluteSizeSpan((int)getTextSize() + 9);
        //System.out.println("Starting "+0+" End:"+(str.indexOf('.')+2));
        //System.out.println("Former Size: "+getTextSize()+" New Size: "+(getTextSize() + 6));
        spannable.setSpan(span,0, str.indexOf('.')+3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(span2,0, str.indexOf('.')+3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        setText(spannable);
    }

}