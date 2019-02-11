package com.dcrandroid.data

import java.io.Serializable
import java.util.*

class Proposal : Serializable {
    var name: String? = null
    var userid: String? = null
    var username: String? = null
    var publickey: String? = null
    var signature: String? = null
    var version: String? = null
    var status: Int = 0
    var state: Int = 0
    private var numcomments: Int = 0
    var timestamp: Long = 0
    var files: ArrayList<File>? = null
    var censorshipRecord: CensorshipRecord? = null
    var voteStatus: VoteStatus? = null

    fun getNumcomments(): Int? {
        return numcomments
    }

    fun setNumcomments(numcomments: Int?) {
        this.numcomments = numcomments!!
    }

    inner class File : Serializable{
        var name: String? = null
        var mime: String? = null
        var digest: String? = null
        var payload: String? = null
    }

    inner class CensorshipRecord : Serializable {
        var token: String? = null
        var merkle: String? = null
        var signature: String? = null
    }

    inner class VoteStatus : Serializable {
        var token: String? = null
        var status: Int = 0
        var totalvotes: Int = 0
        var yes: Int = 0
        var no: Int = 0
    }
}
