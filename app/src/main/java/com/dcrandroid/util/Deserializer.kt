/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.util

import com.dcrandroid.data.Constants
import com.dcrandroid.data.Proposal
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
            // PreviousAccount is an attribute of debit transaction that doesn't exist in credit transaction.
            if (check.has(Constants.PREVIOUS_ACCOUNT)) {
                val inputs = ArrayList<TransactionInput>()
                for (elem in jsonArray) {
                    val input = gson.fromJson(elem.toString(), TransactionInput::class.java)
                    inputs.add(input)
                }

                return inputs
            } else if (check.has(Constants.ACCOUNT)) {
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

    class ProposalDeserializer : JsonDeserializer<Any> {
        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Any? {
            val jsonArray = json!!.asJsonArray
            if (jsonArray.size() <= 0) {
                return null
            }

            val gson = Gson()
            val check = jsonArray.get(0).asJsonObject
            //TODO(c-ollins): complete code before merge
            if(check.has(Constants.FILES)){
                val files = ArrayList<Proposal.PoliteiaFile>()
                for (elem in jsonArray) {
                    val file = gson.fromJson(elem.toString(), Proposal.PoliteiaFile::class.java)
                    files.add(file)
                }

                return files
            }

            return ArrayList<Any>()
        }
    }

    class OptionsResultDeserializer : JsonDeserializer<Any> {
        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Proposal.VoteStatus.OptionsResult? {
            val jsonArray = json!!.asJsonArray
            if (jsonArray.size() <= 0) {
                return Proposal.VoteStatus.OptionsResult(0, 0)
            }

            val check = jsonArray.get(0).asJsonObject
            val result = Proposal.VoteStatus.OptionsResult()

            // votesreceived is an integer attribute that is a part of
            // optionsresult.
            if(check.has(Constants.VOTES_RECEIVED)){
                for(elem in jsonArray){
                    val option = elem.asJsonObject.get(Constants.OPTION).asJsonObject
                    if(option.get(Constants.ID).asString == Constants.YES){
                        result.yes = elem.asJsonObject.get(Constants.VOTES_RECEIVED).asInt
                    }else{
                        result.no = elem.asJsonObject.get(Constants.VOTES_RECEIVED).asInt
                    }
                }
            }

            return result
        }
    }

}