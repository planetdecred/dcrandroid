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

    @SerializedName("token")
    var token: String? = ""

    @SerializedName("name")
    var name: String? = ""

    @SerializedName("userid")
    var userid: String? = ""

    @SerializedName("username")
    var username: String? = ""

    @SerializedName("version")
    var version: String? = ""

    @SerializedName("status")
    var status: Int = 0

    @SerializedName("numcomments")
    var numcomments: Int = 0

    @SerializedName("timestamp")
    var timestamp: Long = 0

    @SerializedName("publishedat")
    var publishedAt: Long = 0

    @SerializedName("indexfile")
    var indexFile: String? = ""

    @SerializedName("votestatus")
    var voteStatus: Int = 0

    @SerializedName("voteapproved")
    var voteApproved: Boolean = false

    @SerializedName("yesvotes")
    var yesVotes: Int = 0

    @SerializedName("novotes")
    var noVotes: Int = 0

    @SerializedName("passpercentage")
    var passPercentage: Int = 0

    companion object {
        fun from(proposalJson: String): Proposal {
            return Gson().fromJson(proposalJson, Proposal::class.java)
        }

        fun from(proposal: dcrlibwallet.Proposal): Proposal {
            return Proposal().apply {
                id = proposal.id
                token = proposal.token
                name = proposal.name
                userid = proposal.userID
                username = proposal.username
                version = proposal.version
                status = proposal.status
                numcomments = proposal.numComments
                timestamp = proposal.timestamp
                indexFile = proposal.indexFile
                voteStatus = proposal.voteStatus
                voteApproved = proposal.voteApproved
                yesVotes = proposal.yesVotes
                noVotes = proposal.noVotes
                passPercentage = proposal.passPercentage
            }
        }
    }
}