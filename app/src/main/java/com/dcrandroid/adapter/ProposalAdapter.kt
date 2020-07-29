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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import com.dcrandroid.activities.ProposalDetailsActivity
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
        holder.comments.text = String.format(Locale.getDefault(), "%d Comments", proposal.getNumcomments())
        holder.version.text = String.format(Locale.getDefault(), "version %s", proposal.version)

        if (proposal.status == 1) {
            holder.status.text = "Not found"
        }
        else if (proposal.status == 2) {
            holder.status.text = "Not Reviewed"
        }
        else if (proposal.status == 3) {
            holder.status.background = getDrawable(context, R.drawable.default_app_button_bg)
            holder.status.text = "Censored"
        }
        else if (proposal.status == 4) {
            holder.status.background = getDrawable(context, R.drawable.bg_light_green_corners_4dp)
            holder.status.text = "Published"
        }
        else if (proposal.status == 5) {
            holder.status.background = getDrawable(context, R.drawable.bg_transparent_light_grey_corners_4dp)
            holder.status.resources.getColor(R.color.blue)
            holder.status.text = "edited by author"
        }
        else {
            holder.status.background = getDrawable(context, R.drawable.bg_light_orange_corners_4dp)
            holder.status.text = "Abandoned"
        }

        if (proposal.voteStatus != null && proposal.voteStatus!!.totalvotes != 0) {
            holder.progress.visibility = View.VISIBLE
            holder.progressBar.visibility = View.VISIBLE
            val percentage = (proposal.voteStatus!!.yes.toFloat() / proposal.voteStatus!!.totalvotes.toFloat()) * 100
            holder.progress.text = "%.2f%%".format(percentage)
            holder.progressBar.progress = percentage.toInt()
        } else {
            holder.progress.visibility = View.GONE
            holder.progressBar.visibility = View.GONE
        }

        holder.view.setOnClickListener {
            val intent = Intent(context, ProposalDetailsActivity::class.java)
            val b = Bundle()
            b.putSerializable("proposal", proposal)
            intent.putExtras(b)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return proposals.size
    }
}