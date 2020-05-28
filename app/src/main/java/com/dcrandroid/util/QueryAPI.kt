/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.util

import android.os.AsyncTask
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class GetExchangeRate(private val userAgent: String,
                      private val callback: ExchangeRateCallback) : AsyncTask<Void, String, String>() {

    val exchangeURL = "https://bittrex.com/api/v1.1/public/getticker?market=USDT-DCR"

    override fun doInBackground(vararg voids: Void): String? {
        try {
            val url = URL(apiURL)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.readTimeout = 7000
            connection.connectTimeout = 7000
            connection.setRequestProperty("User-Agent", userAgent)
            val br = BufferedReader(InputStreamReader(connection.inputStream))
            val result = StringBuilder()
            br.lineSequence().forEach {
                result.append(it)
            }
            br.close()
            connection.disconnect()
            return result.toString()
        } catch (e: MalformedURLException) {
            e.printStackTrace()
            callback.onQueryAPIError(e)
        } catch (e: IOException) {
            e.printStackTrace()
            callback.onQueryAPIError(e)
        }

        return null
    }

    override fun onPostExecute(s: String?) {
        super.onPostExecute(s)

        if (s != null) {
            val rate = BittrexRateParser.parse(s)
            callback.onExchangeRateSuccess(rate)
        }
    }

    interface ExchangeRateCallback {
        fun onExchangeRateSuccess(rate: BittrexRateParser)
        fun onExchangeRateError(e: Exception)
    }

    class BittrexRateParser {

        @SerializedName("Bid")
        var bid: Double = 0.0

        @SerializedName("Ask")
        var ask: Double = 0.0

        @SerializedName("Last")
        var last: Double = 0.0

        val usdRate: BigDecimal
            get() = BigDecimal(last)

        companion object {
            fun parse(resultJsonString: String): BittrexRateParser {
                val resultJson = JSONObject(resultJsonString)
                val result = resultJson.getJSONObject("result")
                return Gson().fromJson(result.toString(), BittrexRateParser::class.java)
            }
        }

    }
}