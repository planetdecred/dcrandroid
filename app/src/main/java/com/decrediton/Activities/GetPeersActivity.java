package com.decrediton.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.decrediton.Adapter.PeerAdapter;
import com.decrediton.R;
import com.decrediton.Util.RecyclerTouchListener;
import com.decrediton.data.Peers;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Macsleven on 05/01/2018.
 */

public class GetPeersActivity extends AppCompatActivity{

    private List<Peers> peerList = new ArrayList<>();
    private PeerAdapter peerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_peers_page);
        setTitle("Peers");
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.get_peers_recycler_view);
        peerAdapter = new PeerAdapter(peerList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Peers peers = peerList.get(position);
                Intent intent = new Intent(GetPeersActivity.this,PeersDetailsActivity.class);
                intent.putExtra("id",peers.getId());
                intent.putExtra("addr",peers.getAddr());
                intent.putExtra("addrlocal",peers.getAddrlocal());
                intent.putExtra("services",peers.getServices());
                intent.putExtra("relaytxes",peers.getRelaytxes());
                intent.putExtra("lastsend",peers.getLastsend());
                intent.putExtra("lastrecv",peers.getLastrecv());
                intent.putExtra("bytessent",peers.getBytessent());
                intent.putExtra("bytesrecv",peers.getBytesrecv());
                intent.putExtra("conntime",peers.getConntime());
                intent.putExtra("timeoffset",peers.getTimeoffset());
                intent.putExtra("pingtime",peers.getPingtime());
                intent.putExtra("version",peers.getVersion());
                intent.putExtra("subver",peers.getSubver());
                intent.putExtra("inbound",peers.getInbound());
                intent.putExtra("startingheight",peers.getStartingheight());
                intent.putExtra("currentheight",peers.getCurrentheight());
                intent.putExtra("banscore",peers.getBanscore());
                intent.putExtra("syncnode",peers.getSyncode());
                startActivity(intent);
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));
        recyclerView.setAdapter(peerAdapter);
        prepareConnectionData();
    }

    public void prepareConnectionData(){
        Peers peers= new Peers("1","176.9.89.217:19108","192.108","","","","","","",
                "","","","","","","","","",
                "");
        peerList.add(peers);
        peers= new Peers("2","176.9.89.217:19408","192.108","","","","","","",
                "","","","","","","","","",
                "");
        peerList.add(peers);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public interface ClickListener {
        void onClick(View view, int position);
        void onLongClick(View view, int position);
    }
}
