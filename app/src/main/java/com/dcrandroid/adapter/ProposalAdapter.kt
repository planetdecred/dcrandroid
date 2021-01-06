package com.dcrandroid.adapter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import com.dcrandroid.activities.ProposalDetailsActivity
import com.dcrandroid.data.Constants
import com.dcrandroid.data.Proposal
import com.dcrandroid.util.Utils
import java.util.*

class ProposalAdapter(private val proposals: List<Proposal>, private val context: Context) : RecyclerView.Adapter<ProposalAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.proposal_list_row, parent, false)
        return MyViewHolder(itemView)
    }

    inner class MyViewHolder internal constructor(var view: View) : RecyclerView.ViewHolder(view) {
        var title: TextView = view.findViewById(R.id.proposal_title)
        var status: TextView = view.findViewById(R.id.proposal_status)
        var author: TextView = view.findViewById(R.id.proposal_author)
        var timestamp: TextView = view.findViewById(R.id.proposal_timestamp)
        var comments: TextView = view.findViewById(R.id.proposal_comments)
        var version: TextView = view.findViewById(R.id.proposal_version)
        var progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        var progress: TextView = view.findViewById(R.id.progress)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val proposal = proposals[position]
        holder.title.text = proposal.name
        holder.author.text = proposal.username
        holder.timestamp.text = Utils.calculateTime(System.currentTimeMillis() / 1000 - proposal.timestamp, context)
        holder.comments.text = String.format(Locale.getDefault(), "%d Comments", proposal.numcomments)
        holder.version.text = String.format(Locale.getDefault(), "version %s", proposal.version)

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
                val totalVotes = (proposal.yesVotes + proposal.noVotes).toFloat()
                val yesPercentage = (proposal.yesVotes / totalVotes) * 100

                if (yesPercentage >= proposal.passPercentage) {
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
            val totalVotes = (proposal.yesVotes + proposal.noVotes).toFloat()
            holder.progress.visibility = View.VISIBLE
            holder.progressBar.visibility = View.VISIBLE
            val percentage = (proposal.yesVotes / totalVotes) * 100
            holder.progress.text = String.format(Locale.getDefault(), "%.2f%%", percentage)
            holder.progressBar.progress = percentage.toInt()
        } else {
            holder.progress.visibility = View.GONE
            holder.progressBar.visibility = View.GONE
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