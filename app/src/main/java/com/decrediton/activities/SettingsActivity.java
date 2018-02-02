package com.decrediton.activities;

import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.view.MenuItem;
import android.widget.Toast;

import com.decrediton.R;
import com.decrediton.data.BestBlock;
import com.decrediton.util.PreferenceUtil;
import com.decrediton.util.Utils;

import dcrwallet.Dcrwallet;

public class SettingsActivity extends AppCompatPreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // load main preference fragment
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MainPreferenceFragment()).commit();
    }

    public static class MainPreferenceFragment extends PreferenceFragment {
        protected PreferenceUtil util;
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            util = new PreferenceUtil(getActivity());
            addPreferencesFromResource(R.xml.pref_main);
            final SwitchPreference localDcrd = (SwitchPreference) findPreference(getString(R.string.key_connection_local_dcrd));
            final EditTextPreference remoteDcrdAddress = (EditTextPreference) findPreference(getString(R.string.remote_dcrd_address));
            final EditTextPreference dcrdCertificate = (EditTextPreference) findPreference(getString(R.string.key_connection_certificate));
            final Preference currentBlockHeight = findPreference(getString(R.string.key_current_block_height));

            /*
            * If local chain server is disabled, enable dcrdCertificate.
            * It is disabled by default
            * */
            if (!util.getBoolean(getString(R.string.key_connection_local_dcrd), true)) {
                dcrdCertificate.setEnabled(true);
            }
            /*
            * Get the current block height from the chain server, parse it and display it
            * */
            new Thread(){
                public void run(){
                    try{
                        final BestBlock bestBlock = Utils.parseBestBlock(Dcrwallet.runDcrCommands(getActivity().getString(R.string.getbestblock)));
                        //System.out.println("Block Hash: "+bestBlock.getHash()+", Block Height: "+bestBlock.getHeight());
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                currentBlockHeight.setSummary(String.valueOf(bestBlock.getHeight()));
                            }
                        });
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }.start();
            localDcrd.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    Boolean b = (boolean) o;
                    if(!b){
                        String address = util.get(getString(R.string.remote_dcrd));
                        if(address.equals("")){
                            util.setBoolean(getString(R.string.key_connection_local_dcrd),true);
                            dcrdCertificate.setEnabled(false);
                            Toast.makeText(getActivity(), "Set remote address first", Toast.LENGTH_SHORT).show();
                            return false;
                        }else{
                            util.setBoolean(getString(R.string.key_connection_local_dcrd),false);
                            dcrdCertificate.setEnabled(true);
                            return true;
                        }
                    }else{
                        util.setBoolean(getString(R.string.key_connection_local_dcrd),true);
                        dcrdCertificate.setEnabled(false);
                        return true;
                    }
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
                        Toast.makeText(getActivity(), "Remote address is invalid", Toast.LENGTH_SHORT).show();
                    }
                    return false;
                }
            });
            dcrdCertificate.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    String certificate = o.toString();
                    util.set(getActivity().getString(R.string.remote_certificate), certificate);
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