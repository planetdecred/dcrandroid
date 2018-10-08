package com.dcrandroid.view

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.dcrandroid.R

class PinView : View {
    private var activePaint: Paint? = null
    private var emptyPaint: Paint? = null

    var passCodeLength: Int = 0
        set(value) {
            field = if (value > pinCount) pinCount else value
            invalidate()
        }

    var pinCount: Int = 0

    private var activeColor: Int = 0
    private var inactiveColor: Int = 0
    private var mWidth: Int = 0
    private var mHeight: Int = 0

    private var autoSpace: Boolean = false

    private var pinSize: Float = 0F
    private var horizontalSpacing: Float = 0F

    private var mContext: Context? = null

    private var circleRect: RectF? = null
    private var activeRect: RectF? = null

    constructor(context: Context) : super(context) {
        init(context, null, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs, defStyleAttr, 0)
    }

    @TargetApi(21)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs, defStyleAttr, defStyleRes)
    }

    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        this.mContext = context

        val values = context.theme.obtainStyledAttributes(attrs, R.styleable.PinView, defStyleAttr, defStyleRes)

        try {
            pinSize = values.getDimension(R.styleable.PinView_pin_size, resources.getDimension(R.dimen.pinview_pin_size))

            pinCount = values.getInteger(R.styleable.PinView_pin_count, 4)

            activeColor = values.getColor(R.styleable.PinView_active_color, resources.getColor(R.color.pinview_active_color))

            inactiveColor = values.getColor(R.styleable.PinView_inactive_color, resources.getColor(R.color.pinview_inactive_color))

            autoSpace = values.getBoolean(R.styleable.PinView_auto_space, true)

            horizontalSpacing = values.getDimension(R.styleable.PinView_horizontal_spacing, resources.getDimension(R.dimen.pinview_horizontal_spacing))

        } catch (e: Exception) {
            e.printStackTrace()
        }

        values.recycle()

        activePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        activePaint!!.style = Paint.Style.FILL
        activePaint!!.color = activeColor

        emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        emptyPaint!!.style = Paint.Style.FILL
        emptyPaint!!.color = inactiveColor

        circleRect = RectF()
        activeRect = RectF()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        this.mWidth = w
        this.mHeight = h
        super.onSizeChanged(w, h, oldw, oldh)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val pl = paddingLeft
        val pr = paddingRight
        val pt = paddingTop
        val pb = paddingBottom

        val usableWidth = width - (pl + pr)
        val usableHeight = height - (pt + pb)

        val totalPinWidth = pinSize * pinCount

        if (autoSpace) {
            horizontalSpacing = (pl + usableWidth - totalPinWidth) / pinCount
        }

        var startX = horizontalSpacing / 2
        val startY = pt + usableHeight / 2 - pinSize / 2
        val totalContentWidth = pinSize + horizontalSpacing

        for (i in 1..pinCount) {
            circleRect!!.left = startX
            circleRect!!.top = startY
            circleRect!!.right = startX + pinSize
            circleRect!!.bottom = startY + pinSize

            if (i <= passCodeLength) {
                canvas.drawArc(circleRect!!, startX, 360f, true, emptyPaint!!)

                val activePadding = pinSize * 0.15f

                activeRect!!.left = circleRect!!.left + activePadding
                activeRect!!.top = circleRect!!.top + activePadding
                activeRect!!.bottom = circleRect!!.bottom - activePadding
                activeRect!!.right = circleRect!!.right - activePadding

                canvas.drawArc(activeRect!!, activeRect!!.left, 360f, true, activePaint!!)

                startX += totalContentWidth
                continue
            }

            canvas.drawArc(circleRect!!, startX, 360f, true, emptyPaint!!)
            startX += totalContentWidth
        }
    }
}

