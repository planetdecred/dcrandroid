package com.dcrandroid.util

import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import mobilewallet.Mobilewallet
import java.util.regex.Pattern

class CoinFormat{

    companion object {
        fun format(str: String): Spannable {
            val doubleOrMoreDecimalPlaces = Pattern.compile("(([0-9]{1,3},*)+\\.)\\d{2,}").matcher(str)
            val oneDecimalPlace = Pattern.compile("(([0-9]{1,3},*)+\\.)\\d").matcher(str)
            val wholeNumber = Pattern.compile("\\d+").matcher(str)

            val spannable = SpannableString(str)

            val span = RelativeSizeSpan(1.2f)

            val startIndex: Int
            val endIndex: Int
            when {
                doubleOrMoreDecimalPlaces.find() -> {
                    startIndex = doubleOrMoreDecimalPlaces.start()
                    endIndex = str.indexOf(".", startIndex) + 3
                }
                oneDecimalPlace.find() -> {
                    startIndex = oneDecimalPlace.start()
                    endIndex = str.indexOf(".", startIndex) + 2
                }
                wholeNumber.find() -> {
                    startIndex = wholeNumber.start()
                    endIndex = wholeNumber.end()
                }
                else -> return spannable
            }

            spannable.setSpan(span, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            return spannable
        }

        fun format(amount: Long): Spannable{
            return format(Mobilewallet.amountCoin(amount))
        }

        fun format(amount: Double): Spannable {
            return format(Utils.removeTrailingZeros(amount) + " DCR")
        }
    }
}