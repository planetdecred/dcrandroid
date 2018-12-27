package com.dcrandroid.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.dcrandroid.BuildConfig;
import com.dcrandroid.R;
import com.dcrandroid.data.Constants;
import com.dcrandroid.dialog.DeleteWalletDialog;
import com.dcrandroid.dialog.StakeyDialog;
import com.dcrandroid.util.DcrConstants;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.Utils;

import mobilewallet.Mobilewallet;

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

    public static class MainPreferenceFragment extends PreferenceFragmentCompat {
        private final int ENCRYPT_REQUEST_CODE = 1;
        private final int PASSCODE_REQUEST_CODE = 2;
        private PreferenceUtil util;
        private int buildDateClicks = 0;
        private SwitchPreference encryptWallet;
        private Preference changeStartupPass;
        private ProgressDialog pd;

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (getActivity() == null)
                return;

            getActivity().setTitle(getActivity().getString(R.string.settings));
            util = new PreferenceUtil(getActivity());
            pd = Utils.getProgressDialog(getActivity(), false, false, "");
            encryptWallet = (SwitchPreference) findPreference(Constants.ENCRYPT);
            changeStartupPass = findPreference("change_startup_passphrase");
            final EditTextPreference remoteNodeAddress = (EditTextPreference) findPreference(getString(R.string.remote_node_address));
            final EditTextPreference remoteNodeCertificate = (EditTextPreference) findPreference(getString(R.string.key_connection_certificate));
            final EditTextPreference peerAddress = (EditTextPreference) findPreference(Constants.PEER_IP);
            final ListPreference networkModes = (ListPreference) findPreference(Constants.NETWORK_MODES);
            Preference buildDate = findPreference(getString(R.string.build_date_system));
            buildDate.setSummary(BuildConfig.VERSION_NAME);

            changeStartupPass.setVisible(encryptWallet.isChecked());

            ListPreference currencyConversion = (ListPreference) findPreference(Constants.CURRENCY_CONVERSION);
            currencyConversion.setSummary(getResources().getStringArray(R.array.currency_conversion)[Integer.parseInt(currencyConversion.getValue())]);
            if (Integer.parseInt(util.get(Constants.NETWORK_MODES, "0")) == 1) {
                remoteNodeCertificate.setVisible(true);
                remoteNodeAddress.setVisible(true);
                peerAddress.setVisible(false);
            } else {
                remoteNodeCertificate.setVisible(false);
                remoteNodeAddress.setVisible(false);
                peerAddress.setVisible(true);
            }

            networkModes.setSummary(getResources().getStringArray(R.array.network_modes)[Integer.parseInt(util.get(Constants.NETWORK_MODES, "0"))]);
            networkModes.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    int i = Integer.valueOf((String) newValue);
                    if (i == 0) {
                        peerAddress.setVisible(true);
                        remoteNodeAddress.setVisible(false);
                        remoteNodeCertificate.setVisible(false);
                    } else {
                        peerAddress.setVisible(false);
                        remoteNodeAddress.setVisible(true);
                        remoteNodeCertificate.setVisible(true);
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
                    if (address.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}:(\\d){1,5}$")
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
                    if (address.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}:(\\d){1,5}$")
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
                    Utils.setRemoteCetificate(getActivity(), certificate);
                    return true;
                }
            });

            // peers preference click listener
            findPreference(getString(R.string.key_get_peers)).setVisible(false);
            findPreference(getString(R.string.key_get_peers)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(getActivity(), GetPeersActivity.class);
                    startActivity(intent);
                    return true;
                }
            });

            findPreference("dcrwallet_log").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    Intent i = new Intent(getActivity(), LogViewer.class);
                    i.putExtra("log_path", getContext().getFilesDir() + BuildConfig.LogDir);
                    startActivity(i);

                    return true;
                }
            });

            if (BuildConfig.IS_TESTNET) {
                findPreference("crash").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        new AlertDialog.Builder(getContext())
                                .setTitle(R.string.confirm_crash)
                                .setMessage(R.string.crash_confirmation_description)
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        float a = 0 / 0;
                                    }
                                }).setNegativeButton(android.R.string.no, null)
                                .show();

                        return true;
                    }
                });
            } else {
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
                    if (++buildDateClicks >= 7) {
                        buildDateClicks = 0;
                        StakeyDialog stakeyDialog = new StakeyDialog(getContext());
                        stakeyDialog.setCancelable(false);
                        stakeyDialog.setCanceledOnTouchOutside(false);
                        stakeyDialog.show();
                    }
                    return true;
                }
            });

            findPreference("change_spending_passphrase").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    boolean isPin = util.get(Constants.SPENDING_PASSPHRASE_TYPE).equals(Constants.PIN);
                    Intent intent;
                    if (isPin) {
                        intent = new Intent(getContext(), EnterPassCode.class);
                    } else {
                        intent = new Intent(getContext(), EnterPasswordActivity.class);
                    }

                    intent.putExtra(Constants.CHANGE, true);
                    intent.putExtra(Constants.SPENDING_PASSWORD, true);

                    startActivity(intent);

                    return true;
                }
            });

            changeStartupPass.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    boolean isPin = util.get(Constants.STARTUP_PASSPHRASE_TYPE).equals(Constants.PIN);
                    Intent intent;
                    if (isPin) {
                        intent = new Intent(getContext(), EnterPassCode.class);
                    } else {
                        intent = new Intent(getContext(), EnterPasswordActivity.class);
                    }

                    intent.putExtra(Constants.CHANGE, true);
                    intent.putExtra(Constants.SPENDING_PASSWORD, false);

                    startActivity(intent);

                    return true;
                }
            });

            encryptWallet.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    boolean encrypt = (Boolean) o;
                    Intent intent;
                    if (encrypt) {
                        intent = new Intent(getContext(), ChangePassphrase.class);
                        intent.putExtra(Constants.ENCRYPT, true);
                        intent.putExtra(Constants.SPENDING_PASSWORD, false);

                    } else {
                        if (util.get(Constants.STARTUP_PASSPHRASE_TYPE).equals(Constants.PASSWORD)) {
                            intent = new Intent(getContext(), EnterPasswordActivity.class);
                        } else {
                            intent = new Intent(getContext(), EnterPassCode.class);
                        }

                        intent.putExtra(Constants.SPENDING_PASSWORD, false);
                    }

                    startActivityForResult(intent, ENCRYPT_REQUEST_CODE);

                    return false;
                }
            });

            findPreference("rescan_block").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new AlertDialog.Builder(getContext())
                            .setTitle(R.string.rescan_block)
                            .setMessage(R.string.rescan_blocks_confirmation)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    try {
                                        DcrConstants.getInstance().wallet.rescanBlocks();
                                        Toast.makeText(getContext(), R.string.check_progress_in_navigation_bar, Toast.LENGTH_SHORT).show();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        if (e.getMessage().equals(Mobilewallet.ErrInvalid)) {
                                            Toast.makeText(getContext(), R.string.wallet_is_rescanning, Toast.LENGTH_LONG).show();
                                        } else {
                                            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                            }).setNegativeButton(android.R.string.cancel, null)
                            .show();
                            return true;
                }
            });
        
            findPreference(getString(R.string.delete_wallet)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    final DeleteWalletDialog deleteWalletDialog = new DeleteWalletDialog(getContext());
                    deleteWalletDialog.setTitle(getString(R.string.delete_wallet_prompt_title));
                    deleteWalletDialog.setMessage(getString(R.string.delete_wallet_prompt_message));
                    deleteWalletDialog.setCancelable(true);
                    deleteWalletDialog.setPositiveButton(new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (util.get(Constants.SPENDING_PASSPHRASE_TYPE).equals(Constants.PIN)) {
                                startActivityForResult(new Intent(getActivity(), EnterPassCode.class), PASSCODE_REQUEST_CODE);
                            } else {
                                pd = Utils.getProgressDialog(getContext(), false, false, "Deleting Wallet . . .");
                                pd.show();
                                new Thread() {
                                    public void run() {
                                        try {
                                            DcrConstants.getInstance().wallet.unlockWallet(deleteWalletDialog.getPassphrase().getBytes());
                                            if (getActivity() != null) {
                                                Utils.clearApplicationData(getActivity());
                                                getActivity().runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        pd.dismiss();
                                                        Utils.restartApp(getContext());
                                                    }
                                                });
                                            }
                                        } catch (final Exception e) {
                                            e.printStackTrace();
                                            if (getActivity() != null) {
                                                getActivity().runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        deleteWalletDialog.dismiss();
                                                        Toast.makeText(getActivity(), getString(R.string.invalid_passphrase), Toast.LENGTH_LONG).show();
                                                        pd.dismiss();
                                                    }
                                                });
                                            }
                                        }
                                    }
                                }.start();
                            }
                        }
                    }).show();
                    deleteWalletDialog.show();
                    return true;
                }
            });
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, final Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == ENCRYPT_REQUEST_CODE) {
                if (resultCode == RESULT_OK) {
                    if (encryptWallet.isChecked()) {
                        String message = util.get(Constants.STARTUP_PASSPHRASE_TYPE).equals(Constants.PASSWORD) ? getString(R.string.removing_startup_pass) : getString(R.string.removing_startup_pin);
                        pd = Utils.getProgressDialog(getContext(), false, false, message);
                        pd.show();
                        new Thread() {
                            public void run() {
                                try {
                                    String passphrase = data.getStringExtra(Constants.PASSPHRASE);
                                    DcrConstants.getInstance().wallet.changePublicPassphrase(passphrase.getBytes(), Constants.INSECURE_PUB_PASSPHRASE.getBytes());
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                encryptWallet.setChecked(false);
                                                changeStartupPass.setVisible(false);
                                                util.set(Constants.STARTUP_PASSPHRASE_TYPE, Constants.EMPTY_STRING);
                                                pd.dismiss();
                                            }
                                        });
                                    }
                                } catch (final Exception e) {
                                    e.printStackTrace();
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                                pd.dismiss();
                                            }
                                        });
                                    }
                                }
                            }
                        }.start();
                    } else {
                        encryptWallet.setChecked(!encryptWallet.isChecked());
                        changeStartupPass.setVisible(encryptWallet.isChecked());
                    }
                }
            } else if (requestCode == PASSCODE_REQUEST_CODE) {
                if (resultCode == RESULT_OK) {
                    pd = Utils.getProgressDialog(getContext(), false, false, getString(R.string.deleting_wallet));
                    pd.show();
                    if (getActivity() != null) {
                        Utils.clearApplicationData(getActivity());
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pd.dismiss();
                                Utils.restartApp(getActivity());
                            }
                        });
                    }
                }
            }
        }

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            setPreferencesFromResource(R.xml.pref_main, s);
        }
    }
}