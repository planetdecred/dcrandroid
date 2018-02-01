package com.decrediton.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.view.MenuItem;
import android.widget.Toast;

import com.decrediton.R;
import com.decrediton.util.PreferenceUtil;

public class SettingsActivity extends AppCompatPreferenceActivity {
    private static final String TAG = SettingsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // load settings fragment
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
            EditTextPreference dcrdCertificate = (EditTextPreference) findPreference(getString(R.string.key_connection_certificate));
            localDcrd.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    Boolean b = (boolean) o;
                    if(!b){
                        String address = util.get(getString(R.string.remote_dcrd));
                        if(address.equals("")){
                            System.out.println("Starting local dcrd 1");
                            util.setBoolean(getString(R.string.key_connection_local_dcrd),true);
                            Toast.makeText(getActivity(), "Set remote address first", Toast.LENGTH_SHORT).show();
                            return false;
                        }else{
                            System.out.println("Not Starting local dcrd");
                            util.setBoolean(getString(R.string.key_connection_local_dcrd),false);
                            return true;
                        }
                    }else{
                        System.out.println("Starting local dcrd 2");
                        util.setBoolean(getString(R.string.key_connection_local_dcrd),true);
                        return true;
                    }
                }
            });
            remoteDcrdAddress.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    String address = o.toString();
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
                    System.out.println("Certificate: "+certificate);
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
            // transaction confirmation EditText change listener
            //bindPreferenceSummaryToValue(findPreference(getString(R.string.key_transaction_confirmation)));

            // local dcrd preference change listener
            //bindPreferenceSummaryToValue(findPreference(getString(R.string.key_connection_local_dcrd)));

            // peers preference click listener
            Preference myPref = findPreference(getString(R.string.key_get_peers));
            myPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    getPeers(getActivity());
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

    private static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String stringValue;

            stringValue = newValue.toString();

            if (preference instanceof EditTextPreference) {
                if (preference.getKey().equals("key_transaction_confirmation")) {
                    // update the changed gallery name to summary filed
                      preference.setSummary(stringValue);
                }
                else if(preference.getKey().equals("key_connection_certificate")){
                      preference.setSummary(stringValue);
                }
            } else {
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    public static void getPeers(Context context) {
        Intent intent = new Intent(context.getApplicationContext(),GetPeersActivity.class);
        context.startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(0);
        finishActivity(2);
    }
}
