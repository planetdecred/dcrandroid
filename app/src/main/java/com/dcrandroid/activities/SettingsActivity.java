package com.dcrandroid.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AlertDialog;
import android.view.MenuItem;
import android.widget.Toast;

import com.dcrandroid.R;
import com.dcrandroid.data.BestBlock;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.Utils;

import org.json.JSONObject;

import java.util.Locale;

import dcrwallet.BlockScanResponse;
import dcrwallet.Dcrwallet;

public class SettingsActivity extends AppCompatPreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // load main preference fragment
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MainPreferenceFragment()).commit();
    }

    public static class MainPreferenceFragment extends PreferenceFragment implements BlockScanResponse {
        protected PreferenceUtil util;
        ProgressDialog pd;
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            util = new PreferenceUtil(getActivity());
            pd = Utils.getProgressDialog(getActivity(),false,false,"Scanning Blocks");
            addPreferencesFromResource(R.xml.pref_main);
            final EditTextPreference remoteDcrdAddress = (EditTextPreference) findPreference(getString(R.string.remote_dcrd_address));
            final EditTextPreference dcrdCertificate = (EditTextPreference) findPreference(getString(R.string.key_connection_certificate));
            final Preference currentBlockHeight = findPreference(getString(R.string.key_current_block_height));
            Preference rescanBlocks = findPreference(getString(R.string.key_rescan_block));
            final EditTextPreference connectToPeer = (EditTextPreference) findPreference("peer_ip");
            final ListPreference networkModes = (ListPreference) findPreference("network_modes");
            if(util.getInt("network_mode") == 2){
                System.out.println("Mode : 2");
                dcrdCertificate.setEnabled(true);
                remoteDcrdAddress.setEnabled(true);
                connectToPeer.setEnabled(false);
            }else {
                System.out.println("Mode : 1 || 0");
                dcrdCertificate.setEnabled(false);
                remoteDcrdAddress.setEnabled(false);
                connectToPeer.setEnabled(true);
            }
            connectToPeer.setText(util.get("peer_address"));
            networkModes.setSummary(getResources().getStringArray(R.array.network_modes)[util.getInt("network_mode")]);
            networkModes.setValueIndex(util.getInt("network_mode"));
            /*
            * Get the current block height from the chain server, parse it and display it
            * */
            new Thread(){
                public void run(){
                    for(;;) {
                        try {
                            if(getActivity() == null){
                                break;
                            }
                            final BestBlock bestBlock = Utils.parseBestBlock(Dcrwallet.runDcrCommands(getActivity().getString(R.string.getbestblock)));
                            JSONObject rawBlock = new JSONObject(Dcrwallet.runDcrCommands("getblockheader "+bestBlock.getHash()));
                            final long lastBlockTime = rawBlock.getLong("time");
                            long currentTime = System.currentTimeMillis() / 1000;
                            //TODO: Make available for both testnet and mainnet
                            final long estimatedBlocks = (currentTime - lastBlockTime) / 120;
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if(estimatedBlocks > bestBlock.getHeight()) {
                                        currentBlockHeight.setSummary(String.format(Locale.getDefault(),"%d blocks (%d blocks behind)", bestBlock.getHeight(), estimatedBlocks-bestBlock.getHeight()));
                                    }else{
                                        currentBlockHeight.setSummary(String.format(Locale.getDefault(),"%d blocks (Last block %d seconds ago)", bestBlock.getHeight(), (System.currentTimeMillis()/1000) - lastBlockTime));
                                    }
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }.start();

            networkModes.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    int i = Integer.valueOf((String)newValue);
                    preference.setSummary(getResources().getStringArray(R.array.network_modes)[i]);
                    util.setInt("network_mode", i);
                    if(i == 0 || i == 1){
                        connectToPeer.setEnabled(true);
                        remoteDcrdAddress.setEnabled(false);
                        dcrdCertificate.setEnabled(false);
                        util.setBoolean("connect_to_peer",true);
                        //util.setBoolean(getString(R.string.key_connection_local_dcrd), false);
                    }else{
                        connectToPeer.setEnabled(false);
                        remoteDcrdAddress.setEnabled(true);
                        dcrdCertificate.setEnabled(true);
                        util.setBoolean("connect_to_peer",false);
                        ///util.setBoolean(getString(R.string.key_connection_local_dcrd), true);
                        Utils.removeDcrwalletConfig("spvconnect");
                        Utils.removeDcrdConfig("connect");
                    }
                    if(i == 0){
                        Utils.setDcrwalletConfig("spv","true");
                    }else{
                        Utils.removeDcrwalletConfig("spv");
                    }
                    return true;
                }
            });

            connectToPeer.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String address = newValue.toString();
                    /*
                    * Check if the address entered by the user matches
                    * an ip address or an ip address with a port
                    * e.g 127.0.0.1 or 127.0.0.1:19109
                    * */
                    if(address.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}:(\\d){1,5}$")
                            || address.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$")
                            || address.equals("")) {
                        util.set("peer_address", address);
                        Utils.setDcrwalletConfig("spvconnect",address);
                        Utils.setDcrdConfiguration("connect",address);
                        return true;
                    }else{
                        Toast.makeText(getActivity(), "Peer address is invalid", Toast.LENGTH_SHORT).show();
                    }
                    return false;
                }
            });

            remoteDcrdAddress.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    String address = o.toString();
                    /*
                    * Check if the address entered by the user matches
                    * an ip address or an ip address with a port
                    * e.g 127.0.0.1 or 127.0.0.1:19109
                    * */
                    if(address.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}:(\\d){1,5}$")
                            || address.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$")) {
                        util.set(getActivity().getString(R.string.remote_dcrd), o.toString());
                        return true;
                    }else{
                        Toast.makeText(getActivity(), R.string.remote_address_invalid, Toast.LENGTH_SHORT).show();
                    }
                    return false;
                }
            });
            dcrdCertificate.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    String certificate = o.toString();
                    System.out.println("Cert: "+certificate);
                    Utils.setRemoteCetificate(getActivity(),certificate);
                    //util.set(getActivity().getString(R.string.remote_certificate), certificate);
                    return true;
                }
            });
            findPreference("discover").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent i = new Intent(getActivity(), DiscoverAddress.class);
                    startActivity(i);
                    return true;
                }
            });

            // peers preference click listener
            Preference peers = findPreference(getString(R.string.key_get_peers));
            peers.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(getActivity(),GetPeersActivity.class);
                    startActivity(intent);
                    return true;
                }
            });

            rescanBlocks.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle("Rescan blocks")
                            .setMessage("Are you sure? This could take some time.")
                            .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    pd.show();
                                    new Thread(){
                                        public void run(){
                                            try {
                                                Looper.prepare();
                                                Dcrwallet.reScanBlocks(MainPreferenceFragment.this,0);
                                            }catch (Exception e){
                                                e.printStackTrace();
                                            }
                                        }
                                    }.start();
                                }
                            }).setNegativeButton("NO", null)
                            .show();
                    return true;
                }
            });
            findPreference("dcrd_log").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    //TODO: Make this available for both testnet and mainnet
                    Intent i = new Intent(getActivity(), LogViewer.class);
                    i.putExtra("log_path","/data/data/com.dcrandroid/files/dcrd/logs/testnet2/dcrd.log");
                    startActivity(i);
                    return true;
                }
            });
            findPreference("dcrwallet_log").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    //TODO: Make this available for both testnet and mainnet
                    Intent i = new Intent(getActivity(), LogViewer.class);
                    i.putExtra("log_path","/data/data/com.dcrandroid/files/dcrwallet/logs/testnet2/dcrwallet.log");
                    startActivity(i);
                    return true;
                }
            });

            findPreference("crash").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    float a = 0/0;
                    return true;
                }
            });

        }

        @Override
        public void onEnd(final long height) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(pd.isShowing()){
                        pd.dismiss();
                    }
                    Toast.makeText(getActivity(), height+" "+getString(R.string.blocks_scanned), Toast.LENGTH_SHORT).show();
                    util.setInt("block_checkpoint", (int) height);
                }
            });
        }

        @Override
        public void onScan(final long rescanned_through) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pd.show();
                    PreferenceUtil util = new PreferenceUtil(MainPreferenceFragment.this.getActivity());
                    //int percentage = (int) ((rescanned_through/Float.parseFloat(util.get(PreferenceUtil.BLOCK_HEIGHT))) * 100);
                    pd.setMessage(getString(R.string.scanning_block)+" "+rescanned_through);
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(0);
        finishActivity(2);
    }
}