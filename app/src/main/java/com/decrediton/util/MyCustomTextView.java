package com.decrediton.util;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.support.v7.widget.AppCompatTextView;



/**
 * Created by Macsleven on 05/01/2018.
 */

public class MyCustomTextView extends AppCompatTextView{
    public MyCustomTextView(Context context, AttributeSet attributeSet){
        super(context,attributeSet);
        this.setTypeface(Typeface.createFromAsset(context.getAssets(),"fonts/Inconsolata-Regular.ttf"));
    }
}
