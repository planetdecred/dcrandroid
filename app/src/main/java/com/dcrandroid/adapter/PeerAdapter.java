/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dcrandroid.R;
import com.dcrandroid.data.Peers;

import java.util.List;

import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by Macsleven on 05/01/2018.
 */

public class PeerAdapter extends RecyclerView.Adapter<PeerAdapter.MyViewHolder> {
    private List<Peers> peersList;

    public PeerAdapter(List<Peers> peerListList) {
        this.peersList = peerListList;

    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.get_peers_list_row, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {

        Peers peer = peersList.get(position);
        holder.peerId.setText(peer.getId());
        holder.peerAddr.setText(peer.getAddr());
    }

    @Override
    public int getItemCount() {
        return peersList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView peerId;
        public TextView peerAddr;


        public MyViewHolder(View view) {
            super(view);
            peerId = view.findViewById(R.id.get_peers_id);
            peerAddr = view.findViewById(R.id.get_peers_addr);
        }
    }
}
