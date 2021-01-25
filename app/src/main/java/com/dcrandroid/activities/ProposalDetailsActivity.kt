package com.dcrandroid.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.widget.NestedScrollView
import com.dcrandroid.R
import com.dcrandroid.data.Constants
import com.dcrandroid.data.Proposal
import com.dcrandroid.extensions.show
import com.dcrandroid.util.Utils
import dcrlibwallet.Dcrlibwallet
import kotlinx.android.synthetic.main.activity_proposal_details.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.nio.charset.Charset
import java.util.*

class ProposalDetailsActivity : BaseActivity() {

    private lateinit var proposal: Proposal

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_proposal_details)

        val proposalId = intent.getSerializableExtra(Constants.PROPOSAL_ID) as Long
        proposal = Proposal.from(multiWallet!!.politeia.getProposalByIDRaw(proposalId))
        loadProposalDetails()

        open_proposal.setOnClickListener {
            val url = getString(R.string.politeia_server_url) + proposal.token
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }

        share_proposal.setOnClickListener {
            val share = Intent(Intent.ACTION_SEND)
            share.type = getString(R.string.text_pain)
            share.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
            share.putExtra(Intent.EXTRA_SUBJECT, proposal.name)
            share.putExtra(Intent.EXTRA_TEXT, getString(R.string.politeia_server_url) + proposal.token)
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

        proposal_title.text = proposal.name
        proposal_author.text = proposal.username
        proposal_timestamp.text = Utils.calculateTime(System.currentTimeMillis() / 1000 - proposal.publishedAt, this@ProposalDetailsActivity)
        proposal_comments.text = String.format(Locale.getDefault(), getString(R.string.comments), proposal.numcomments)
        proposal_version.text = String.format(Locale.getDefault(), getString(R.string.version_number), proposal.version)

        if (proposal.indexFile!!.isNotEmpty()) {
            val data = Dcrlibwallet.decodeBase64(proposal.indexFile!!)
            try {
                proposal_description.text = String(data, Charset.forName(Constants.CHARSET_UTF_8))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Get vote status.
        if (proposal.voteStatus == 4) {
            vote_summary.show()

            yes_votes.text = getString(R.string.yes_votes_percent, proposal.yesVotes, proposal.yesPercentage)
            no_votes.text = getString(R.string.no_votes_percent, proposal.noVotes, proposal.noPercentage)

            progressBar.max = proposal.totalVotes
            progressBar.progress = proposal.yesVotes
            progressBar.secondaryProgress = proposal.totalVotes
        }

        // Set proposal status.
        if (proposal.status == 6) {
            proposal_status.visibility = View.VISIBLE
            proposal_status.background = AppCompatResources.getDrawable(this@ProposalDetailsActivity, R.drawable.bg_light_orange_corners_4dp)
            proposal_status.text = getString(R.string.status_abandoned)
        } else {
            proposal_status.visibility = View.VISIBLE
            if (proposal.voteStatus == 0) {
                proposal_status.text = getString(R.string.status_invalid)
            } else if (proposal.voteStatus == 1) {
                proposal_status.background = AppCompatResources.getDrawable(this@ProposalDetailsActivity, R.drawable.orange_bg_corners_4dp)
                proposal_status.text = getString(R.string.status_not_authorized)
            } else if (proposal.voteStatus == 2) {
                proposal_status.background = AppCompatResources.getDrawable(this@ProposalDetailsActivity, R.drawable.default_app_button_bg)
                proposal_status.text = getString(R.string.status_authorized)
            } else if (proposal.voteStatus == 3) {
                proposal_status.background = AppCompatResources.getDrawable(this@ProposalDetailsActivity, R.drawable.default_app_button_bg)
                proposal_status.text = getString(R.string.status_vote_started)
            } else if (proposal.voteStatus == 4) {
                if (proposal.approved) {
                    proposal_status.background = AppCompatResources.getDrawable(this@ProposalDetailsActivity, R.drawable.bg_dark_green_corners_4dp)
                    proposal_status.text = getString(R.string.status_approved)
                } else {
                    proposal_status.background = AppCompatResources.getDrawable(this@ProposalDetailsActivity, R.drawable.orange_bg_corners_4dp)
                    proposal_status.text = getString(R.string.status_rejected)
                }
            } else if (proposal.voteStatus == 5) {
                proposal_status.background = AppCompatResources.getDrawable(this@ProposalDetailsActivity, R.drawable.orange_bg_corners_4dp)
                proposal_status.text = getString(R.string.status_non_existent)
            }
        }
    }
}