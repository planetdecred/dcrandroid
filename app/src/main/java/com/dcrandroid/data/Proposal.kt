/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.data

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.*

class Proposal : Serializable {
    @SerializedName("name")
    var name: String? = ""

    @SerializedName("userid")
    var userid: String? = ""

    @SerializedName("username")
    var username: String? = ""

    @SerializedName("publickey")
    var publickey: String? = ""

    @SerializedName("signature")
    var signature: String? = ""

    @SerializedName("version")
    var version: String? = ""

    @SerializedName("status")
    var status: Int = 0

    @SerializedName("state")
    var state: Int = 0

    @SerializedName("numcomments")
    private var numcomments: Int = 0

    @SerializedName("timestamp")
    var timestamp: Long = 0

    @SerializedName("files")
    var files: ArrayList<File>? = null

    @SerializedName("censorshiprecord")
    var censorshipRecord: CensorshipRecord? = null

    @SerializedName("votestatus")
    var voteStatus: VoteStatus? = null

    fun getNumcomments(): Int? {
        return numcomments
    }

    fun setNumcomments(numcomments: Int?) {
        this.numcomments = numcomments!!
    }

    inner class File : Serializable {
        @SerializedName("name")
        var name: String? = null

        @SerializedName("mime")
        var mime: String? = null

        @SerializedName("digest")
        var digest: String? = null

        @SerializedName("payload")
        var payload: String? = null
    }

    inner class CensorshipRecord : Serializable {
        @SerializedName("token")
        var token: String? = null

        @SerializedName("merkle")
        var merkle: String? = null

        @SerializedName("signature")
        var signature: String? = null
    }

    inner class VoteStatus : Serializable {
        @SerializedName("token")
        var token: String? = null

        @SerializedName("status")
        var status: Int = 0

        @SerializedName("totalvotes")
        var totalvotes: Int = 0

        @SerializedName("yes")
        var yes: Int = 0

        @SerializedName("no")
        var no: Int = 0
    }

    companion object {
        fun from(proposalJson: String): Proposal {
            return Gson().fromJson(proposalJson, Proposal::class.java)
        }
    }
}