package com.dcrandroid.activities

import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import com.dcrandroid.R
import com.dcrandroid.data.Proposal
import com.dcrandroid.util.Deserializer
import com.dcrandroid.util.Utils
import com.google.gson.GsonBuilder
import dcrlibwallet.Politeia
import kotlinx.android.synthetic.main.activity_proposal_details.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.util.*
import kotlin.collections.ArrayList

class ProposalDetailsActivity : BaseActivity() {

    private lateinit var proposalTitle: TextView
    private lateinit var proposalDescription: TextView
    private lateinit var votePercent: TextView
    private lateinit var author: TextView
    private lateinit var timestamp: TextView
    private lateinit var comments: TextView
    private lateinit var version: TextView
    private lateinit var status: TextView
    private lateinit var yes: TextView
    private lateinit var no: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var voteProgress: ProgressBar
    private var politeia: Politeia? = Politeia()
    private val gson = GsonBuilder().registerTypeHierarchyAdapter(ArrayList::class.java, Deserializer.ProposalDeserializer()).create()
    private var proposal: Proposal? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_proposal_details)

        proposalTitle = findViewById(R.id.proposal_title)
        proposalDescription = findViewById(R.id.proposal_description)
        votePercent = findViewById(R.id.vote_progress)
        author = findViewById(R.id.proposal_author)
        timestamp = findViewById(R.id.proposal_timestamp)
        comments = findViewById(R.id.proposal_comments)
        version = findViewById(R.id.proposal_version)
        progressBar = findViewById(R.id.progress)
        voteProgress = findViewById(R.id.progressBar)
        status = findViewById(R.id.proposal_status)
        yes = findViewById(R.id.yes_votes)
        no = findViewById(R.id.no_votes)

        proposal = intent.getSerializableExtra("proposal") as Proposal

        loadProposalDetails()

        go_back.setOnClickListener {
            finish()
        }
    }

    private fun loadProposalDetails() = GlobalScope.launch(Dispatchers.Default) {
        val proposalResult = politeia!!.getProposalDetails(proposal!!.censorshipRecord!!.token, "")
        val voteStatusResult = politeia!!.getVoteStatus(proposal!!.censorshipRecord!!.token)

        runOnUiThread {
            val proposalItem = gson.fromJson(proposalResult, Proposal::class.java)
            val voteItem = gson.fromJson(voteStatusResult, Proposal.VoteStatus::class.java)

            proposalTitle.text = proposalItem.name
            author.text = proposal!!.username
            timestamp.text = Utils.calculateTime(System.currentTimeMillis() / 1000 - proposal!!.timestamp, this@ProposalDetailsActivity)
            comments.text = String.format(Locale.getDefault(), "%d Comments", proposal!!.getNumcomments())
            version.text = String.format(Locale.getDefault(), "version %s", proposal!!.version)

            // Get and read file from array.
            if (proposal!!.files != null && proposal!!.files!!.isEmpty()) {
                progressBar.visibility = View.VISIBLE

                var payload: String?
                val description = StringBuilder()
                var i = 0
                while (proposalItem.files != null && i < proposalItem.files!!.size) {
                    if (proposalItem.files!![i].name == "index.md") {
                        payload = proposalItem.files!![i].payload
                        val data: ByteArray = Base64.decode(payload, Base64.DEFAULT)
                        try {
                            description.append(String(data, Charset.forName("UTF-8")))
                        } catch (e: UnsupportedEncodingException) {
                            e.printStackTrace()
                        }
                    }
                    i++
                }
                proposalDescription.text = description.toString()
                progressBar.visibility = View.GONE
            }

            // Get vote status.
            if (voteItem != null && voteItem.totalvotes != 0) {
                votePercent.visibility = View.VISIBLE
                voteProgress.visibility = View.VISIBLE
                yes.text = "Yes: " + voteItem.optionsResults!![1].votesreceived + " (" + (voteItem.optionsResults!![1].votesreceived.toFloat() / voteItem.totalvotes.toFloat()) * 100 + "%)"
                no.text = "No: " + voteItem.optionsResults!![0].votesreceived + " (" + (voteItem.optionsResults!![0].votesreceived.toFloat() / voteItem.totalvotes.toFloat()) * 100 + "%)"
                val percentage = (voteItem.optionsResults!![1].votesreceived.toFloat() / voteItem.totalvotes.toFloat()) * 100
                votePercent.text = "%.2f%%".format(percentage)
                voteProgress.progress = percentage.toInt()
            } else {
                votePercent.visibility = View.GONE
                voteProgress.visibility = View.GONE
            }

            // Set proposal status.
            if (proposal!!.status == 6) {
                status.visibility = View.VISIBLE
                status.background = AppCompatResources.getDrawable(this@ProposalDetailsActivity, R.drawable.bg_light_orange_corners_4dp)
                status.text = getString(R.string.status_abandoned)
            } else {
                status.visibility = View.VISIBLE
                if (proposal!!.voteStatus!!.status == 0) {
                    status.text = getString(R.string.status_invalid)
                } else if (proposal!!.voteStatus!!.status == 1) {
                    status.background = AppCompatResources.getDrawable(this@ProposalDetailsActivity, R.drawable.orange_bg_corners_4dp)
                    status.text = getString(R.string.status_not_authorized)
                } else if (proposal!!.voteStatus!!.status == 2) {
                    status.background = AppCompatResources.getDrawable(this@ProposalDetailsActivity, R.drawable.default_app_button_bg)
                    status.text = getString(R.string.status_authorized)
                } else if (proposal!!.voteStatus!!.status == 3) {
                    status.background = AppCompatResources.getDrawable(this@ProposalDetailsActivity, R.drawable.default_app_button_bg)
                    status.text = getString(R.string.status_vote_started)
                } else if (proposal!!.voteStatus!!.status == 4) {
                    val yesPercentage = (proposal!!.voteStatus!!.optionsResults!![1].votesreceived.toFloat() / proposal!!.voteStatus!!.totalvotes.toFloat()) * 100
                    val passPercentage = proposal!!.voteStatus!!.passpercentage

                    if (yesPercentage >= passPercentage) {
                        status.background = AppCompatResources.getDrawable(this@ProposalDetailsActivity, R.drawable.bg_light_green_corners_4dp)
                        status.text = getString(R.string.status_approved)
                    } else {
                        status.background = AppCompatResources.getDrawable(this@ProposalDetailsActivity, R.drawable.orange_bg_corners_4dp)
                        status.text = getString(R.string.status_rejected)
                    }
                } else if (proposal!!.voteStatus!!.status == 5) {
                    status.background = AppCompatResources.getDrawable(this@ProposalDetailsActivity, R.drawable.orange_bg_corners_4dp)
                    status.text = getString(R.string.status_non_existent)
                }
            }
        }

    }
}