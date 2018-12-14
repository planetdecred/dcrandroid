package com.dcrandroid.view


import android.content.Context
import android.support.v4.widget.NestedScrollView
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent

class CustomNestedScrollView : NestedScrollView {

    // true if we can scroll (not locked)
    // false if we cannot scroll (locked)
    private var isScrollable = false

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        // TODO Auto-generated constructor stub
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context) : super(context)

    fun setScrollingEnabled(enabled: Boolean) {
        isScrollable = enabled
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // TODO Auto-generated method stub
        return false
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        // TODO Auto-generated method stub
        return false
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        return when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                // if we can scroll pass the event to the superclass
                if (isScrollable) super.onTouchEvent(ev) else isScrollable
                // only continue to handle the touch event if scrolling enabled
                // mScrollable is always false at this point
            }
            else -> super.onTouchEvent(ev)
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // Don't do anything with intercepted touch events if
        // we are not scrollable
        return if (!isScrollable)
            false
        else
            super.onInterceptTouchEvent(ev)
    }


}