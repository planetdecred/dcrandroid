/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.view.util

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.text.Editable
import android.text.TextPaint
import android.text.TextWatcher
import android.util.TypedValue
import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.StringRes
import com.dcrandroid.R
import com.dcrandroid.activities.ReaderActivity
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.show
import com.dcrandroid.util.Utils
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.custom_input.view.*

const val ANIMATION_DURATION = 167L
const val SCAN_QR_REQUEST_CODE = 100

/*
    Using this class for a normal input
    - always return true for validateInput
    - hideQrScanner()
    - optionally hideErrorRow()
 */
class InputHelper(
    private val context: Context, private val container: View,
    val validateInput: (String) -> Boolean
) : View.OnFocusChangeListener, View.OnClickListener, TextWatcher {

    var textChanged: () -> Unit = {}
    val validatedInput: String?
        get() {
            val enteredText = editText.text.toString()
            if (validateInput(enteredText)) {
                return enteredText
            }

            return null
        }

    val pasteTextView = container.custom_input_paste
    var pasteHidden = false

    val clearBtn = container.custom_input_clear
    var clearBtnHidden = false

    val addressLayoutDefaultHeight = context.resources.getDimension(R.dimen.margin_padding_size_48)
    val addressLayout = container.input_layout

    var validationMessage: Int = R.string.invalid_address
    val errorTextView = container.custom_input_error

    val hintTextView = container.custom_input_hint
    val editText = container.custom_input_et
    val qrScanImageView = container.iv_scan
    val pasteQRLayout = container.paste_qr_layout

    init {
        hintTextView.minWidth = hintTextView.width
        editText.onFocusChangeListener = this
        editText.addTextChangedListener(this)
        qrScanImageView.setOnClickListener(this)
        pasteTextView.setOnClickListener(this)
        clearBtn.setOnClickListener(this)

        if (addressLayout.viewTreeObserver.isAlive) {
            addressLayout.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (addressLayout.height > 0) {
                        addressLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        setupLayout()
                    }
                }

            })
        }

        setupLayout()
        setupButtons()
    }

    fun setupLayout() {
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

        val translationAnimator =
            ValueAnimator.ofFloat(hintTextView.translationY, translationYTarget)
        translationAnimator.addUpdateListener {
            val animatedValue = (translationAnimator.animatedValue as Float)
            hintTextView.translationY = animatedValue
        }

        val animatorSet = AnimatorSet()
        animatorSet.duration = ANIMATION_DURATION
        animatorSet.playTogether(fontSizeAnimator, translationAnimator)
        animatorSet.start()

        hintTextView.setTextColor(textColor)
        container.input_layout.setBackgroundResource(backgroundResource)
    }

    fun setEnabled(enabled: Boolean) {
        editText.isEnabled = false
        clearBtn.isEnabled = enabled
        qrScanImageView.isEnabled
        pasteTextView.isEnabled = enabled
    }

    private fun setupButtons() {
        if (editText.text.isNotEmpty()) {
            if (!clearBtnHidden)
                clearBtn.show()

            pasteQRLayout.hide()
        } else {
            clearBtn.hide()
            setupPasteButton()
            pasteQRLayout.show()
        }
    }

    private fun setupPasteButton() {
        val clipBoardContent = Utils.readFromClipboard(context)
        if (clipBoardContent.isNotBlank() && validateInput(clipBoardContent) && editText.text.isEmpty() && !pasteHidden) {
            pasteTextView.show()
        } else {
            pasteTextView.hide()
        }
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.custom_input_paste -> {
                val clip = Utils.readFromClipboard(context)
                if (clip.isNotBlank()) {
                    editText.setText(clip)
                    editText.setSelection(editText.text.length)
                }
            }
            R.id.iv_scan -> {
                val integrator = IntentIntegrator(context as Activity)
                integrator.captureActivity = ReaderActivity::class.java
                integrator.setRequestCode(SCAN_QR_REQUEST_CODE)
                integrator.initiateScan()
            }
            R.id.custom_input_clear -> {
                editText.text = null
            }
        }
    }

    override fun onFocusChange(v: View?, hasFocus: Boolean) {
        setupLayout()
    }

    override fun afterTextChanged(s: Editable?) {

        if (s.isNullOrEmpty()) {
            setError(null) // this calls setup layout
            setupButtons()

            validateInput("") // trigger validators to show errors if setup
        } else {
            val enteredText = s.toString()
            if (validateInput(enteredText)) { // address is valid
                setError(null)
            } else {
                setError(context.getString(validationMessage))
            }

            setupLayout()
            setupButtons()
        }

        textChanged()
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    fun setHint(@StringRes hint: Int) {
        hintTextView.setText(hint)
    }

    fun setError(error: String?) {
        container.custom_input_error.text = error
        setupLayout()
    }

    // also returns false if address is empty
    fun isInvalid(): Boolean {
        val enteredAddress = editText.text.toString()
        if (enteredAddress.isNotEmpty()) {
            return !validateInput(enteredAddress)
        }

        return false
    }

    fun hideErrorRow() {
        container.custom_input_error_layout.hide()
    }

    fun hideQrScanner() {
        qrScanImageView.hide()
    }

    fun hidePasteButton() {
        pasteHidden = true
        pasteTextView.hide()
    }

    fun hideClearButton() {
        clearBtnHidden = true
        clearBtn.hide()
    }

    fun isVisible(): Boolean {
        return container.visibility == View.VISIBLE
    }

    fun show() {
        container.show()
    }

    fun hide() {
        container.hide()
    }

    fun onResume() {
        setupButtons()
    }
}