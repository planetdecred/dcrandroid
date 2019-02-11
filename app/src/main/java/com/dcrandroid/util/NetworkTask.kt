package com.dcrandroid.util

import android.os.AsyncTask
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.*


class NetworkTask(config: HashMap<String, String>, private var delegate: AsyncResponse) : AsyncTask<String, Int, String>() {
    private var config: HashMap<*, *> = config
    override fun doInBackground(vararg strings: String): String? {
        try {
            val url = URL(if (config["url"] == null) "" else config["url"].toString())
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = if (config["method"] == null) "GET" else config["method"].toString().toUpperCase()
            connection.doInput = true
            connection.readTimeout = if (config["readtimeout"] == null) 7000 else Integer.valueOf(config["readtimeout"].toString())
            connection.connectTimeout = if (config["connecttimeout"] == null) 7000 else Integer.valueOf(config["connecttimeout"].toString())
            val bufferedReader = BufferedReader(InputStreamReader(connection.inputStream))
            var line: String? = bufferedReader.readLine()
            val result = StringBuilder()
            while (line != null) {
                result.append(line)
                line = bufferedReader.readLine()
            }
            bufferedReader.close()
            connection.disconnect()
            delegate.onResponse(result.toString())
        } catch (e: MalformedURLException) {
            try {
                delegate.onFailure(e)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        } catch (e: IOException) {
            try {
                delegate.onFailure(e)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        return null
    }

    interface AsyncResponse {
        fun onResponse(response: String)
        fun onFailure(t: Throwable)
    }
}

