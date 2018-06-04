package com.dcrandroid.util;

import android.content.Context;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;

import com.dcrandroid.data.Constants;

/**
 * Created by Macsleven on 13/05/2018.
 */

public class BlockedSelectionEditText extends AppCompatEditText {
    public BlockedSelectionEditText(Context context){
        super((context));
    }

    public BlockedSelectionEditText (Context context , AttributeSet attrs){
        super(context,attrs);
    }
    public BlockedSelectionEditText(Context context,AttributeSet attrs, int defStyle){
        super(context, attrs,defStyle);
    }
    @Override
    protected  void onSelectionChanged(int selstart, int selEnd){
        //setSelection(this.length());
    }
}
