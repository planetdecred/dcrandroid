/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.StringRes
import com.dcrandroid.R
import dcrlibwallet.Dcrlibwallet
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.*

class PinView : TextView, View.OnClickListener {

    private var circlePaint: Paint? = null
    private var hintPaint: TextPaint? = null

    private val lock: ReentrantLock = ReentrantLock()

    private var hint: String = ""
    private var showHint: Boolean = true

    var rejectInput = false
    private var passCodeLength: Int = 0

    private var dotColor: Int = 0
    private var autoSpace: Boolean = false

    private var pinSize: Float = 0F
    private var horizontalSpacing: Float = 0F
    private var verticalSpacing: Float = 0F

    private var mContext: Context? = null

    private var circleRect: RectF? = null

    var counterTextView: TextView? = null

    fun setHint(hint: String){
        lock.lock()

        counterTextView?.text = null
        this.hint = hint
        passCodeLength = 0
        showHint = true

        lock.unlock()

        requestLayout()
        invalidate()
    }

    fun reset(){
        lock.lock()

        counterTextView?.text = "0"
        passCodeLength = 0
        showHint = true

        lock.unlock()

        requestLayout()
        invalidate()
    }

    constructor(context: Context) : super(context) {
        init(context, null, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs, defStyleAttr, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs, defStyleAttr, defStyleRes)
    }

    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        this.mContext = context
        this.setOnClickListener(this)

        val values = context.theme.obtainStyledAttributes(attrs, R.styleable.PinView, defStyleAttr, defStyleRes)

        try {
            pinSize = values.getDimension(R.styleable.PinView_pin_size, resources.getDimension(R.dimen.pinview_pin_size))

            dotColor = values.getColor(R.styleable.PinView_active_color, resources.getColor(R.color.pinview_active_color))

            autoSpace = values.getBoolean(R.styleable.PinView_auto_space, true)

            horizontalSpacing = values.getDimension(R.styleable.PinView_horizontal_spacing, resources.getDimension(R.dimen.pinview_horizontal_spacing))
            verticalSpacing = values.getDimension(R.styleable.PinView_vertical_spacing, resources.getDimension(R.dimen.pinview_vertical_spacing))

        } catch (e: Exception) {
            e.printStackTrace()
        }

        values.recycle()

        circlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        circlePaint!!.style = Paint.Style.FILL
        circlePaint!!.color = dotColor

        hintPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        hintPaint!!.textSize = context.resources.getDimension(R.dimen.edit_text_size_16)
        hintPaint!!.textAlign = Paint.Align.CENTER
        hintPaint!!.color = context.resources.getColor(R.color.lightGrayTextColor)

        circleRect = RectF()

        this.post {
            showKeyboard()
        }

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        val widthSize = MeasureSpec.getSize(widthMeasureSpec)

        val dotWidthPlusPadding = pinSize + horizontalSpacing
        val dotHeightPlusPadding = pinSize + verticalSpacing

        val maxDotsPerRow = floor(widthSize / dotWidthPlusPadding)
        val numberOfRows = ceil(passCodeLength / maxDotsPerRow).toInt()

        var heightSize = (numberOfRows * dotHeightPlusPadding) + paddingBottom
        heightSize -= verticalSpacing // remove padding after last row

        if(passCodeLength == 0){
            heightSize = pinSize + paddingBottom
        }


