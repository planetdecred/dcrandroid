/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.util

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.CharacterStyle
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import androidx.annotation.ColorInt
import dcrlibwallet.Dcrlibwallet
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*
import java.util.regex.Pattern

object CoinFormat {

    val dcrWithCommas = "#,###,###,##0.########"
    val dcrWithCommasAndZeros = "#,###,###,##0.00000000"
    val dcrWithoutCommas = "#########0.########"

    fun format(str: String, relativeSize: Float = 0.7f): Spannable {
        val spannable = SpannableString(str)
        return formatRelative(spannable, relativeSize)
    }

    fun formatRelative(spannable: Spannable, relativeSize: Float = 0.7f): Spannable {
        val span = RelativeSizeSpan(relativeSize)
        return formatSpannable(spannable, span)
    }

    fun applyColor(spannable: Spannable, @ColorInt color: Int): Spannable {
        val span = ForegroundColorSpan(color)

        spannable.setSpan(span, 0, spannable.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        return spannable
    }

    fun formatAlpha(dcr: Long): Spannable {
        val str = formatDecred(Dcrlibwallet.amountCoin(dcr)) + " DCR"
        val spannable = SpannableString(str)
        val span = ForegroundColorSpan(Color.parseColor("#596D81"))
        return formatSpannable(spannable, span)
    }

    private fun formatSpannable(spannable: Spannable, span: CharacterStyle): Spannable {

        val removeRelativeSpan = spannable.getSpans(0, spannable.length, RelativeSizeSpan::class.java)
        for (s in removeRelativeSpan) {
            spannable.removeSpan(s)
        }

        val doubleOrMoreDecimalPlaces = Pattern.compile("(([0-9]{1,3},*)*\\.)\\d{2,}").matcher(spannable)
        val oneDecimalPlace = Pattern.compile("(([0-9]{1,3},*)*\\.)\\d").matcher(spannable)
        val noDecimal = Pattern.compile("([0-9]{1,3},*)+").matcher(spannable)

        var startIndex: Int = -1

        if (noDecimal.find()) {
            startIndex = noDecimal.end()
        }

        if (oneDecimalPlace.find()) {
            val start = spannable.indexOf(".", oneDecimalPlace.start())
            if (start <= startIndex || startIndex == -1) {
                startIndex = start + 2
            }
        }

        if (doubleOrMoreDecimalPlaces.find()) {
            val start = spannable.indexOf(".", doubleOrMoreDecimalPlaces.start())
            if (start <= startIndex || startIndex == -1) {
                startIndex = start + 3
            }
        }

        if (startIndex == -1) {
            return spannable
        }

        spannable.setSpan(span, startIndex, spannable.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        return spannable
    }

    fun format(amount: Long, relativeSize: Float = 0.7f, suffix: String = " DCR"): Spannable {
        return format(Dcrlibwallet.amountCoin(amount), relativeSize, suffix)
    }

    fun format(amount: Double, relativeSize: Float = 0.7f, suffix: String = " DCR"): Spannable {
        return format(formatDecred(amount) + suffix, relativeSize)
    }

    fun formatDecred(dcr: Double, pattern: String = dcrWithCommasAndZeros) = formatDecred(Dcrlibwallet.amountAtom(dcr), pattern)

    fun formatDecred(dcr: Long, pattern: String = dcrWithCommasAndZeros): String {
        val convertedDcr = Dcrlibwallet.amountCoin(dcr)
        val df = NumberFormat.getNumberInstance(Locale.ENGLISH) as DecimalFormat
        df.applyPattern(pattern)
        return df.format(convertedDcr)
    }
}