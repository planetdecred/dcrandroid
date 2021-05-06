/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dcrandroid.R
import com.dcrandroid.data.PeerInfo
import com.dcrandroid.extensions.hide
import com.dcrandroid.extensions.show
import kotlinx.android.synthetic.main.connected_peers_list_row.view.*

class PeerInfoAdapter(val peerInfos: ArrayList<PeerInfo>) : RecyclerView.Adapter<PeerInfoAdapter.ViewHolder>() {

    private var expandedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)

        return ViewHolder(layoutInflater.inflate(R.layout.connected_peers_list_row, parent, false))
    }

    override fun getItemCount(): Int = peerInfos.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val peerInfo = peerInfos[position]

        holder.itemView.tv_id.text = peerInfo.id.toString()
        holder.itemView.tv_addr.text = peerInfo.addr

        holder.itemView.tv_peer_info.text = holder.itemView.context.getString(R.string.connected_peers_details, peerInfo.id,
                peerInfo.addr, peerInfo.addrLocal, peerInfo.services, peerInfo.version, peerInfo.subVer, peerInfo.startingHeight, peerInfo.banScore)

        if (expandedPosition == position) {
            holder.itemView.expand_row.setImageResource(R.drawable.ic_collapse)
            holder.itemView.peer_info_details.show()
        } else {
            holder.itemView.expand_row.setImageResource(R.drawable.ic_expand)
            holder.itemView.peer_info_details.hide()
        }

        holder.itemView.container.setOnClickListener {
            if (expandedPosition == position) {
                expandedPosition = -1
                notifyItemChanged(position)
            } else {
                val currentlyExpanded = expandedPosition
                expandedPosition = position

                notifyItemChanged(currentlyExpanded)
                notifyItemChanged(expandedPosition)
            }
        }
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v)
}