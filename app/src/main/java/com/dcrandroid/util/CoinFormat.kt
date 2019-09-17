/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.util

import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import dcrlibwallet.Dcrlibwallet
import java.util.regex.Pattern

class CoinFormat {

    companion object {
        fun format(str: String, relativeSize: Float = 0.7f): Spannable {
            val spannable = SpannableString(str)

            return formatSpannable(spannable, relativeSize)
        }

        fun formatSpannable(spannable: Spannable, relativeSize: Float = 0.7f): Spannable{

            val removeRelativeSpan = spannable.getSpans(0, spannable.length, RelativeSizeSpan::class.java)
            for(span in removeRelativeSpan){
                spannable.removeSpan(span)
            }

            val doubleOrMoreDecimalPlaces = Pattern.compile("(([0-9]{1,3},*)+\\.)\\d{2,}").matcher(spannable)
            val oneDecimalPlace = Pattern.compile("(([0-9]{1,3},*)+\\.)\\d").matcher(spannable)
            val noDecimal = Pattern.compile("([0-9]{1,3},*)+").matcher(spannable)

            val span = RelativeSizeSpan(relativeSize)

            val startIndex: Int
            val endIndex: Int
            when {
                doubleOrMoreDecimalPlaces.find() -> {
                    startIndex = spannable.indexOf(".", doubleOrMoreDecimalPlaces.start()) + 3
                    endIndex = spannable.length
                }
                oneDecimalPlace.find() -> {
                    startIndex = spannable.indexOf(".", oneDecimalPlace.start()) + 2
                    endIndex = spannable.length
                }
                noDecimal.find() -> {
                    startIndex = noDecimal.end()
                    endIndex = spannable.length
                }
                else -> return spannable
            }

            spannable.setSpan(span, startIndex, endIndex, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            return spannable
        }

        fun format(amount: Long, relativeSize: Float = 0.7f): Spannable {
            return format(Dcrlibwallet.amountCoin(amount), relativeSize)
        }

        fun format(amount: Double, relativeSize: Float = 0.7f): Spannable {
            return format(Utils.removeTrailingZeros(amount) + " DCR", relativeSize)
        }
    }
}