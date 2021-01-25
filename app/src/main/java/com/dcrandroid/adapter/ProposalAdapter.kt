package com.dcrandroid.adapter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import com.dcrandroid.activities.ProposalDetailsActivity
import com.dcrandroid.data.Constants
import com.dcrandroid.data.Proposal
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.show
import com.dcrandroid.util.Utils
import kotlinx.android.synthetic.main.proposal_list_row.view.*
import java.util.*

class ProposalAdapter(private val proposals: List<Proposal>, private val context: Context) : RecyclerView.Adapter<ProposalAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.proposal_list_row, parent, false)
        return MyViewHolder(itemView)
    }

    inner class MyViewHolder(var view: View) : RecyclerView.ViewHolder(view) {
        var title = view.proposal_title
        var status = view.proposal_status
        var author = view.proposal_author
        var timestamp = view.proposal_timestamp
        var comments = view.proposal_comments
        var version = view.proposal_version

        var progrssBarContainer = view.progress_bar_container
        var progressBar = view.progressBar
        var progress = view.progress
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val proposal = proposals[position]

        holder.title.text = proposal.name
        holder.author.text = proposal.username
        holder.timestamp.text = Utils.calculateTime((System.currentTimeMillis() / 1000) - proposal.publishedAt, context)
        holder.comments.text = String.format(Locale.getDefault(), context.getString(R.string.comments), proposal.numcomments)
        holder.version.text = String.format(Locale.getDefault(), context.getString(R.string.version_number), proposal.version)

        // Set proposal vote status
        if (proposal.status == 6) {
            holder.status.background = getDrawable(context, R.drawable.bg_light_orange_corners_4dp)
            holder.status.text = context.getString(R.string.status_abandoned)
        } else {
            if (proposal.voteStatus == 0) {
                holder.status.text = context.getString(R.string.status_invalid)
            } else if (proposal.voteStatus == 1) {
                holder.status.background = getDrawable(context, R.drawable.orange_bg_corners_4dp)
                holder.status.text = context.getString(R.string.status_not_authorized)
            } else if (proposal.voteStatus == 2) {
                holder.status.background = getDrawable(context, R.drawable.default_app_button_bg)
                holder.status.text = context.getString(R.string.status_authorized)
            } else if (proposal.voteStatus == 3) {
                holder.status.background = getDrawable(context, R.drawable.default_app_button_bg)
                holder.status.text = context.getString(R.string.status_vote_started)
            } else if (proposal.voteStatus == 4) {
                if (proposal.approved) {
                    holder.status.background = getDrawable(context, R.drawable.bg_dark_green_corners_4dp)
                    holder.status.text = context.getString(R.string.status_approved)
                } else {
                    holder.status.background = getDrawable(context, R.drawable.orange_bg_corners_4dp)
                    holder.status.text = context.getString(R.string.status_rejected)
                }
            } else if (proposal.voteStatus == 5) {
                holder.status.background = getDrawable(context, R.drawable.orange_bg_corners_4dp)
                holder.status.text = context.getString(R.string.status_non_existent)
            }
        }

        if (proposal.voteStatus == 4) {

            holder.progrssBarContainer.show()

            holder.progressBar.max = proposal.totalVotes
            holder.progressBar.progress = proposal.yesVotes
            holder.progressBar.secondaryProgress = proposal.totalVotes

            holder.progress.text = context.getString(R.string.yes_no_votes_percent, proposal.yesVotes, proposal.yesPercentage,
                    proposal.noVotes, proposal.noPercentage)
        } else {
            holder.progrssBarContainer.hide()
        }

        holder.view.setOnClickListener {
            val intent = Intent(context, ProposalDetailsActivity::class.java)
            val b = Bundle()
            b.putSerializable(Constants.PROPOSAL_ID, proposal.id)
            intent.putExtras(b)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return proposals.size
    }
}