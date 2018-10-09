package com.dcrandroid.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.dcrandroid.BuildConfig;
import com.dcrandroid.R;
import com.dcrandroid.data.Constants;
import com.dcrandroid.dialog.StakeyDialog;
import com.dcrandroid.util.DcrConstants;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.Utils;


import mobilewallet.BlockScanResponse;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS |
                    View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // load main preference fragment
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new MainPreferenceFragment()).commit();
    }

    public static class MainPreferenceFragment extends PreferenceFragmentCompat implements BlockScanResponse{
        private PreferenceUtil util;
        private ProgressDialog pd;
        private DcrConstants constants;
        private int buildDateClicks = 0;

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if(getActivity() == null)
                return;

            getActivity().setTitle(getActivity().getString(R.string.settings));
            constants = DcrConstants.getInstance();
            util = new PreferenceUtil(getActivity());
            pd = Utils.getProgressDialog(getActivity(),false,false,getActivity().getString(R.string.scanning_block));
            final EditTextPreference remoteNodeAddress = (EditTextPreference) findPreference(getString(R.string.remote_node_address));
            final EditTextPreference remoteNodeCertificate = (EditTextPreference) findPreference(getString(R.string.key_connection_certificate));
            final Preference rescanBlocks = findPreference(getString(R.string.key_rescan_block));
            final EditTextPreference peerAddress = (EditTextPreference) findPreference(Constants.PEER_IP);
            final ListPreference networkModes = (ListPreference) findPreference(Constants.NETWORK_MODES);
            Preference buildDate = findPreference(getString(R.string.build_date_system));
            buildDate.setSummary(BuildConfig.VERSION_NAME);

            ListPreference currencyConversion = (ListPreference) findPreference(Constants.CURRENCY_CONVERSION);
            currencyConversion.setSummary(getResources().getStringArray(R.array.currency_conversion)[Integer.parseInt(currencyConversion.getValue())]);
            if(Integer.parseInt(util.get(Constants.NETWORK_MODES, "0")) == 1){
                remoteNodeCertificate.setVisible(true);
                remoteNodeAddress.setVisible(true);
                peerAddress.setVisible(false);
                rescanBlocks.setEnabled(true);
            }else {
                remoteNodeCertificate.setVisible(false);
                remoteNodeAddress.setVisible(false);
                peerAddress.setVisible(true);
                rescanBlocks.setEnabled(false);
            }

            networkModes.setSummary(getResources().getStringArray(R.array.network_modes)[Integer.parseInt(util.get(Constants.NETWORK_MODES, "0"))]);
            networkModes.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    int i = Integer.valueOf((String)newValue);
                    if(i == 0){
                        peerAddress.setVisible(true);
                        remoteNodeAddress.setVisible(false);
                        remoteNodeCertificate.setVisible(false);
                        rescanBlocks.setEnabled(false);
                    }else{
                        peerAddress.setVisible(false);
                        remoteNodeAddress.setVisible(true);
                        remoteNodeCertificate.setVisible(true);
                        rescanBlocks.setEnabled(true);
                    }
                    preference.setSummary(getResources().getStringArray(R.array.network_modes)[i]);
                    util.set(Constants.NETWORK_MODES, String.valueOf(i));
                    Toast.makeText(getActivity(), R.string.changes_after_restart, Toast.LENGTH_SHORT).show();
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

                    Toast.makeText(getActivity(), R.string.invalid_peer_address, Toast.LENGTH_SHORT).show();
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
                            .setTitle(R.string.rescan_blocks)
                            .setMessage(R.string.rescan_blocks_confirmation)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    pd.show();
                                    new Thread(){
                                        public void run(){
                                            try {
                                                constants.wallet.rescan(0, MainPreferenceFragment.this);
                                            }catch (Exception e){
                                                e.printStackTrace();
                                            }
                                        }
                                    }.start();
                                }
                            }).setNegativeButton(android.R.string.no, null)
                            .show();
                    return true;
                }
            });

            findPreference("dcrwallet_log").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    String logDir = BuildConfig.IS_TESTNET ? "/dcrwallet/logs/testnet3/dcrwallet.log" : "/dcrwallet/logs/mainnet/dcrwallet.log";

                    Intent i = new Intent(getActivity(), LogViewer.class);
                    i.putExtra("log_path", getContext().getFilesDir() + logDir);
                    startActivity(i);

                    return true;
                }
            });

            if(BuildConfig.IS_TESTNET) {
                findPreference("crash").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        float a = 0 / 0;
                        return true;
                    }
                });
            }else{
                findPreference("crash").setVisible(false);
            }

            currencyConversion.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    preference.setSummary(getResources().getStringArray(R.array.currency_conversion)[Integer.parseInt((newValue.toString()))]);
                    return true;
                }
            });

            buildDate.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if(++buildDateClicks >= 7){
                        buildDateClicks = 0;
                        StakeyDialog stakeyDialog = new StakeyDialog(getContext());
                        stakeyDialog.setCancelable(false);
                        stakeyDialog.setCanceledOnTouchOutside(false);
                        stakeyDialog.show();
                    }
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
                        Toast.makeText(getActivity(), R.string.rescan_cancelled, Toast.LENGTH_SHORT).show();
                    }else {
                        Toast.makeText(getActivity(), height + " " + getString(R.string.blocks_scanned), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        @Override
        public void onError(final String message) {
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