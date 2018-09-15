package com.dcrandroid.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.MenuItem;
import android.widget.Toast;

import com.dcrandroid.BuildConfig;
import com.dcrandroid.R;
import com.dcrandroid.data.Constants;
import com.dcrandroid.util.DcrConstants;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.Utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import mobilewallet.BlockScanResponse;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // load main preference fragment
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new MainPreferenceFragment()).commit();
    }

    public static class MainPreferenceFragment extends PreferenceFragmentCompat implements BlockScanResponse{
        protected PreferenceUtil util;
        ProgressDialog pd;
        private DcrConstants constants;
        String result;
        SimpleDateFormat formatter;

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getActivity().setTitle("Settings");
            constants = DcrConstants.getInstance();
            util = new PreferenceUtil(getActivity());
            pd = Utils.getProgressDialog(getActivity(),false,false,"Scanning Blocks");
            final EditTextPreference remoteNodeAddress = (EditTextPreference) findPreference(getString(R.string.remote_node_address));
            final EditTextPreference remoteNodeCertificate = (EditTextPreference) findPreference(getString(R.string.key_connection_certificate));
            final Preference rescanBlocks = findPreference(getString(R.string.key_rescan_block));
            final EditTextPreference peerAddress = (EditTextPreference) findPreference(Constants.PEER_IP);
            final ListPreference networkModes = (ListPreference) findPreference("network_modes");
            Preference buildDate = findPreference(getString(R.string.build_date_system));
            formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            Date buildTime = BuildConfig.buildTime;
            result = formatter.format(buildTime);
            buildDate.setSummary(result);
            ListPreference currencyConversion = (ListPreference) findPreference("currency_conversion");
            currencyConversion.setSummary(getResources().getStringArray(R.array.currency_conversion)[Integer.parseInt(currencyConversion.getValue())]);
            if(Integer.parseInt(util.get(Constants.NETWORK_MODES, "0")) == 1){
                remoteNodeCertificate.setEnabled(true);
                remoteNodeAddress.setEnabled(true);
                peerAddress.setEnabled(false);
                rescanBlocks.setEnabled(true);
            }else {
                remoteNodeCertificate.setEnabled(false);
                remoteNodeAddress.setEnabled(false);
                peerAddress.setEnabled(true);
                rescanBlocks.setEnabled(false);
            }
            networkModes.setSummary(getResources().getStringArray(R.array.network_modes)[Integer.parseInt(util.get(Constants.NETWORK_MODES, "0"))]);

            networkModes.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    int i = Integer.valueOf((String)newValue);
                    if(i == 0){
                        peerAddress.setEnabled(true);
                        remoteNodeAddress.setEnabled(false);
                        remoteNodeCertificate.setEnabled(false);
                        rescanBlocks.setEnabled(false);
                    }else{
                        peerAddress.setEnabled(false);
                        remoteNodeAddress.setEnabled(true);
                        remoteNodeCertificate.setEnabled(true);
                        rescanBlocks.setEnabled(true);
                    }
                    preference.setSummary(getResources().getStringArray(R.array.network_modes)[i]);
                    util.set(Constants.NETWORK_MODES, String.valueOf(i));
                    Toast.makeText(getActivity(), "Changes will take effect after app restarts", Toast.LENGTH_SHORT).show();
                    return true;
                }
            });

            peerAddress.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
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
                        return true;
                    }

                    Toast.makeText(getActivity(), "Peer address is invalid", Toast.LENGTH_SHORT).show();
                    return false;
                }
            });

            remoteNodeAddress.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
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
                        return true;
                    }

                    Toast.makeText(getActivity(), R.string.remote_address_invalid, Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
            remoteNodeCertificate.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    String certificate = o.toString();
                    Utils.setRemoteCetificate(getActivity(),certificate);
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

            findPreference("dcrwallet_log").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    //TODO: Make this available for both testnet and mainnet
                    Intent i = new Intent(getActivity(), LogViewer.class);
                    i.putExtra("log_path","/data/data/com.dcrandroid/files/dcrwallet/logs/testnet3/dcrwallet.log");
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

            currencyConversion.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    preference.setSummary(getResources().getStringArray(R.array.currency_conversion)[Integer.parseInt((newValue.toString()))]);
                    return true;
                }
            });
        }

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            setPreferencesFromResource(R.xml.pref_main, s);
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
            if(util.getInt(PreferenceUtil.RESCAN_HEIGHT, 0) < rescanned_through){
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