        setMeasuredDimension(widthSize, heightSize.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val pl = paddingLeft
        val pr = paddingRight
        val pt = paddingTop
        val pb = paddingBottom

        val usableWidth = width - (pl + pr)
        val usableHeight = height - (pt + pb)

        if(showHint){
            val xPos = usableWidth.toFloat() / 2
            val yPos = (usableHeight / 2 - (hintPaint!!.descent() + hintPaint!!.ascent()) / 2)
            canvas.drawText(hint, xPos, yPos, hintPaint!!)
            return
        }

        val dotWidthPlusPadding = pinSize + horizontalSpacing
        val dotHeightPlusPadding = pinSize + verticalSpacing
        val maxDotsPerRow = floor(usableWidth / dotWidthPlusPadding)

        var currentRowDotCount = min(maxDotsPerRow.toInt(), passCodeLength)

        var startX = (usableWidth / 2) - ((dotWidthPlusPadding / 2) * currentRowDotCount) + horizontalSpacing
        var startY = 0f

        var currentRowDots = 0
        for (i in 1..passCodeLength) {

            circleRect!!.left = startX
            circleRect!!.top = startY
            circleRect!!.right = startX + pinSize
            circleRect!!.bottom = startY + pinSize

            canvas.drawArc(circleRect!!, circleRect!!.left, 360f, true, circlePaint!!)


            currentRowDots++
            if(currentRowDots >= maxDotsPerRow){

                val remainingDotCount = passCodeLength - i
                currentRowDotCount = min(maxDotsPerRow.toInt(), remainingDotCount)

                startX = (usableWidth / 2) - ((dotWidthPlusPadding / 2) * currentRowDotCount) + horizontalSpacing
                startY += dotHeightPlusPadding
                currentRowDots = 0

            }else{
                startX += dotWidthPlusPadding
            }
        }
    }

    var pinEntered: ((character: Char?, backspace: Boolean) -> Unit?)? = null
    var onEnter: (() -> Unit?)? = null
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if((keyCode <  KeyEvent.KEYCODE_0  || keyCode > KeyEvent.KEYCODE_9)
                && keyCode != KeyEvent.KEYCODE_DEL && keyCode != KeyEvent.KEYCODE_ENTER){
            return false
        }else if (rejectInput){
            return false
        }

        lock.lock()
        showHint = false
        if(keyCode == KeyEvent.KEYCODE_DEL){
            if(passCodeLength > 0){
                passCodeLength--
                pinEntered?.invoke(null, true)
            }
        }else if (keyCode == KeyEvent.KEYCODE_ENTER){
            if(passCodeLength > 0){
                onEnter?.invoke()
            }
        }else{
            passCodeLength++
            pinEntered?.invoke(event!!.unicodeChar.toChar(), false)
        }

        if(passCodeLength > 0){
            counterTextView?.text = passCodeLength.toString()
        }else{
            counterTextView?.text = null
            showHint = true
        }

        lock.unlock()

        requestLayout()
        invalidate()
        return true
    }

    override fun onClick(v: View?) {
        showKeyboard()
    }

    private fun showKeyboard(){
        this.requestFocus()
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }
}

class PinViewUtil(val pinView: PinView, counterView: TextView?, val pinStrength: ProgressBar? = null){

    var passCode: String = ""
    private val context = pinView.context

    var pinChanged: ((String) -> Unit?)? = null

    init {
        pinView.counterTextView = counterView
        pinView.reset()
        pinStrength?.progress = 0

        pinView.pinEntered = { c: Char?, backspace: Boolean ->
            if(backspace){
                if(passCode.isNotEmpty()){
                    passCode = passCode.substring(0, passCode.length-1)
                }
            }else{
                passCode += c.toString()
            }

            if(pinStrength != null){
                val progress = (Dcrlibwallet.shannonEntropy(passCode) / 4) * 100
                if (progress > 70) {
                    pinStrength.progressDrawable = context.resources.getDrawable(R.drawable.password_strength_bar_strong)
                } else {
                    pinStrength.progressDrawable = context.resources.getDrawable(R.drawable.password_strength_bar_weak)
                }

                pinStrength.progress = progress.toInt()
            }

            pinChanged?.invoke(passCode)

            Unit
        }
    }

    fun reset(){
        passCode = ""
        pinView.reset()
        pinStrength?.progress = 0
    }

    fun showHint(@StringRes hint: Int) = pinView.setHint(context.getString(hint))
}

