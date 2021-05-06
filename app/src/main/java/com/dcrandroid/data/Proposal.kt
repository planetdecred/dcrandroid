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

    @SerializedName("fileversion")
    var indexFileVersion: String? = ""

    @SerializedName("votestatus")
    var voteStatus: Int = 0

    @SerializedName("yesvotes")
    var yesVotes: Int = 0

    @SerializedName("novotes")
    var noVotes: Int = 0

    val totalVotes: Int
        get() = yesVotes + noVotes

    val yesPercentage: Float
        get() {
            if (yesVotes == 0) return 0f
            return (yesVotes / totalVotes.toFloat()) * 100
        }

    val noPercentage: Float
        get() {
            if (noVotes == 0) return 0f
            return (noVotes / totalVotes.toFloat()) * 100
        }

    @SerializedName("eligibletickets")
    var eligibleTickets: Int = 0

    @SerializedName("quorumpercentage")
    var quorumPercentage: Int = 0

    @SerializedName("passpercentage")
    var passPercentage: Int = 0

    val quorum: Int
        get() = (eligibleTickets * (quorumPercentage / 100F)).toInt()

    val approved: Boolean
        get() = yesVotes >= (quorum * (passPercentage / 100F)) && yesPercentage >= passPercentage

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
                publishedAt = proposal.publishedAt
                timestamp = proposal.timestamp
                indexFile = proposal.indexFile
                indexFileVersion = proposal.indexFileVersion
                voteStatus = proposal.voteStatus
                yesVotes = proposal.yesVotes
                noVotes = proposal.noVotes
                eligibleTickets = proposal.eligibleTickets
                quorumPercentage = proposal.quorumPercentage
                passPercentage = proposal.passPercentage
            }
        }
    }
}