package com.dcrandroid.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.widget.NestedScrollView
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.data.Proposal
import com.dcrandroid.util.Deserializer
import com.dcrandroid.util.Utils
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_proposal_details.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.util.*
import kotlin.collections.ArrayList

class ProposalDetailsActivity : BaseActivity() {

    private val gson = GsonBuilder().registerTypeHierarchyAdapter(ArrayList::class.java, Deserializer.ProposalDeserializer()).create()
    private var proposal: Proposal? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_proposal_details)

        proposal = intent.getSerializableExtra(Constants.PROPOSAL) as Proposal

        loadProposalDetails()

        open_proposal.setOnClickListener {
            val url = getString(R.string.politeia_server_url) + proposal!!.censorshipRecord!!.token
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }

        share_proposal.setOnClickListener {
            val share = Intent(Intent.ACTION_SEND)
            share.type = getString(R.string.text_pain)
            share.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
            share.putExtra(Intent.EXTRA_SUBJECT, proposal!!.name)
            share.putExtra(Intent.EXTRA_TEXT, getString(R.string.politeia_server_url) + proposal!!.censorshipRecord!!.token)
            startActivity(Intent.createChooser(share, getString(R.string.share_proposal)))
        }

        nested_scroll_view.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (scrollY > oldScrollY) {
                app_bar.elevation = resources.getDimension(R.dimen.app_bar_elevation)
            }
            if (scrollY == 0) {
                app_bar.elevation = 0f
            }
        })

        go_back.setOnClickListener {
            finish()
        }
    }

    private fun loadProposalDetails() = GlobalScope.launch(Dispatchers.Main) {
        val proposalResult = multiWallet!!.politeia!!.getProposalByID(proposal!!.id)
        val proposalObjectJson = JSONObject(proposalResult).getJSONObject(Constants.RESULT)
        val proposalResultString: String = proposalObjectJson.toString()

        val voteObjectJson = proposalObjectJson.getJSONObject(Constants.VOTE_SUMMARY)
        val voteSummaryString: String = voteObjectJson.toString()

        val proposalItem = gson.fromJson(proposalResultString, Proposal::class.java)
        val voteSummaryItem = gson.fromJson(voteSummaryString, Proposal.VoteSummary::class.java)

        proposal_title.text = proposalItem.name
            proposal_author.text = proposal!!.username
            proposal_timestamp.text = Utils.calculateTime(System.currentTimeMillis() / 1000 - proposal!!.timestamp, this@ProposalDetailsActivity)
            proposal_comments.text = String.format(Locale.getDefault(), getString(R.string.comments), proposal!!.getNumcomments())
            proposal_version.text = String.format(Locale.getDefault(), getString(R.string.version_number), proposal!!.version)

            // Get and read file from array.
            if (proposal!!.files != null && proposal!!.files!!.isNotEmpty()) {
                progressBar.visibility = View.VISIBLE

                var payload: String?
                val description = StringBuilder()
                var i = 0
                while (proposalItem.files != null && i < proposalItem.files!!.size) {
                    if (proposalItem.files!![i].name == Constants.INDEX_MD) {
                        payload = proposalItem.files!![i].payload
                        val data: ByteArray = Base64.decode(payload, Base64.DEFAULT)
                        try {
                            description.append(String(data, Charset.forName(Constants.CHARSET_UTF_8)))
                        } catch (e: UnsupportedEncodingException) {
                            e.printStackTrace()
                        }
                    }
                    i++
                }

                proposal_description.text = description.toString()

                progress.visibility = View.GONE
            }

        // Get vote status.
        if (voteSummaryItem.status == 4) {
            val totalVotes = voteSummaryItem.optionsResults!![0].votesreceived + voteSummaryItem.optionsResults!![1].votesreceived
            vote_progress.visibility = View.VISIBLE
            progressBar.visibility = View.VISIBLE
            yes_votes.text = "Yes: " + voteSummaryItem.optionsResults!![1].votesreceived + " (" + (voteSummaryItem.optionsResults!![1].votesreceived.toFloat() / totalVotes.toFloat()) * 100 + "%)"
            no_votes.text = "No: " + voteSummaryItem.optionsResults!![0].votesreceived + " (" + (voteSummaryItem.optionsResults!![0].votesreceived.toFloat() / totalVotes.toFloat()) * 100 + "%)"
            val percentage = (voteSummaryItem.optionsResults!![1].votesreceived.toFloat() / totalVotes.toFloat()) * 100
            vote_progress.text = getString(R.string.proposal_percentage).format(percentage)
            progressBar.progress = percentage.toInt()
        } else {
            vote_progress.visibility = View.GONE
            progressBar.visibility = View.GONE
        }

        // Set proposal status.
        if (proposal!!.status == 6) {
            proposal_status.visibility = View.VISIBLE
            proposal_status.background = AppCompatResources.getDrawable(this@ProposalDetailsActivity, R.drawable.bg_light_orange_corners_4dp)
            proposal_status.text = getString(R.string.status_abandoned)
        } else {
            proposal_status.visibility = View.VISIBLE
            if (proposal!!.voteSummary!!.status == 0) {
                proposal_status.text = getString(R.string.status_invalid)
            } else if (proposal!!.voteSummary!!.status == 1) {
                proposal_status.background = AppCompatResources.getDrawable(this@ProposalDetailsActivity, R.drawable.orange_bg_corners_4dp)
                proposal_status.text = getString(R.string.status_not_authorized)
            } else if (proposal!!.voteSummary!!.status == 2) {
                proposal_status.background = AppCompatResources.getDrawable(this@ProposalDetailsActivity, R.drawable.default_app_button_bg)
                proposal_status.text = getString(R.string.status_authorized)
            } else if (proposal!!.voteSummary!!.status == 3) {
                proposal_status.background = AppCompatResources.getDrawable(this@ProposalDetailsActivity, R.drawable.default_app_button_bg)
                proposal_status.text = getString(R.string.status_vote_started)
            } else if (proposal!!.voteSummary!!.status == 4) {
                val totalVotes: Int = proposal!!.voteSummary!!.optionsResults!![0].votesreceived + proposal!!.voteSummary!!.optionsResults!![1].votesreceived
                val yesPercentage = (proposal!!.voteSummary!!.optionsResults!![1].votesreceived.toFloat() / totalVotes.toFloat()) * 100
                val passPercentage = proposal!!.voteSummary!!.passpercentage

                if (yesPercentage >= passPercentage) {
                    proposal_status.background = AppCompatResources.getDrawable(this@ProposalDetailsActivity, R.drawable.bg_dark_green_corners_4dp)
                    proposal_status.text = getString(R.string.status_approved)
                } else {
                    proposal_status.background = AppCompatResources.getDrawable(this@ProposalDetailsActivity, R.drawable.orange_bg_corners_4dp)
                    proposal_status.text = getString(R.string.status_rejected)
                }
            } else if (proposal!!.voteSummary!!.status == 5) {
                proposal_status.background = AppCompatResources.getDrawable(this@ProposalDetailsActivity, R.drawable.orange_bg_corners_4dp)
                proposal_status.text = getString(R.string.status_non_existent)
            }
        }
    }
}