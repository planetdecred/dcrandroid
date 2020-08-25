/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.data

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.Serializable

class Proposal : Serializable {
    @SerializedName("ID")
    var id: Long = 0

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
    var files: Array<File>? = null

    @SerializedName("censorshiprecord")
    var censorshipRecord: CensorshipRecord? = null

    @SerializedName("votestatus")
    var voteStatus: VoteStatus? = null

    @SerializedName("votesummary")
    var voteSummary: VoteSummary? = null

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

        @SerializedName("optionsresult")
        var optionsResults: Array<OptionsResult>? = null

        @SerializedName("passpercentage")
        var passpercentage: Int = 0
    }

    inner class VoteSummary : Serializable {
        @SerializedName("status")
        var status: Int = 0

        @SerializedName("approved")
        var approved: Boolean = false

        @SerializedName("results")
        var optionsResults: Array<OptionsResult>? = null

        @SerializedName("passpercentage")
        var passpercentage: Int = 0
    }

    class OptionsResult : Serializable {
        @SerializedName("option")
        var voteOption: VoteOption? = null

        @SerializedName("votesreceived")
        var votesreceived: Int = 0
    }

    inner class VoteOption : Serializable {
        @SerializedName("id")
        var id: String? = null

        @SerializedName("description")
        var description: String? = null

        @SerializedName("bits")
        var bits: Int = 0
    }

    companion object {
        fun from(proposalJson: String): Proposal {
            return Gson().fromJson(proposalJson, Proposal::class.java)
        }
    }
}