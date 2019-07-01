/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricConstants;
import androidx.biometric.BiometricPrompt;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.dcrandroid.BuildConfig;
import com.dcrandroid.MainActivity;
import com.dcrandroid.R;
import com.dcrandroid.data.Constants;
import com.dcrandroid.dialog.DeleteWalletDialog;
import com.dcrandroid.dialog.PasswordDialog;
import com.dcrandroid.dialog.StakeyDialog;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.Utils;
import com.dcrandroid.util.WalletData;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import dcrlibwallet.Dcrlibwallet;
import dcrlibwallet.LibWallet;

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
        private final int BIOMETRIC_AUTH_PASSCODE_REQUEST = 3;

        private PreferenceUtil util;
        private int buildDateClicks = 0;
        private SwitchPreference encryptWallet;
        private ListPreference useBiometric;
        private Preference changeStartupPass;
        private ProgressDialog pd;

        private LibWallet wallet;

        private String biometricAuthNewValue = null;

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (getActivity() == null)
                return;

            getActivity().setTitle(getActivity().getString(R.string.settings));

            wallet = WalletData.getInstance().wallet;

            util = new PreferenceUtil(getActivity());
            pd = Utils.getProgressDialog(getActivity(), false, false, "");
            encryptWallet = (SwitchPreference) findPreference(Constants.ENCRYPT);
            changeStartupPass = findPreference("change_startup_passphrase");
            useBiometric = (ListPreference) findPreference(Constants.USE_BIOMETRIC);
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
                                        wallet.rescanBlocks();
                                        Toast.makeText(getContext(), R.string.check_progress_in_navigation_bar, Toast.LENGTH_SHORT).show();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        if (e.getMessage().equals(Dcrlibwallet.ErrInvalid)) {
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
                                            wallet.unlockWallet(deleteWalletDialog.getPassphrase().getBytes());
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

            findPreference(Constants.WIFI_SYNC).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean wifiSync = (Boolean) newValue;
                    if (getActivity() instanceof MainActivity) {
                        if (wifiSync) {
                            if (!WalletData.getInstance().synced) {
                                ((MainActivity) getActivity()).startSyncing();
                            }
                        } else {
                            ConnectivityManager connectionManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                            if (connectionManager != null) {
                                NetworkInfo networkInfo = connectionManager.getActiveNetworkInfo();
                                if (networkInfo != null && networkInfo.isConnected()) {
                                    if (networkInfo.getType() != ConnectivityManager.TYPE_WIFI) {
                                        wallet.cancelSync();
                                    }
                                } else {
                                    wallet.cancelSync();
                                }
                            }
                        }
                    }
                    return true;
                }
            });

            findPreference(Constants.LICENSE).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent i = new Intent(getContext(), License.class);
                    startActivity(i);
                    return true;
                }
            });

            if (Utils.Biometric.isFingerprintEnrolled(getContext())) {
                useBiometric.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        useBiometric.setEnabled(false);

                        biometricAuthNewValue = (String) newValue;
                        if (util.get(Constants.SPENDING_PASSPHRASE_TYPE).equals(Constants.PIN)) {
                            startActivityForResult(new Intent(getActivity(), EnterPassCode.class), BIOMETRIC_AUTH_PASSCODE_REQUEST);
                        } else {
                            PasswordDialog dialog = new PasswordDialog(getContext())
                                    .setPositiveButton(new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            PasswordDialog passwordDialog = (PasswordDialog) dialog;
                                            savePassword(passwordDialog.getPassword());
                                        }
                                    });

                            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    useBiometric.setEnabled(true);
                                }
                            });

                            dialog.show();
                        }

                        return false;
                    }
                });
            } else {
                useBiometric.setVisible(false);
            }
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
                                    wallet.changePublicPassphrase(passphrase.getBytes(), Constants.INSECURE_PUB_PASSPHRASE.getBytes());
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
            } else if (requestCode == BIOMETRIC_AUTH_PASSCODE_REQUEST) {
                if (resultCode == RESULT_OK) {
                    String passphrase = data.getStringExtra(Constants.PASSPHRASE);
                    savePassword(passphrase);
                } else {
                    // re-enable preference option since the user
                    // canceled the pass code input
                    useBiometric.setEnabled(true);
                }
            }
        }

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            setPreferencesFromResource(R.xml.pref_main, s);
        }

        private void savePassword(final String password) {
            new Thread() {
                public void run() {
                    try {
                        // verify that entered pass is correct
                        wallet.unlockWallet(password.getBytes());
                        wallet.lockWallet();
                        System.out.println("Wallet unlocked successfully");

                        if (biometricAuthNewValue.equals(Constants.OFF)) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        useBiometric.setValue(biometricAuthNewValue);
                                        useBiometric.setEnabled(true);
                                    }
                                });
                            }
                            return;
                        }

                        if (biometricAuthNewValue.equals(Constants.FINGERPRINT)) {
                            Utils.Biometric.savePassToKeystore(getContext(), password, Constants.SPENDING_PASSPHRASE_TYPE);
                            System.out.println("Password saved successfully, checking biometric support");
                        }

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    checkBiometric();
                                }
                            });
                        }

                    } catch (final Exception e) {
                        e.printStackTrace();

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    String errMessage = Utils.translateError(getContext(), e);
                                    Toast.makeText(getContext(), "Error Occurred: " + errMessage, Toast.LENGTH_LONG).show();
                                    useBiometric.setEnabled(true);
                                }
                            });
                        }
                    }
                }
            }.start();
        }

        private void checkBiometric() {
            if (getActivity() == null || getContext() == null) {
                return;
            }

            if (Utils.Biometric.isFingerprintEnrolled(getContext())) {
                try {
                    Executor executor = Executors.newSingleThreadExecutor();
                    BiometricPrompt biometricPrompt = new BiometricPrompt(getActivity(), executor, biometricAuthenticationCallback);
                    BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                            .setTitle(getString(R.string.authentication_required))
                            .setNegativeButtonText(getString(R.string.cancel))
                            .build();

                    biometricPrompt.authenticate(promptInfo);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private BiometricPrompt.AuthenticationCallback biometricAuthenticationCallback = new BiometricPrompt.AuthenticationCallback() {

            @Override
            public void onAuthenticationError(int errorCode, @NonNull final CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                if (getActivity() == null || getContext() == null) {
                    return;
                }

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        useBiometric.setEnabled(true);
                    }
                });

                System.out.println("Biometric Error Code: " + errorCode + " Error String: " + errString);
                if (errorCode == BiometricConstants.ERROR_NEGATIVE_BUTTON) {
                    return;
                }

                final String message = Utils.Biometric.translateError(getContext(), errorCode);

                if (message != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            useBiometric.setEnabled(true);
                            useBiometric.setValue(biometricAuthNewValue);
                        }
                    });
                }
            }
        };

    }
}