/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.util

import com.dcrandroid.data.Transaction.TransactionInput
import com.dcrandroid.data.Transaction.TransactionOutput
import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class Deserializer {
    class TransactionDeserializer : JsonDeserializer<Any> {
        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Any? {
            val jsonArray = json!!.asJsonArray
            if (jsonArray.size() <= 0) {
                return ArrayList<Any>()
            }

            val gson = Gson()

            val check = jsonArray.get(0).asJsonObject
            //Previous account is an attribute of debit transaction that doesn't exist in credit transaction.
            if (check.has("PreviousAccount")) {
                val inputs = ArrayList<TransactionInput>()
                for (elem in jsonArray) {
                    val input = gson.fromJson(elem.toString(), TransactionInput::class.java)
                    inputs.add(input)
                }

                return inputs
            } else if (check.has("Account")) {
                val outputs = ArrayList<TransactionOutput>()
                for (elem in jsonArray) {
                    val output = gson.fromJson(elem.toString(), TransactionOutput::class.java)
                    outputs.add(output)
                }

                return outputs
            }

            return null
        }

    }
}