/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.util

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

const val KEY_ENCRYPT = "encrypt"
const val KEY_INSECURE_PUB_PASSPHRASE = "public"
const val KEY_STARTUP_PASSPHRASE_TYPE = "encrypt_passphrase_type"
const val KEY_SPENDING_PASSPHRASE_TYPE = "spending_passphrase_type"
const val KEY_PASSWORD = "password"
const val KEY_PEER_IP = "peer_ip"

class V1PreferenceUtil private constructor() {

    private lateinit var preferences: SharedPreferences

    companion object{
        fun with(ctx: Context): V1PreferenceUtil{
            val util = V1PreferenceUtil()
            util.preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
            return  util
        }
    }

    fun getBoolean(key: String, default: Boolean)= preferences.getBoolean(key, default)

    fun set(key: String, value: Boolean) = preferences.edit().putBoolean(key, value).apply()


    fun getString(key: String, default: String) : String = preferences.getString(key, default)!!

    fun set(key: String, value: String) = preferences.edit().putString(key, value).apply()
}