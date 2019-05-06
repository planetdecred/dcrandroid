/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.util

import android.os.AsyncTask
import com.dcrandroid.data.Constants
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class GetExchangeRate(private val exchangeURL: String, private val userAgent: String,
                      private val callback: ExchangeRateCallback) : AsyncTask<Void, String, String>() {

    override fun doInBackground(vararg voids: Void): String? {
        try {
            val url = URL(exchangeURL)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.readTimeout = 7000
            connection.connectTimeout = 7000
            connection.setRequestProperty(Constants.USER_AGENT, userAgent)
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
            callback.onExchangeRateError(e)
        } catch (e: IOException) {
            e.printStackTrace()
            callback.onExchangeRateError(e)
        }

        return null
    }

    override fun onPostExecute(s: String?) {
        super.onPostExecute(s)
        callback.onExchangeRateSuccess(s)
    }

    interface ExchangeRateCallback {
        fun onExchangeRateSuccess(result: String?)
        fun onExchangeRateError(e: Exception)
    }
}