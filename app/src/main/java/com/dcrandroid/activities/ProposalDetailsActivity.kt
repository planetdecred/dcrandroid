package com.dcrandroid.activities

import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.dcrandroid.R
import com.dcrandroid.util.NetworkTask
import com.dcrandroid.util.NetworkTask.AsyncResponse
import kotlinx.android.synthetic.main.activity_proposal_details.*
import org.jetbrains.annotations.NotNull
import org.json.JSONException
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class ProposalDetailsActivity  : BaseActivity() {

    private lateinit var tv_title: TextView
    private lateinit var tv_description: TextView
    private lateinit var tv_progress: TextView
    private lateinit var tv_meta: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var vote_progress: ProgressBar


    var proposal: Proposal? = null

       override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_proposal_details)

        tv_title  = findViewById(R.id.proposal_title)
        tv_description = findViewById(R.id.proposal_description)
        tv_progress = findViewById(R.id.vote_progress)
//        tv_meta = findViewById(R.id.proposal_author)
        progressBar = findViewById(R.id.progress)
        vote_progress = findViewById(R.id.progressBar)

        proposal = intent.getSerializableExtra("proposal") as Proposal
           loadProposal()


           go_back.setOnClickListener {
            finish()
        }
    }

    fun loadContent() {
        var payload: String?
        val description = StringBuilder()
        var i = 0
        while (proposal!!.files != null && i < proposal!!.files!!.size) {
            if (proposal!!.files!![i].name == "index.md") {
                payload = proposal!!.files!![i].payload
                val data: ByteArray = Base64.decode(payload, Base64.DEFAULT)
                try {
                    description.append(String(data, Charset.forName("UTF-8")))
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                }
            }
            i++
        }
        tv_description.text = description.toString()
    }

    fun loadProposal() {
        tv_title.text = proposal!!.name
//        val meta: String = java.lang.String.format(Locale.getDefault(), "updated %s \nby %s \nversion %s - %d Comments",
//                Utils.calculateTime(System.currentTimeMillis() / 1000 - proposal.getTimestamp(), this), proposal.getUsername(), proposal.getVersion(), proposal.getNumcomments())
//        tv_meta.setText(meta, TextView.BufferType.SPANNABLE)
        if (proposal!!.files != null && proposal!!.files!!.size < 1) {
            progressBar.visibility = View.VISIBLE
            val config: HashMap<String, String> = HashMap()
            config["url"] = "https://proposals.decred.org/api/v1/proposals/" + proposal!!.censorshipRecord!!.token
            NetworkTask(config, object : AsyncResponse {
                override fun onResponse(@NotNull response: String) {
                    try {
                        val jsonObject = JSONObject(response)
                        val files: ArrayList<Proposal.File> = ArrayList()
                        val proposal_files = jsonObject.getJSONObject("proposal").getJSONArray("files")
                        for (i in 0 until proposal_files.length()) {
                            val file = Proposal().File()
                            file.name = proposal_files.getJSONObject(i).getString("name")
                            file.mime = proposal_files.getJSONObject(i).getString("mime")
                            file.digest = proposal_files.getJSONObject(i).getString("digest")
                            file.payload = proposal_files.getJSONObject(i).getString("payload")
                            files.add(file)
                        }
                        proposal!!.files
                        runOnUiThread {
                            progressBar.visibility = View.GONE
                            loadContent()
                        }
                    } catch (e: JSONException) {
                        runOnUiThread {
                            progressBar.visibility = View.GONE
                            Toast.makeText(this@ProposalDetailsActivity, e.localizedMessage, Toast.LENGTH_SHORT).show()
                        }
                        e.printStackTrace()
                    }
                }

                override fun onFailure(t: Throwable) {
                    runOnUiThread {
                        Toast.makeText(this@ProposalDetailsActivity, t.localizedMessage, Toast.LENGTH_SHORT).show()
                        progressBar.visibility = View.GONE
                    }
                    t.printStackTrace()
                }
            }).execute()
        } else {
            loadContent()
        }
        val config: HashMap<String, String> = HashMap()
        config["url"] = "https://proposals.decred.org/api/v1/proposals/" + proposal!!.censorshipRecord!!.token.toString() + "/votestatus"
        NetworkTask(config, object : AsyncResponse {
            override fun onResponse(@NotNull votesReponse: String) {
                try {
                    val vote = JSONObject(votesReponse)
                    val voteStatus = Proposal().VoteStatus()
                    voteStatus.status = vote.getInt("status")
                    voteStatus.totalvotes = vote.getInt("totalvotes")
                    if (!vote.isNull("optionsresult")) {
                        voteStatus.no = vote.getJSONArray("optionsresult").getJSONObject(0).getInt("votesreceived")
                        voteStatus.yes = vote.getJSONArray("optionsresult").getJSONObject(1).getInt("votesreceived")
                    }
                    voteStatus.totalvotes = vote.getInt("totalvotes")
                    proposal!!.voteStatus
                    runOnUiThread {
                        if (proposal!!.voteStatus != null && proposal!!.voteStatus!!.totalvotes !== 0) {
                            tv_progress.setVisibility(View.VISIBLE)
                            vote_progress.visibility = View.VISIBLE
                            val percentage = proposal!!.voteStatus!!.yes as Float / proposal!!.VoteStatus().totalvotes as Float * 100
                            tv_progress.setText(java.lang.String.format(Locale.getDefault(), "%.2f%%", percentage))
                            vote_progress.setProgress(percentage.toInt())
                        } else {
                            tv_progress.setVisibility(View.GONE)
                            vote_progress.visibility = View.GONE
                        }
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

            override fun onFailure(t: Throwable) {
                t.printStackTrace()
            }
        }).execute()
    }
}