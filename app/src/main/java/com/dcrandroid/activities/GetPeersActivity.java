/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.dcrandroid.R;
import com.dcrandroid.adapter.PeerAdapter;
import com.dcrandroid.data.Peers;
import com.dcrandroid.util.RecyclerTouchListener;
import com.dcrandroid.util.Utils;
import com.dcrandroid.util.WalletData;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


/**
 * Created by Macsleven on 05/01/2018.
 */

public class GetPeersActivity extends AppCompatActivity {

    private List<Peers> peerList = new ArrayList<>();
    private PeerAdapter peerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS |
                    View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        setContentView(R.layout.activity_get_peers_page);
        setTitle(getString(R.string.peers));
        RecyclerView recyclerView = findViewById(R.id.get_peers_recycler_view);
        peerAdapter = new PeerAdapter(peerList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Peers peers = peerList.get(position);
                Intent intent = new Intent(GetPeersActivity.this, PeersDetailsActivity.class);
                intent.putExtra("id", peers.getId());
                intent.putExtra("addr", peers.getAddr());
                intent.putExtra("addrlocal", peers.getAddrlocal());
                intent.putExtra("services", peers.getServices());
                intent.putExtra("relaytxes", peers.getRelaytxes());
                intent.putExtra("lastsend", peers.getLastsend());
                intent.putExtra("lastrecv", peers.getLastrecv());
                intent.putExtra("bytessent", peers.getBytessent());
                intent.putExtra("bytesrecv", peers.getBytesrecv());
                intent.putExtra("conntime", peers.getConntime());
                intent.putExtra("timeoffset", peers.getTimeoffset());
                intent.putExtra("pingtime", peers.getPingtime());
                intent.putExtra("version", peers.getVersion());
                intent.putExtra("subver", peers.getSubver());
                intent.putExtra("inbound", peers.getInbound());
                intent.putExtra("startingheight", peers.getStartingheight());
                intent.putExtra("currentheight", peers.getCurrentheight());
                intent.putExtra("banscore", peers.getBanscore());
                intent.putExtra("syncnode", peers.getSyncode());
                startActivity(intent);
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));
        recyclerView.setAdapter(peerAdapter);
        prepareConnectionData();
    }

    private void prepareConnectionData() {
        final ProgressDialog pd = Utils.getProgressDialog(this, false, false, "Getting Peers...");
        pd.show();
        new Thread() {
            public void run() {
                try {
                    String dcrdAddress = Utils.getNetworkAddress(GetPeersActivity.this);
                    String result = WalletData.getInstance().wallet.callJSONRPC("getpeerinfo", "", dcrdAddress, "dcrwallet", "dcrwallet", Utils.getRemoteCertificate(GetPeersActivity.this));
                    System.out.println("Peers: " + result);
                    JSONArray array = new JSONArray(result);
                    if (array.length() == 0) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(GetPeersActivity.this, R.string.no_peer_found, Toast.LENGTH_SHORT).show();
                                if (pd.isShowing()) {
                                    pd.dismiss();
                                }
                            }
                        });
                        return;
                    }
                    peerList.clear();
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        Peers peer = new Peers();
                        peer.setId(String.valueOf(obj.getInt("id")));
                        peer.setAddr(obj.getString("addr"));
                        if (obj.has("addrlocal")) {
                            peer.setAddrlocal(obj.getString("addrlocal"));
                        }
                        peer.setServices(obj.getString("services"));
                        if (obj.has("relaytxes")) {
                            peer.setRelaytxes(String.valueOf(obj.getBoolean("relaytxes")));
                        }
                        peer.setLastsend(String.valueOf(obj.getInt("lastsend")));
                        peer.setLastrecv(String.valueOf(obj.getInt("lastrecv")));
                        peer.setBytessent(String.valueOf(obj.getInt("bytessent")));
                        peer.setBytesrecv(String.valueOf(obj.getInt("bytesrecv")));
                        peer.setConntime(String.valueOf(obj.getInt("conntime")));
                        peer.setTimeoffset(String.valueOf(obj.getInt("timeoffset")));
                        peer.setPingtime(String.valueOf(obj.getInt("pingtime")));
                        peer.setVersion(String.valueOf(obj.getInt("version")));
                        peer.setSubver(obj.getString("subver"));
                        peer.setInbound(String.valueOf(obj.getBoolean("inbound")));
                        peer.setStartingheight(String.valueOf(obj.getInt("startingheight")));
                        peer.setCurrentheight(String.valueOf(obj.getInt("currentheight")));
                        peer.setBanscore(String.valueOf(obj.getInt("banscore")));
                        peer.setSyncode(String.valueOf(obj.getBoolean("syncnode")));
                        peerList.add(peer);
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            peerAdapter.notifyDataSetChanged();
                            if (pd.isShowing()) {
                                pd.dismiss();
                            }
                        }
                    });
                } catch (final Exception e) {
                    e.printStackTrace();
                    if (pd.isShowing()) {
                        pd.dismiss();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(GetPeersActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }.start();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
