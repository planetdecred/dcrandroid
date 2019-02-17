/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities;

import android.os.Bundle;
import android.widget.TextView;

import com.dcrandroid.R;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by Macsleven on 06/01/2018.
 */

public class PeersDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.peer_details));
        setContentView(R.layout.peers_details_view);
        TextView id = findViewById(R.id.peer_view_id);
        TextView addr = findViewById(R.id.peer_view_addr);
        TextView addrlocal = findViewById(R.id.peer_view_addrlocal);
        TextView services = findViewById(R.id.peer_view_services);
        TextView relaytxes = findViewById(R.id.peer_view_relaytxes);
        TextView lastsend = findViewById(R.id.peer_view_lastsend);
        TextView lastrecv = findViewById(R.id.peer_view_lastrecv);
        TextView bytessent = findViewById(R.id.peer_view_bytessent);
        TextView bytesrecv = findViewById(R.id.peer_view_bytesrecv);
        TextView conntime = findViewById(R.id.peer_view_conntime);
        TextView timeoffset = findViewById(R.id.peer_view_timeoffset);
        TextView pingtime = findViewById(R.id.peer_view_pingtime);
        TextView version = findViewById(R.id.peer_view_version);
        TextView subver = findViewById(R.id.peer_view_subver);
        TextView inbound = findViewById(R.id.peer_view_inbound);
        TextView startingheight = findViewById(R.id.peer_view_startingheight);
        TextView currentheight = findViewById(R.id.peer_view_currentheight);
        TextView banscore = findViewById(R.id.peer_view_banscore);
        TextView syncnode = findViewById(R.id.peer_view_syncnode);

        id.setText(getIntent().getStringExtra("id"));
        addr.setText(getIntent().getStringExtra("addr"));
        addrlocal.setText(getIntent().getStringExtra("addrlocal"));
        services.setText(getIntent().getStringExtra("services"));
        relaytxes.setText(getIntent().getStringExtra("relaytxes"));
        lastsend.setText(getIntent().getStringExtra("lastsend"));
        lastrecv.setText(getIntent().getStringExtra("lastrecv"));
        bytessent.setText(getIntent().getStringExtra("bytessent"));
        bytesrecv.setText(getIntent().getStringExtra("bytesrecv"));
        conntime.setText(getIntent().getStringExtra("conntime"));
        timeoffset.setText(getIntent().getStringExtra("timeoffset"));
        pingtime.setText(getIntent().getStringExtra("pingtime"));
        version.setText(getIntent().getStringExtra("version"));
        subver.setText(getIntent().getStringExtra("subver"));
        inbound.setText(getIntent().getStringExtra("inbound"));
        startingheight.setText(getIntent().getStringExtra("startingheight"));
        currentheight.setText(getIntent().getStringExtra("currentheight"));
        banscore.setText(getIntent().getStringExtra("banscore"));
        syncnode.setText(getIntent().getStringExtra("syncnode"));

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
