/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.view.util

import android.animation.AnimatorSet
import android.content.Context
import android.view.View
import kotlinx.android.synthetic.main.address_input.view.*
import android.util.TypedValue
import com.dcrandroid.R
import android.animation.ValueAnimator
import android.app.Activity
import android.text.Editable
import android.text.TextPaint
import android.text.TextWatcher
import android.view.ViewTreeObserver
import com.dcrandroid.activities.ReaderActivity
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.show
import com.dcrandroid.util.Utils
import com.google.zxing.integration.android.IntentIntegrator

const val ANIMATION_DURATION = 167L
const val SCAN_QR_REQUEST_CODE = 100

/*
    Using this class for a normal input
    - always return true for validateAddress
    - hideQrScanner()
    - optionally hideErrorRow()
 */
class AddressInputHelper(private val context: Context, private val container: View,
                         val validateAddress:(String) -> Boolean) : View.OnFocusChangeListener, View.OnClickListener, TextWatcher {

    lateinit var textChanged:() -> Unit
    val address: String?
    get() {
        val enteredAddress = editText.text.toString()
        if(validateAddress(enteredAddress)){
            return enteredAddress
        }

        return null
    }

    val pasteTextView = container.send_dest_paste
    var pasteHidden = false

    val addressLayoutDefaultHeight = context.resources.getDimension(R.dimen.margin_padding_size_48)
    val addressLayout = container.destination_address_layout

    val hintTextView = container.send_dest_hint
    val errorTextView =  container.send_dest_error
    val editText = container.send_dest_et
    val qrScanImageView = container.iv_scan

    init {
        hintTextView.minWidth = hintTextView.width
        editText.onFocusChangeListener = this
        editText.addTextChangedListener(this)
        qrScanImageView.setOnClickListener(this)
        pasteTextView.setOnClickListener(this)

        if(addressLayout.viewTreeObserver.isAlive){
            addressLayout.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener{
                override fun onGlobalLayout() {
                    if(addressLayout.height > 0){
                        addressLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        setupLayout()
                    }
                }

            })
        }

        setupLayout()
        setupPasteButton()
    }

    fun setupLayout(){
        val active = editText.hasFocus() || editText.text.isNotEmpty()

        val fontSizeTarget: Float
        val translationYTarget: Float

        if(active){

            fontSizeTarget = context.resources.getDimension(R.dimen.edit_text_size_14)
            translationYTarget = - (hintTextView.height.toFloat() / 2)

        }else{

            fontSizeTarget = context.resources.getDimension(R.dimen.edit_text_size_16)

            val textPaint = TextPaint(hintTextView.paint)
            textPaint.textSize = fontSizeTarget

            val textHeight = textPaint.descent() - textPaint.ascent()
            translationYTarget = (addressLayoutDefaultHeight / 2) - (textHeight / 2)
        }

        val textColor: Int
        val backgroundResource: Int

        when {
            errorTextView.text.isNotEmpty() -> {
                backgroundResource = R.drawable.input_background_error
                textColor = context.resources.getColor(R.color.orangeTextColor)
            }
            editText.hasFocus() -> {
                textColor = context.resources.getColor(R.color.blue)
                backgroundResource = R.drawable.input_background_active
            }
            else -> {
                textColor = context.resources.getColor(R.color.lightGrayTextColor)
                backgroundResource = R.drawable.input_background
            }
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
        container.destination_address_layout.setBackgroundResource(backgroundResource)
    }

    private fun setupPasteButton(){
        if(Utils.readFromClipboard(context).isNotBlank() && editText.text.isEmpty() && !pasteHidden){
            pasteTextView.show()
        }else{
            pasteTextView.hide()
        }
    }

    fun scanQRSuccess(result: String){
        editText.setText(result)
    }

    override fun onClick(v: View?) {
        when(v!!.id){
            R.id.send_dest_paste -> {
                val clip = Utils.readFromClipboard(context)
                if(clip.isNotBlank()){
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
        }
    }

    override fun onFocusChange(v: View?, hasFocus: Boolean) {
        setupLayout()
    }

    override fun afterTextChanged(s: Editable?) {

        if(s.isNullOrEmpty()){
            setError(null) // this calls setup layout
            setupPasteButton()
        }else{
            val enteredAddress = s.toString()
            if(validateAddress(enteredAddress)){ // address is valid
                setError(null)
            }else{
                setError(context.getString(R.string.invalid_address))
            }

            setupLayout()
            setupPasteButton()
        }

        textChanged()
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    fun setHint(hint: String){
        hintTextView.text = hint
    }

    fun setError(error: String?){
        container.send_dest_error.text = error
        setupLayout()
    }

    // also returns false if address is empty
    fun isInvalid(): Boolean {
        val enteredAddress = editText.text.toString()
        if(enteredAddress.isNotEmpty()){
            return !validateAddress(enteredAddress)
        }

        return false
    }

    fun hideErrorRow(){
        container.dest_address_error_layout.hide()
    }

    fun hideQrScanner(){
        qrScanImageView.hide()
    }

    fun hidePasteButton(){
        pasteTextView.hide()
    }

    fun isVisible(): Boolean{
        return container.visibility == View.VISIBLE
    }

    fun show(){
        container.show()
    }

    fun hide(){
        container.hide()
    }

    fun onResume(){
        setupPasteButton()
    }

    fun onPause(){
        setupPasteButton()
    }
}