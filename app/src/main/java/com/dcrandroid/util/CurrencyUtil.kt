/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

val usdAmountFormat: DecimalFormat = DecimalFormat("###,###,###,###,###.####")
val usdAmountFormat2: DecimalFormat = DecimalFormat("###,###,###,###,###.##")

object CurrencyUtil {
    fun dcrToFormattedUSD(exchangeDecimal: BigDecimal?, dcr: Double, scale: Int = 4): String {
        if (scale == 4) {
            return usdAmountFormat.format(
                dcrToUSD(exchangeDecimal, dcr)!!.setScale(scale, BigDecimal.ROUND_HALF_EVEN)
                    .toDouble()
            )
        }
        return usdAmountFormat2.format(
            dcrToUSD(exchangeDecimal, dcr)!!.setScale(scale, BigDecimal.ROUND_HALF_EVEN).toDouble()
        )
    }

    fun dcrToUSD(exchangeDecimal: BigDecimal?, dcr: Double): BigDecimal? {
        val dcrDecimal = BigDecimal(dcr)
        return exchangeDecimal?.multiply(dcrDecimal)
    }

    fun usdToDCR(exchangeDecimal: BigDecimal?, usd: Double): BigDecimal? {
        if (exchangeDecimal == null) {
            return null
        }

        val usdDecimal = BigDecimal(usd)
        // using 8 to be safe
        return usdDecimal.divide(exchangeDecimal, 8, RoundingMode.HALF_EVEN)
    }
}
