/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.view

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextPaint
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.dcrandroid.R
import com.dcrandroid.extensions.show
import com.dcrandroid.view.util.ANIMATION_DURATION
import kotlinx.android.synthetic.main.custom_password_input.view.*

class PasswordInput : FrameLayout, TextWatcher {

    val textString: String
        get() = editText.text.toString()

    private var isMasked = true

    private lateinit var ivConcealReveal: ImageView
    private lateinit var editText: EditText

    private lateinit var errorTextView: TextView
    private lateinit var counterTextView: TextView
    private lateinit var hintTextView: TextView

    var validationMessage: String? = null
    var validateInput: ((String) -> Boolean)? = null

    val addressLayoutDefaultHeight = context.resources.getDimension(R.dimen.margin_padding_size_48)

    constructor(context: Context) : super(context) {
        init(context, null, 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs, defStyleAttr)
    }

    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        val view = inflate(context, R.layout.custom_password_input, null)

        ivConcealReveal = view.iv_conceal_reveal
        editText = view.password_input_et

        errorTextView = view.password_input_error
        counterTextView = view.password_input_counter
        hintTextView = view.password_input_hint

        editText.addTextChangedListener(this)
        editText.setOnFocusChangeListener { _, _ ->
            setupLayout()
        }

        ivConcealReveal.setOnClickListener {
            if (isMasked) {
                editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                ivConcealReveal.setImageResource(R.drawable.ic_reveal)
            } else {
                editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                ivConcealReveal.setImageResource(R.drawable.ic_conceal)
            }

            editText.setSelection(editText.text.length)
            editText.typeface = ResourcesCompat.getFont(context, R.font.source_sans_pro)

            isMasked = !isMasked
        }

        addView(view)
        setupLayout()

        val values = context.theme.obtainStyledAttributes(attrs, R.styleable.PinView, defStyleAttr, 0)
        val counterEnabled = values.getBoolean(R.styleable.PasswordInput_counter_enabled, false)

        if (counterEnabled) {
            counterTextView.show()
        }
    }

    fun setError(error: String?) {
        errorTextView.text = error
        setupLayout()
    }

    fun setHint(hint: Int) = password_input_hint.setText(hint)

    override fun afterTextChanged(s: Editable) {
        val valid = validateInput?.invoke(s.toString())
        if (valid != null && !valid) {
            password_input_error.text = validationMessage
        } else {
            password_input_error.text = null
        }

        password_input_counter.text = s.length.toString()

        setupLayout()
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    private fun setupLayout() {
        val active = editText.hasFocus() || editText.text.isNotEmpty()

        val textColor: Int
        val backgroundResource: Int
        val fontSizeTarget: Float

        when {
            errorTextView.text.isNotEmpty() -> {
                textColor = context.resources.getColor(R.color.orangeTextColor)
                backgroundResource = R.drawable.input_background_error
                fontSizeTarget = context.resources.getDimension(R.dimen.edit_text_size_14)
            }
            editText.hasFocus() -> {
                textColor = context.resources.getColor(R.color.blue)
                backgroundResource = R.drawable.input_background_active
                fontSizeTarget = context.resources.getDimension(R.dimen.edit_text_size_14)
            }
            else -> {
                textColor = context.resources.getColor(R.color.lightGrayTextColor)
                backgroundResource = R.drawable.input_background
                fontSizeTarget = if (editText.text.isNotEmpty()) {
                    context.resources.getDimension(R.dimen.edit_text_size_14)
                } else {
                    context.resources.getDimension(R.dimen.edit_text_size_16)
                }
            }
        }

        val translationYTarget = if (active) {
            -(hintTextView.height.toFloat() / 2)
        } else {

            val textPaint = TextPaint(hintTextView.paint)
            textPaint.textSize = fontSizeTarget

            val textHeight = textPaint.descent() - textPaint.ascent()
            (addressLayoutDefaultHeight / 2) - (textHeight / 2)
        }

        val fontSizeAnimator = ValueAnimator.ofFloat(hintTextView.textSize, fontSizeTarget)
        fontSizeAnimator.addUpdateListener {
            val animatedValue = (fontSizeAnimator.animatedValue as Float)
            hintTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, animatedValue)
        }

        val translationAnimator = ValueAnimator.ofFloat(hintTextView.translationY, translationYTarget)
        translationAnimator.addUpdateListener {
            val animatedValue = (translationAnimator.animatedValue as Float)
            hintTextView.translationY = animatedValue
        }

        val animatorSet = AnimatorSet()
        animatorSet.duration = ANIMATION_DURATION
        animatorSet.playTogether(fontSizeAnimator, translationAnimator)
        animatorSet.start()

        hintTextView.setTextColor(textColor)
        getChildAt(0).input_layout.setBackgroundResource(backgroundResource)
    }
}