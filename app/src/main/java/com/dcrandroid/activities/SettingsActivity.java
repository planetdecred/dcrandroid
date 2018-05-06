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
import android.preference.SwitchPreference;
import android.support.v7.app.AlertDialog;
import android.view.MenuItem;
import android.widget.Toast;

import com.dcrandroid.R;
import com.dcrandroid.data.Constants;
import com.dcrandroid.util.DcrConstants;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.Utils;

import mobilewallet.BlockScanResponse;

public class SettingsActivity extends AppCompatPreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // load main preference fragment
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MainPreferenceFragment()).commit();
    }

    public static class MainPreferenceFragment extends PreferenceFragment implements BlockScanResponse{
        protected PreferenceUtil util;
        ProgressDialog pd;
        private DcrConstants constants;
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            constants = DcrConstants.getInstance();
            util = new PreferenceUtil(getActivity());
            pd = Utils.getProgressDialog(getActivity(),false,false,"Scanning Blocks");
            addPreferencesFromResource(R.xml.pref_main);
            final EditTextPreference remoteDcrdAddress = (EditTextPreference) findPreference(getString(R.string.remote_dcrd_address));
            final EditTextPreference dcrdCertificate = (EditTextPreference) findPreference(getString(R.string.key_connection_certificate));
            final Preference dcrLog = findPreference(getString(R.string.dcrd_log_key));
            Preference rescanBlocks = findPreference(getString(R.string.key_rescan_block));
            final EditTextPreference connectToPeer = (EditTextPreference) findPreference(Constants.KEY_PEER_IP);
            final ListPreference networkModes = (ListPreference) findPreference("network_modes");
            if(Integer.parseInt(util.get(Constants.KEY_NETWORK_MODES, "0")) == 2){
                System.out.println("Mode : 2");
                dcrdCertificate.setEnabled(true);
                remoteDcrdAddress.setEnabled(true);
                connectToPeer.setEnabled(false);
                dcrLog.setEnabled(false);
            }
            else if(util.getInt("network_mode") == 1) {
                System.out.println("Mode : 1");
                dcrdCertificate.setEnabled(false);
                remoteDcrdAddress.setEnabled(false);
                connectToPeer.setEnabled(true);
                dcrLog.setEnabled(true);
            }
            else {
                System.out.println("Mode : 0");
                dcrdCertificate.setEnabled(false);
                remoteDcrdAddress.setEnabled(false);
                connectToPeer.setEnabled(true);
                dcrLog.setEnabled(false);
            }
            networkModes.setSummary(getResources().getStringArray(R.array.network_modes)[Integer.parseInt(util.get(Constants.KEY_NETWORK_MODES, "0"))]);

            networkModes.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    int i = Integer.valueOf((String)newValue);
                    preference.setSummary(getResources().getStringArray(R.array.network_modes)[i]);
                    util.set(Constants.KEY_NETWORK_MODES, String.valueOf(i));
                    if(i == 0){
                        connectToPeer.setEnabled(true);
                        remoteDcrdAddress.setEnabled(false);
                        dcrdCertificate.setEnabled(false);
                        dcrLog.setEnabled(false);
                    }else if(i == 1){
                        connectToPeer.setEnabled(true);
                        remoteDcrdAddress.setEnabled(false);
                        dcrdCertificate.setEnabled(false);
                        dcrLog.setEnabled(true);
                    }else{
                        connectToPeer.setEnabled(false);
                        remoteDcrdAddress.setEnabled(true);
                        dcrdCertificate.setEnabled(true);
                        dcrLog.setEnabled(false);
                        Utils.removeDcrdConfig("connect");
                    }
                    Toast.makeText(getActivity(), "Changes will take effect after app restarts", Toast.LENGTH_SHORT).show();
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
                        //Utils.setDcrwalletConfig("spvconnect",address);
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
                    * an ip address or an ip address with
                    * a port
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
                    Utils.setRemoteCetificate(getActivity(),certificate);
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
            findPreference(getString(R.string.key_get_peers)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
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
                                                System.out.println("Rescanning");
                                                constants.wallet.rescan(0, MainPreferenceFragment.this);
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
        public void onEnd(final int height, final boolean cancelled) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(pd.isShowing()){
                        pd.dismiss();
                    }
                    if(cancelled){
                        Toast.makeText(getActivity(), "Rescan cancelled", Toast.LENGTH_SHORT).show();
                    }else {
                        Toast.makeText(getActivity(), height + " " + getString(R.string.blocks_scanned), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        @Override
        public void onError(int code, final String message) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
                }
            });
        }

        @Override
        public boolean onScan(final int rescanned_through) {
            if(util.getInt(PreferenceUtil.RESCAN_HEIGHT) < rescanned_through){
                util.setInt(PreferenceUtil.RESCAN_HEIGHT, rescanned_through);
            }
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pd.show();
                    pd.setMessage(getString(R.string.scanning_block)+" "+rescanned_through);
                }
            });
            return true;
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