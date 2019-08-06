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
        fun format(str: String): Spannable {
            val doubleOrMoreDecimalPlaces = Pattern.compile("(([0-9]{1,3},*)+\\.)\\d{2,}").matcher(str)
            val oneDecimalPlace = Pattern.compile("(([0-9]{1,3},*)+\\.)\\d").matcher(str)
            val noDecimal = Pattern.compile("([0-9]{1,3},*)+").matcher(str)

            val spannable = SpannableString(str)

            val span = RelativeSizeSpan(0.6f)

            val startIndex: Int
            val endIndex: Int
            when {
                doubleOrMoreDecimalPlaces.find() -> {
                    startIndex = str.indexOf(".", doubleOrMoreDecimalPlaces.start()) + 3
                    endIndex = str.length
                }
                oneDecimalPlace.find() -> {
                    startIndex = str.indexOf(".", oneDecimalPlace.start()) + 2
                    endIndex = str.length
                }
                noDecimal.find() -> {
                    startIndex = noDecimal.end()
                    endIndex = str.length
                }
                else -> return spannable
            }

            spannable.setSpan(span, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            return spannable
        }

        fun format(amount: Long): Spannable {
            return format(Dcrlibwallet.amountCoin(amount))
        }

        fun format(amount: Double): Spannable {
            return format(Utils.removeTrailingZeros(amount) + " DCR")
        }
    }
}