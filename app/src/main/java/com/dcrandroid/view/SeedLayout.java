package com.dcrandroid.view;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;


public class SeedLayout extends ViewGroup {
    int deviceWidth;

    public SeedLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public SeedLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SeedLayout(Context context) {
        this(context, null, 0);
    }

    private void init(Context context) {
        final Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point deviceDisplay = new Point();
        display.getSize(deviceDisplay);
        deviceWidth = deviceDisplay.x;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();
        int currentWidth, currentHeight, currentTop, currentLeft, maxHeight;

        //get the available size of subview
        int TopSubview = this.getPaddingTop();
        int LeftSubview = this.getPaddingLeft();
        int RightSubview = this.getMeasuredWidth() - this.getPaddingRight();
        int BottomSubview = this.getMeasuredHeight() - this.getPaddingBottom();
        int SubviewWidth = RightSubview - LeftSubview;
        int SubviewHeight = BottomSubview - TopSubview;

        maxHeight = 0;
        currentLeft = LeftSubview;
        currentTop = TopSubview;

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);

            if (child.getVisibility() == GONE)
                return;

            //Get the maximum size of the subview
            child.measure(MeasureSpec.makeMeasureSpec(SubviewWidth, SubviewWidth / 3), MeasureSpec.makeMeasureSpec(SubviewHeight, MeasureSpec.AT_MOST));
            currentWidth = SubviewWidth / 3;
            currentHeight = child.getMeasuredHeight();
            //wrap is reach to the end
            if (currentLeft + currentWidth >= RightSubview) {
                currentLeft = LeftSubview;
                currentTop += maxHeight;
                maxHeight = 0;
            }
            //build the layout
            child.layout(currentLeft, currentTop, currentLeft + currentWidth, currentTop + currentHeight);
            //save maximum height
            if (maxHeight < currentHeight)
                maxHeight = currentHeight;
            currentLeft += currentWidth;
        }
    }


    @Override
    protected void onMeasure(int widthMeasurement, int heightMeasurement) {
        int count = getChildCount();
        int subviewState = 0;
        int mLeftWidth = 0;
        int maxHeight = 0;
        int rowCount = 0;
        int maxWidth = 0;

        // loop through all subview, measuring them and calculating their dimensions from their size.
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE)
                continue;
            // Measure the subview.
            measureChild(child, widthMeasurement, heightMeasurement);
            maxWidth += Math.max(maxWidth, child.getMeasuredWidth());
            mLeftWidth += child.getMeasuredWidth();

            if ((mLeftWidth / deviceWidth) > rowCount) {
                maxHeight += child.getMeasuredHeight();
                rowCount++;
            } else {
                maxHeight = Math.max(maxHeight, child.getMeasuredHeight());
            }
            subviewState = combineMeasuredStates(subviewState, child.getMeasuredState());
        }

        // compare against our minimum height and width
        maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

        // present dimensions.
        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasurement, subviewState), resolveSizeAndState(maxHeight, heightMeasurement, subviewState << MEASURED_HEIGHT_STATE_SHIFT));
    }
}
