package com.dcrandroid;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dcrandroid.activities.SettingsActivity;
import com.dcrandroid.adapter.NavigationListAdapter;
import com.dcrandroid.adapter.NavigationListAdapter.NavigationBarItem;
import com.dcrandroid.data.Account;
import com.dcrandroid.data.Constants;
import com.dcrandroid.dialog.WiFiSyncDialog;
import com.dcrandroid.fragments.AccountsFragment;
import com.dcrandroid.fragments.HelpFragment;
import com.dcrandroid.fragments.HistoryFragment;
import com.dcrandroid.fragments.OverviewFragment;
import com.dcrandroid.fragments.ReceiveFragment;
import com.dcrandroid.fragments.SecurityFragment;
import com.dcrandroid.fragments.SendFragment;
import com.dcrandroid.service.SyncService;
import com.dcrandroid.util.CoinFormat;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.TransactionsResponse;
import com.dcrandroid.util.TransactionsResponse.TransactionInput;
import com.dcrandroid.util.TransactionsResponse.TransactionOutput;
import com.dcrandroid.util.Utils;
import com.dcrandroid.util.WalletData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import dcrlibwallet.Dcrlibwallet;
import dcrlibwallet.SpvSyncResponse;
import dcrlibwallet.TransactionListener;

public class MainActivity extends AppCompatActivity implements TransactionListener,
        SpvSyncResponse, View.OnClickListener {

    private final double RESCAN_PERCENTAGE = 0.1;
    private final double DISCOVERY_PERCENTAGE = 0.8;

    private TextView chainStatus, bestBlockTime, connectionStatus, totalBalance;
    private ImageView stopScan, syncIndicator;
    private ProgressBar syncProgressBar;
    private ListView mListView;

    private Fragment currentFragment;
    private SendFragment sendFragment = new SendFragment();
    private SecurityFragment securityFragment = new SecurityFragment();

    private WalletData walletData;
    private PreferenceUtil util;
    private NotificationManager notificationManager;
    private Animation rotateAnimation;
    private SoundPool alertSound;
    private Thread blockUpdate, accountDiscoveryProgress;

    private Handler handler = new Handler();
    private Intent broadcastIntent = null;

    private int bestBlock = 0, blockNotificationSound, pageID;
    private long bestBlockTimestamp;
    private boolean scanning = false, isForeground;

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        System.out.println("Memory Trim: " + level);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View view = getCurrentFocus();
        if (view != null && (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_MOVE) && view instanceof EditText && !view.getClass().getName().startsWith("android.webkit.")) {
            EditText activeText = (EditText) view;
            activeText.clearFocus();
            int scrcoords[] = new int[2];
            view.getLocationOnScreen(scrcoords);
            float x = ev.getRawX() + view.getLeft() - scrcoords[0];
            float y = ev.getRawY() + view.getTop() - scrcoords[1];
            if (x < view.getLeft() || x > view.getRight() || y < view.getTop() || y > view.getBottom())
                ((InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow((this.getWindow().getDecorView().getApplicationWindowToken()), 0);
        }
        return super.dispatchTouchEvent(ev);
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        connectionStatus = findViewById(R.id.tv_connection_status);
        bestBlockTime = findViewById(R.id.best_block_time);
        chainStatus = findViewById(R.id.chain_status);
        stopScan = findViewById(R.id.iv_stop_rescan);
        totalBalance = findViewById(R.id.tv_total_balance);
        syncIndicator = findViewById(R.id.iv_sync_indicator);
        syncProgressBar = findViewById(R.id.pb_sync_progress);
        mListView = findViewById(R.id.lv_nav);

        connectionStatus.setOnClickListener(this);

        if (BuildConfig.IS_TESTNET) {
            findViewById(R.id.tv_testnet).setVisibility(View.VISIBLE);
        } else {
            ImageView decredSymbol = findViewById(R.id.nav_bar_logo);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) decredSymbol.getLayoutParams();
            params.bottomMargin = (int) getResources().getDimension(R.dimen.mainnet_logo_bottom_margin);
            decredSymbol.setLayoutParams(params);
        }

        String[] itemTitles = getResources().getStringArray(R.array.nav_list_titles);
        int[] itemIcons = new int[]{R.drawable.overview, R.drawable.history, R.mipmap.send,
                R.mipmap.receive, R.drawable.account, R.drawable.security, R.drawable.settings, R.drawable.help};
        ArrayList<NavigationBarItem> items = new ArrayList<>();
        for (int i = 0; i < itemTitles.length; i++) {
            NavigationBarItem item = new NavigationBarItem(itemTitles[i], itemIcons[i]);
            items.add(item);
        }

        mListView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switchFragment(position);
            }
        });

        NavigationListAdapter listAdapter = new NavigationListAdapter(this, items);
        mListView.setAdapter(listAdapter);

        if (itemTitles.length > 0) {
            mListView.setItemChecked(0, true);
        }

        displayOverview();

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        rotateAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.anim_rotate);
        rotateAnimation.setRepeatCount(-1);
        syncIndicator.setBackgroundResource(R.drawable.sync_animation);
        syncIndicator.post(new Runnable() {
            @Override
            public void run() {
                AnimationDrawable syncAnimation = (AnimationDrawable) syncIndicator.getBackground();
                syncAnimation.start();
            }
        });

        stopScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanning = false;
            }
        });
    }

    private void registerNotificationChannel() {
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("new transaction", getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setImportance(NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS |
                    View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }

        setContentView(R.layout.activity_main);

        initViews();

        registerNotificationChannel();

        util = new PreferenceUtil(this);
        walletData = WalletData.getInstance();

        if (walletData.wallet == null) {
            System.out.println("Restarting app");
            Utils.restartApp(this);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            SoundPool.Builder builder = new SoundPool.Builder().setMaxStreams(3);
            AudioAttributes attributes = new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION).build();
            builder.setAudioAttributes(attributes);
            alertSound = builder.build();
        } else {
            alertSound = new SoundPool(3, AudioManager.STREAM_NOTIFICATION, 0);
        }

        blockNotificationSound = alertSound.load(MainActivity.this, R.raw.beep, 1);

        walletData.wallet.transactionNotification(this);
        walletData.wallet.addSyncResponse(this);

        displayBalance();

        checkWifiSync();

        startBlockUpdate();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isForeground = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        isForeground = true;

        if (walletData.peers == 0 && walletData.synced) {
            // restart spv synchronization.
            connectionStatus.performClick();
        }

        if (broadcastIntent != null)
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (broadcastIntent != null) {
                        sendBroadcast(broadcastIntent);
                    }
                }
            }, 1000);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getAction() != null && intent.getAction().equals(Constants.NEW_TRANSACTION_NOTIFICATION)) {
            if (pageID != 0)
                displayOverview();
        }
    }

    public void displayBalance() {
        try {
            final ArrayList<com.dcrandroid.data.Account> accounts = Account.parse(walletData.wallet.getAccounts(util.getBoolean(Constants.SPEND_UNCONFIRMED_FUNDS) ? 0 : Constants.REQUIRED_CONFIRMATIONS));
            long walletBalance = 0;
            for (int i = 0; i < accounts.size(); i++) {
                if (util.getBoolean(Constants.HIDE_WALLET + accounts.get(i).getAccountNumber())) {
                    continue;
                }
                walletBalance += accounts.get(i).getBalance().getTotal();
            }

            totalBalance.setText(CoinFormat.Companion.format(Utils.formatDecredWithComma(walletBalance) + " DCR"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkWifiSync() {

        // One-time notice
        if (util.getBoolean(Constants.FIRST_RUN, true)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.welcome_title)
                    .setMessage(R.string.first_run_notice)
                    .setPositiveButton(R.string.ok, null)
                    .show();

            util.setBoolean(Constants.FIRST_RUN, false);
        }

        if (!util.getBoolean(Constants.WIFI_SYNC, false)) {
            // Check if wifi is connected
            ConnectivityManager connectionManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectionManager != null) {
                NetworkInfo networkInfo = connectionManager.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected()) {
                    if (networkInfo.getType() != ConnectivityManager.TYPE_WIFI) {
                        setConnectionStatus(R.string.connect_to_wifi);
                        showWifiNotice();
                        return;
                    }
                } else {
                    setConnectionStatus(R.string.connect_to_wifi);
                    showWifiNotice();
                    return;
                }
            }
        }

        startSyncing();
    }

    private void showWifiNotice() {
        WiFiSyncDialog wifiSyncDialog = new WiFiSyncDialog(this)
                .setPositiveButton(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startSyncing();
                        WiFiSyncDialog syncDialog = (WiFiSyncDialog) dialog;
                        util.setBoolean(Constants.WIFI_SYNC, syncDialog.getChecked());
                    }
                });

        wifiSyncDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                walletData.syncing = false;
                setupSyncedLayout();
                sendBroadcast(new Intent(Constants.SYNCED));
                setConnectionStatus(R.string.connect_to_wifi);
                connectionStatus.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
            }
        });

        wifiSyncDialog.show();
    }

    public void startSyncing() {
        walletData.syncing = true;

        if (Integer.parseInt(util.get(Constants.NETWORK_MODES, "0")) == 0) {
            setConnectionStatus(R.string.connecting_to_peers);
        } else {
            setConnectionStatus(R.string.connecting_to_rpc_server);
        }

        Intent syncIntent = new Intent(this, SyncService.class);
        startService(syncIntent);
    }

    private void startBlockUpdate() {
        if (blockUpdate != null) {
            return;
        }

        blockUpdate = new Thread() {
            public void run() {
                while (!this.isInterrupted()) {
                    try {
                        if (!scanning && !walletData.syncing) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setBestBlockTime(bestBlockTimestamp);
                                }
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            }
        };
        blockUpdate.start();
    }

    private void setConnectionStatus(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectionStatus.setText(str);
            }
        });
    }

    private void setConnectionStatus(@StringRes final int resid) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectionStatus.setText(resid);
            }
        });
    }

    private void setChainStatus(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                chainStatus.setText(str);
            }
        });
    }

    private void setBestBlockTime(final long seconds) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (seconds == -1) {
                    bestBlockTime.setText(null);
                    return;
                }

                if (!walletData.syncing) {
                    bestBlockTime.setText(Utils.calculateTime((System.currentTimeMillis() / 1000) - seconds, MainActivity.this));
                } else {
                    bestBlockTime.setText(Utils.calculateDaysAgo((System.currentTimeMillis() / 1000) - seconds, MainActivity.this));
                }
            }
        });
    }

    private void showText(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (pageID == 0 && drawer.isDrawerOpen(GravityCompat.START)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.exit_app_prompt_title)
                    .setMessage(R.string.exit_app_prompt_message)
                    .setNegativeButton(android.R.string.no, null)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface arg0, int arg1) {
                            MainActivity.super.onBackPressed();
                        }
                    }).create().show();
        } else if (pageID == 0 && !drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.openDrawer(GravityCompat.START);
        } else if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (pageID == 0) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.exit_app_prompt_title)
                    .setMessage(R.string.exit_app_prompt_message)
                    .setNegativeButton(android.R.string.no, null)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface arg0, int arg1) {
                            MainActivity.super.onBackPressed();
                        }
                    }).create().show();
        } else {
            displayOverview();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (blockUpdate != null && !blockUpdate.isInterrupted()) {
            blockUpdate.interrupt();
        }

        Intent syncIntent = new Intent(this, SyncService.class);
        stopService(syncIntent);

        if (walletData.wallet != null) {
            walletData.wallet.shutdown();
        }

        finish();
    }

    public void switchFragment(int position) {
        switch (position) {
            case 0:
                currentFragment = new OverviewFragment();
                break;
            case 1:
                currentFragment = new HistoryFragment();
                break;
            case 2:
                currentFragment = sendFragment;
                break;
            case 3:
                currentFragment = new ReceiveFragment();
                break;
            case 4:
                currentFragment = new AccountsFragment();
                break;
            case 5:
                currentFragment = securityFragment;
                break;
            case 6:
                currentFragment = new SettingsActivity.MainPreferenceFragment();
                break;
            case 7:
                currentFragment = new HelpFragment();
                break;
            default:
                return;
        }

        pageID = position;

        //Change the currentFragment
        getSupportFragmentManager().beginTransaction().replace(R.id.frame, currentFragment).commit();

        //Close Navigation Drawer
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
    }

    public void displayOverview() {
        switchFragment(0);
        mListView.setItemChecked(0, true);
    }

    public void displayHistory() {
        switchFragment(1);
        mListView.setItemChecked(1, true);
    }

    public void displaySend() {
        switchFragment(2);
        mListView.setItemChecked(2, true);
    }

    public void displayReceive() {
        switchFragment(3);
        mListView.setItemChecked(3, true);
    }

    @Override
    public void onTransaction(String s) {
        System.out.println("Notification Received: " + s);
        try {
            JSONObject obj = new JSONObject(s);

            TransactionsResponse.TransactionItem transaction = new TransactionsResponse.TransactionItem();

            transaction.timestamp = obj.getLong(Constants.TIMESTAMP);
            transaction.fee = obj.getLong(Constants.FEE);
            transaction.type = obj.getString(Constants.TYPE);
            transaction.hash = obj.getString(Constants.HASH);
            transaction.height = obj.getInt(Constants.HEIGHT);
            transaction.amount = obj.getLong(Constants.AMOUNT);
            transaction.direction = obj.getInt(Constants.DIRECTION);

            long totalInput = 0, totalOutput = 0;

            ArrayList<TransactionsResponse.TransactionInput> inputs = new ArrayList<>();
            JSONArray debits = obj.getJSONArray(Constants.DEBITS);
            for (int i = 0; i < debits.length(); i++) {
                JSONObject debit = debits.getJSONObject(i);

                TransactionInput input = new TransactionInput();
                input.index = debit.getInt(Constants.INDEX);
                input.previous_account = debit.getLong(Constants.PREVIOUS_ACCOUNT);
                input.previous_amount = debit.getLong(Constants.PREVIOUS_AMOUNT);
                input.accountName = debit.getString(Constants.ACCOUNT_NAME);
                totalInput += debit.getLong(Constants.PREVIOUS_ACCOUNT);

                inputs.add(input);
            }

            ArrayList<TransactionOutput> outputs = new ArrayList<>();
            JSONArray credits = obj.getJSONArray(Constants.CREDITS);
            for (int i = 0; i < credits.length(); i++) {
                JSONObject credit = credits.getJSONObject(i);
                TransactionOutput output = new TransactionOutput();
                output.account = credit.getInt(Constants.ACCOUNT);
                output.internal = credit.getBoolean(Constants.INTERNAL);
                output.address = credit.getString(Constants.ADDRESS);
                output.index = credit.getInt(Constants.INDEX);
                output.amount = credit.getLong(Constants.AMOUNT);
                totalOutput += credit.getLong(Constants.AMOUNT);
            }

            transaction.totalInput = totalInput;
            transaction.totalOutputs = totalOutput;
            transaction.inputs = inputs;
            transaction.outputs = outputs;

            if (currentFragment instanceof OverviewFragment) {
                OverviewFragment overviewFragment = (OverviewFragment) currentFragment;
                overviewFragment.newTransaction(transaction);
            } else if (currentFragment instanceof HistoryFragment) {
                HistoryFragment historyFragment = (HistoryFragment) currentFragment;
                historyFragment.newTransaction(transaction);
            }

            if (util.getBoolean(Constants.TRANSACTION_NOTIFICATION, true)) {
                double fee = obj.getDouble(Constants.FEE);
                if (fee == 0) {
                    BigDecimal satoshi = BigDecimal.valueOf(obj.getLong(Constants.AMOUNT));

                    BigDecimal amount = satoshi.divide(BigDecimal.valueOf(1e8), new MathContext(100));
                    DecimalFormat format = new DecimalFormat(getString(R.string.you_received) + " #.######## DCR");
                    util.set(Constants.TX_NOTIFICATION_HASH, transaction.hash);
                    sendNotification(format.format(amount), (int) transaction.totalInput + (int) transaction.totalOutputs + (int) transaction.timestamp);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            if (util.getBoolean(Constants.DEBUG_MESSAGES)) {
                showText(e.getMessage());
            }
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                displayBalance();
            }
        });
    }

    @Override
    public void onTransactionConfirmed(final String hash, final int height) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                displayBalance();

                if (currentFragment instanceof OverviewFragment) {
                    OverviewFragment overviewFragment = (OverviewFragment) currentFragment;
                    overviewFragment.transactionConfirmed(hash, height);
                } else if (currentFragment instanceof HistoryFragment) {
                    HistoryFragment historyFragment = (HistoryFragment) currentFragment;
                    historyFragment.transactionConfirmed(hash, height);
                }
            }
        });
    }

    @Override
    public void onBlockAttached(int height, long timestamp) {
        this.bestBlock = height;
        this.bestBlockTimestamp = timestamp / 1000000000;
        if (util.getBoolean(Constants.NEW_BLOCK_NOTIFICATION, false)) {
            alertSound.play(blockNotificationSound, 1, 1, 1, 0, 1);
        }
        if (!walletData.syncing) {
            String status = String.format(Locale.getDefault(), "%s: %d", getString(R.string.latest_block), bestBlock);
            setChainStatus(status);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    displayBalance();
                }
            });
            setBestBlockTime(bestBlockTimestamp);
        }

        if (currentFragment instanceof OverviewFragment) {
            OverviewFragment overviewFragment = (OverviewFragment) currentFragment;
            overviewFragment.blockAttached(height);
        } else if (currentFragment instanceof HistoryFragment) {
            HistoryFragment historyFragment = (HistoryFragment) currentFragment;
            historyFragment.blockAttached(height);
        }
    }

    private void sendNotification(String amount, int nonce) {
        Intent launchIntent = new Intent(this, MainActivity.class);
        launchIntent.setAction(Constants.NEW_TRANSACTION_NOTIFICATION);
        PendingIntent launchPendingIntent = PendingIntent.getActivity(this, 1, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder(this, "new transaction")
                .setContentTitle(getString(R.string.new_transaction))
                .setContentText(amount)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setOngoing(false)
                .setAutoCancel(true)
                .setGroup(Constants.TRANSACTION_NOTIFICATION_GROUP)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(launchPendingIntent)
                .build();
        Notification groupSummary = new NotificationCompat.Builder(this, "new transaction")
                .setContentTitle(getString(R.string.new_transaction))
                .setContentText(getString(R.string.new_transaction))
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setGroup(Constants.TRANSACTION_NOTIFICATION_GROUP)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();

        notificationManager.notify(nonce, notification);
        notificationManager.notify(Constants.TRANSACTION_SUMMARY_ID, groupSummary);

    }

    @Override
    public void onSyncError(long l, final Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onFetchedHeaders(int fetchedHeadersCount, long lastHeaderTime, String state) {
        if (!walletData.syncing) {
            // Ignore this call because this function gets called for each peer and
            // we'd want to ignore those calls as far as the wallet is synced.
            return;
        } else if (walletData.totalFetchTime != -1) {
            return;
        }

        long currentTime = System.currentTimeMillis() / 1000;
        long estimatedBlocks = ((currentTime - walletData.wallet.getBestBlockTimeStamp()) / BuildConfig.TargetTimePerBlock) + walletData.wallet.getBestBlock();

        switch (state) {
            case Dcrlibwallet.START:
                if (walletData.fetchHeaderTime != -1) {
                    return;
                }

                setConnectionStatus(R.string.fetching_headers);

                walletData.syncStartPoint = walletData.wallet.getBestBlock();
                walletData.syncEndPoint = (int) estimatedBlocks - walletData.syncStartPoint;
                walletData.syncCurrentPoint = walletData.syncStartPoint;
                walletData.fetchHeaderTime = System.currentTimeMillis();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        syncProgressBar.setProgress(0);
                        syncProgressBar.setVisibility(View.VISIBLE);
                    }
                });

                break;
            case Dcrlibwallet.PROGRESS:

                walletData.syncEndPoint = (int) estimatedBlocks - walletData.syncStartPoint;
                walletData.syncCurrentPoint += fetchedHeadersCount;

                int count = walletData.syncCurrentPoint;
                if (walletData.syncStartPoint > 0) {
                    count -= walletData.syncStartPoint;
                }

                float percent = (float) count / walletData.syncEndPoint;
                double totalFetchTime = (System.currentTimeMillis() - walletData.fetchHeaderTime) / percent;
                long remainingFetchTime = Math.round(totalFetchTime) - (System.currentTimeMillis() - walletData.fetchHeaderTime);
                long elapsedFetchTime = System.currentTimeMillis() - walletData.fetchHeaderTime;

                //10% of fetch time is used for estimating both rescan while 80% is used for address discovery time
                double estimatedRescanTime = totalFetchTime * RESCAN_PERCENTAGE;
                double estimatedDiscoveryTime = totalFetchTime * DISCOVERY_PERCENTAGE;
                double totalSyncTime = totalFetchTime + estimatedRescanTime + estimatedDiscoveryTime;

                walletData.syncRemainingTime = Math.round(remainingFetchTime + estimatedRescanTime + estimatedDiscoveryTime);
                walletData.syncProgress = ((float) elapsedFetchTime / totalSyncTime) * 100;
                walletData.syncVerbose = getString(R.string.sync_status_verbose, Utils.getTime(elapsedFetchTime), Utils.getTime(walletData.syncRemainingTime),
                        Utils.getTime(Math.round(totalSyncTime)), Utils.getTime(elapsedFetchTime), Utils.getTime(remainingFetchTime), Utils.getTime(Math.round(totalFetchTime)));

                setConnectionStatus(getString(R.string.fetching_headers));
                setBestBlockTime(lastHeaderTime);
                setChainStatus(getString(R.string.blocks_behind, walletData.syncEndPoint - count));

                if (currentFragment instanceof OverviewFragment) {
                    OverviewFragment overviewFragment = (OverviewFragment) currentFragment;

                    String daysBehind = Utils.calculateDays((System.currentTimeMillis() / 1000) - lastHeaderTime, MainActivity.this);
                    walletData.syncStatus = getString(R.string.fetched_header_format, count, walletData.syncEndPoint, Math.round(percent * 100), daysBehind);
                    overviewFragment.publishProgress();
                }

                syncProgressBar.post(new Runnable() {
                    @Override
                    public void run() {
                        syncProgressBar.setProgress((int) walletData.syncProgress);
                    }
                });

                if (walletData.initialSyncEstimate == -1) {
                    walletData.initialSyncEstimate = walletData.syncRemainingTime;
                }

                break;
            case Dcrlibwallet.FINISH:
                updatePeerCount();
                walletData.totalFetchTime = System.currentTimeMillis() - walletData.fetchHeaderTime;
                walletData.syncStartPoint = -1;
                walletData.syncEndPoint = -1;
                walletData.syncCurrentPoint = -1;
                break;
        }
    }

    @Override
    public void onFetchMissingCFilters(int missingCFiltersStart, int missingCFiltersEnd, String state) {
        // Not implemented
    }

    @Override
    public void onDiscoveredAddresses(String state) {
        setChainStatus(null);
        setBestBlockTime(-1);
        if (state.equals(Dcrlibwallet.START)) {
            accountDiscoveryProgress = new Thread() {
                public void run() {
                    try {
                        walletData.accountDiscoveryStartTime = System.currentTimeMillis();
                        long estimatedRescanTime = Math.round(walletData.totalFetchTime * RESCAN_PERCENTAGE);
                        long estimatedDiscoveryTime = Math.round(walletData.totalFetchTime * DISCOVERY_PERCENTAGE);

                        while (!interrupted()) {
                            double elapsedDiscoveryTime = System.currentTimeMillis() - walletData.accountDiscoveryStartTime;

                            double totalSyncTime;
                            if (elapsedDiscoveryTime > estimatedDiscoveryTime) {
                                totalSyncTime = walletData.totalFetchTime + elapsedDiscoveryTime + estimatedRescanTime;
                            } else {
                                totalSyncTime = walletData.totalFetchTime + estimatedDiscoveryTime + estimatedRescanTime;
                            }

                            double elapsedTime = walletData.totalFetchTime + elapsedDiscoveryTime;

                            long remainingAccountDiscoveryTime = Math.round(estimatedDiscoveryTime - elapsedDiscoveryTime);
                            if (remainingAccountDiscoveryTime < 0) {
                                remainingAccountDiscoveryTime = 0;
                            }

                            walletData.syncProgress = (elapsedTime / (float) totalSyncTime) * 100;
                            walletData.syncRemainingTime = (remainingAccountDiscoveryTime + estimatedRescanTime);
                            walletData.syncVerbose = getString(R.string.sync_status_verbose, Utils.getTime(Math.round(elapsedTime)), Utils.getTime(walletData.syncRemainingTime),
                                    Utils.getTime(Math.round(totalSyncTime)), Utils.getTime(Math.round(elapsedDiscoveryTime)),
                                    Utils.getTime(remainingAccountDiscoveryTime), Utils.getTime(estimatedDiscoveryTime));

                            setConnectionStatus(R.string.discovering_used_addresses);
                            setChainStatus(Utils.getSyncTimeRemaining(walletData.syncRemainingTime, (int) walletData.syncProgress, true, MainActivity.this));

                            if (currentFragment instanceof OverviewFragment) {
                                OverviewFragment overviewFragment = (OverviewFragment) currentFragment;
                                long discoveryProgress = Math.round((elapsedDiscoveryTime / estimatedDiscoveryTime) * 100);
                                if (discoveryProgress > 100) {
                                    walletData.syncStatus = getString(R.string.overview_discovering_used_addresses_over, discoveryProgress);
                                    overviewFragment.publishProgress();
                                } else {
                                    walletData.syncStatus = getString(R.string.overview_discovering_used_addresses, discoveryProgress);
                                    overviewFragment.publishProgress();
                                }
                            }

                            syncProgressBar.post(new Runnable() {
                                @Override
                                public void run() {
                                    syncProgressBar.setProgress((int) walletData.syncProgress);
                                }
                            });

                            sleep(1000);
                        }
                    } catch (InterruptedException ignored) {
                    }
                }
            };

            accountDiscoveryProgress.start();
        } else {
            if (accountDiscoveryProgress != null && accountDiscoveryProgress.isAlive()) {
                accountDiscoveryProgress.interrupt();
            }

            walletData.totalDiscoveryTime = (System.currentTimeMillis() - walletData.accountDiscoveryStartTime);
            long estimatedDiscoveryTime = Math.round((walletData.totalFetchTime * DISCOVERY_PERCENTAGE) / 1000);

            double percentageDifference = (walletData.totalDiscoveryTime / estimatedDiscoveryTime) - 1;
            percentageDifference *= 100;

            String log = String.format(Locale.getDefault(), "Discovery, assumed: %ds, actual: %ds (%s%.2f%%)",
                    estimatedDiscoveryTime, Math.round(walletData.totalDiscoveryTime / 1000), percentageDifference > 0 ? "+" : "", percentageDifference);
            Dcrlibwallet.log(log);

            updatePeerCount();
        }
    }

    @Override
    public void onRescan(int rescannedThrough, String state) {
        if (walletData.syncEndPoint == -1) {
            walletData.syncEndPoint = walletData.wallet.getBestBlock();
        }

        switch (state) {
            case Dcrlibwallet.START:
                setConnectionStatus(R.string.scanning_blocks);
                walletData.syncStartPoint = 0;
                walletData.syncCurrentPoint = 0;
                walletData.syncEndPoint = walletData.wallet.getBestBlock();
                walletData.rescanStartTime = System.currentTimeMillis();
                break;
            case Dcrlibwallet.PROGRESS:

                float scannedPercentage = ((float) rescannedThrough / walletData.syncEndPoint) * 100;

                long elapsedRescanTime = System.currentTimeMillis() - walletData.rescanStartTime;
                double totalScanTime = elapsedRescanTime / ((float) rescannedThrough / walletData.syncEndPoint);
                double totalSyncTime = walletData.totalFetchTime + walletData.totalDiscoveryTime + totalScanTime;
                double elapsedTime = walletData.totalFetchTime + walletData.totalDiscoveryTime + elapsedRescanTime;

                walletData.syncRemainingTime = Math.round(totalScanTime) - elapsedRescanTime;
                walletData.syncProgress = (elapsedTime / (float) totalSyncTime) * 100;
                walletData.syncVerbose = getString(R.string.sync_status_verbose, Utils.getTime(Math.round(elapsedTime)), Utils.getTime(walletData.syncRemainingTime),
                        Utils.getTime(Math.round(totalSyncTime)), Utils.getTime(Math.round(elapsedRescanTime)),
                        Utils.getTime(walletData.syncRemainingTime), Utils.getTime(Math.round(totalScanTime)));

                setConnectionStatus(R.string.scanning_blocks);
                setChainStatus(Utils.getSyncTimeRemaining(walletData.syncRemainingTime, (int) walletData.syncProgress, true, MainActivity.this));

                syncProgressBar.post(new Runnable() {
                    @Override
                    public void run() {
                        syncProgressBar.setProgress((int) walletData.syncProgress);
                    }
                });

                if (currentFragment instanceof OverviewFragment) {
                    OverviewFragment overviewFragment = (OverviewFragment) currentFragment;
                    walletData.syncStatus = getString(R.string.overview_rescan_height_format, rescannedThrough, walletData.syncEndPoint, Math.round(scannedPercentage));
                    overviewFragment.publishProgress();
                }

                break;
            default:
                double discoveryTime = walletData.totalDiscoveryTime;
                double estimatedDiscoveryTime = walletData.totalFetchTime * DISCOVERY_PERCENTAGE;
                double percentageDifference = (discoveryTime / estimatedDiscoveryTime) - 1;
                percentageDifference *= 100;

                String log = String.format(Locale.getDefault(), "Discovery, assumed: %ds, actual: %ds (%s%.2f%%)", Math.round(estimatedDiscoveryTime / 1000), Math.round(discoveryTime / 1000), percentageDifference > 0 ? "+" : "", percentageDifference);
                Dcrlibwallet.log(log);

                double rescanTime = (System.currentTimeMillis() - walletData.rescanStartTime);
                double estimatedScanTime = walletData.totalFetchTime * RESCAN_PERCENTAGE;

                double estimatePercent = (rescanTime / walletData.totalFetchTime) * 100;
                percentageDifference = (rescanTime / estimatedScanTime) - 1;
                percentageDifference *= 100;

                log = String.format(Locale.getDefault(), "Scan, assumed: 50%%(%ds), actual: %.2f%%(%ds) (%s%.2f%%)", Math.round(estimatedScanTime / 1000), estimatePercent, Math.round(rescanTime / 1000), percentageDifference > 0 ? "+" : "", percentageDifference);
                Dcrlibwallet.log(log);

                Dcrlibwallet.log("Sync, Initial Estimate: " + Math.round(walletData.initialSyncEstimate / 1000) + "s Actual: " + Math.round((rescanTime + walletData.totalFetchTime + discoveryTime) / 1000) + "s");

                double totalSync = walletData.totalFetchTime + discoveryTime + rescanTime;
                log = String.format(Locale.getDefault(), "Fetch: %.2f%%, Discovery: %.2f%%, Rescan %.2f%%", walletData.totalFetchTime / totalSync * 100, discoveryTime / totalSync * 100, rescanTime / totalSync * 100);
                Dcrlibwallet.log(log);

                syncProgressBar.post(new Runnable() {
                    @Override
                    public void run() {
                        syncProgressBar.setProgress(0);
                        syncProgressBar.setVisibility(View.GONE);
                    }
                });

                updatePeerCount();

                if(!walletData.syncing && walletData.wallet.isRescanning()){ // this is a rescan
                    // delete saved transactions and fetch transactions from wallet to add missing transactions
                    File overviewTxFile = Utils.getTransactionFile(this, OverviewFragment.transactionsFileName);
                    if(overviewTxFile.exists()){
                        overviewTxFile.delete();
                    }

                    File historyTxFile = Utils.getTransactionFile(this, HistoryFragment.transactionsFileName);
                    if(historyTxFile.exists()){
                        historyTxFile.delete();
                    }
                }
                break;
        }
    }

    private void setupSyncedLayout() {
        syncProgressBar.setProgress(0);
        syncProgressBar.setVisibility(View.GONE);

        ((AnimationDrawable) syncIndicator.getBackground()).stop();
        syncIndicator.setVisibility(View.GONE);

        bestBlock = walletData.wallet.getBestBlock();
        bestBlockTimestamp = walletData.wallet.getBestBlockTimeStamp();
        String status = String.format(Locale.getDefault(), "%s: %d", getString(R.string.latest_block), bestBlock);
        setChainStatus(status);
        setBestBlockTime(bestBlockTimestamp);

        displayBalance();
        totalBalance.setVisibility(View.VISIBLE);
    }

    @Override
    public void onSynced(boolean b) {
        walletData.synced = b;
        walletData.syncing = false;
        if (b) {
            walletData.syncStartPoint = -1;
            walletData.syncEndPoint = -1;
            walletData.syncCurrentPoint = -1;
            walletData.syncRemainingTime = -1;
            walletData.fetchHeaderTime = -1;
            walletData.syncStatus = null;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    connectionStatus.setBackgroundColor(getResources().getColor(R.color.greenLightTextColor));
                    updatePeerCount();
                    setupSyncedLayout();
                }
            });

            broadcastIntent = new Intent(Constants.SYNCED);
            if (isForeground) {
                sendBroadcast(broadcastIntent);
                broadcastIntent = null;
            }
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    connectionStatus.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));

                    String status = String.format(Locale.getDefault(), "%s: %d", getString(R.string.latest_block), walletData.wallet.getBestBlock());
                    setChainStatus(status);

                    updatePeerCount();

                    broadcastIntent = new Intent(Constants.SYNCED);
                    if (isForeground) {
                        sendBroadcast(broadcastIntent);
                        broadcastIntent = null;
                    }

                    ((AnimationDrawable) syncIndicator.getBackground()).stop();
                    syncIndicator.setVisibility(View.GONE);

                    displayBalance();
                    totalBalance.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    @Override
    public void onPeerConnected(int peerCount) {
        walletData.peers = peerCount;
        if (!walletData.syncing) updatePeerCount();
    }

    @Override
    public void onPeerDisconnected(int peerCount) {
        walletData.peers = peerCount;
        if (!walletData.syncing) updatePeerCount();
    }

    private void updatePeerCount() {
        if (!walletData.synced && !walletData.syncing) {
            setConnectionStatus(R.string.not_synced);
            return;
        }

        if (!walletData.syncing) {
            if (walletData.peers == 1) {
                setConnectionStatus(R.string.synced_with_one_peer);
            } else {
                setConnectionStatus(getString(R.string.synced_with_multiple_peer, walletData.peers));
            }
        } else {
            if (walletData.peers == 1) {
                setConnectionStatus(R.string.syncing_with_one_peer);
            } else {
                setConnectionStatus(getString(R.string.syncing_with_multiple_peer, walletData.peers));
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_connection_status:
                walletData.wallet.dropSpvConnection();
                Toast.makeText(this, R.string.re_establishing_connection, Toast.LENGTH_SHORT).show();
                checkWifiSync();
                break;
        }
    }
}