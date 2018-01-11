package com.decrediton.Activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import com.decrediton.Adapter.PeerAdapter;
import com.decrediton.R;
import com.decrediton.Util.RecyclerTouchListener;
import com.decrediton.Util.Utils;
import com.decrediton.data.Peers;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import dcrwallet.Dcrwallet;

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
        final ProgressDialog pd  = Utils.getProgressDialog(this,false,false,"Getting Peers...");
        pd.show();
        new Thread(){
            public void run(){
                try{
                    String result = Dcrwallet.runUtil();
                    JSONArray array = new JSONArray(result);
                    if(array.length() == 0){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(GetPeersActivity.this,"No Peer Found",Toast.LENGTH_SHORT).show();
                                if(pd.isShowing()){
                                    pd.dismiss();
                                }
                            }
                        });
                        return;
                    }
                    peerList.clear();
                    for(int i = 0; i < array.length(); i++){
                        JSONObject obj = array.getJSONObject(i);
                        Peers peer = new Peers();
                        peer.setId(String.valueOf(obj.getInt("id")));
                        peer.setAddr(obj.getString("addr"));
                        if(obj.has("addrlocal")){
                            peer.setAddrlocal(obj.getString("addrlocal"));
                        }
                        peer.setServices(obj.getString("services"));
                        if(obj.has("relaytxes")){
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
                            if(pd.isShowing()){
                                pd.dismiss();
                            }
                        }
                    });
                }catch (final Exception e){
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(GetPeersActivity.this,"Error: "+e.getMessage(),Toast.LENGTH_SHORT).show();
                            if(pd.isShowing()){
                                pd.dismiss();
                            }
                        }
                    });
                }
            }
        }.start();
//        Peers peers= new Peers("1","176.9.89.217:19108","192.108","","","","","","",
//                "","","","","","","","","",
//                "");
//        peerList.add(peers);
//        peers= new Peers("2","176.9.89.217:19408","192.108","","","","","","",
//                "","","","","","","","","",
//                "");
//        peerList.add(peers);
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
