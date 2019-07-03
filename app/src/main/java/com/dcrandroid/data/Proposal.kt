/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.data

import com.google.gson.annotations.SerializedName
import java.io.Serializable

class Proposal : Serializable {
    @SerializedName("name")
    var name: String? = null
    @SerializedName("userid")
    var userID: String? = null
    @SerializedName("username")
    var username: String? = null
    @SerializedName("publickey")
    var publicKey: String? = null
    @SerializedName("signature")
    var signature: String? = null
    @SerializedName("version")
    var version: String? = null
    @SerializedName("status")
    var status: Int = 0
    @SerializedName("state")
    var state: Int = 0
    @SerializedName("numcomments")
    var numComments: Int = 0
    @SerializedName("timestamp")
    var timestamp: Long = 0
    @SerializedName("files")
    var files: ArrayList<PoliteiaFile>? = null
    @SerializedName("censorshiprecord")
    var censorshipRecord: CensorshipRecord? = null
    var voteStatus: VoteStatus? = null

    class PoliteiaFile : Serializable {
        @SerializedName("name")
        var name: String? = null
        @SerializedName("mime")
        var mime: String? = null
        @SerializedName("digest")
        var digest: String? = null
        @SerializedName("payload")
        var payload: String? = null
    }

    class CensorshipRecord : Serializable {
        @SerializedName("token")
        var token: String? = null
        @SerializedName("merkle")
        var merkle: String? = null
        @SerializedName("signature")
        var signature: String? = null
    }

    class VoteStatus : Serializable {
        @SerializedName("token")
        var token: String? = null
        @SerializedName("status")
        var status: Int = 0
        @SerializedName("totalvotes")
        var totalVotes: Int = 0
        @SerializedName("optionsresult")
        var optionsResult: OptionsResult? = null
        @SerializedName("endheight")
        var endHeight: String? = null
        @SerializedName("numofeligiblevotes")
        var numOfEligibleVotes: Int = 0
        @SerializedName("quorumpercentage")
        var quorumPercentage: Int = 0
        @SerializedName("passpercentage")
        var passPercentage: Int = 0

        class OptionsResult(var yes: Int = 0, var no: Int = 0) : Serializable
    }
}
