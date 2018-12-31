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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dcrandroid.activities.SettingsActivity;
import com.dcrandroid.adapter.NavigationListAdapter;
import com.dcrandroid.adapter.NavigationListAdapter.NavigationBarItem;
import com.dcrandroid.data.Account;
import com.dcrandroid.data.Constants;
import com.dcrandroid.fragments.AccountsFragment;
import com.dcrandroid.fragments.HelpFragment;
import com.dcrandroid.fragments.HistoryFragment;
import com.dcrandroid.fragments.OverviewFragment;
import com.dcrandroid.fragments.ReceiveFragment;
import com.dcrandroid.fragments.SecurityFragment;
import com.dcrandroid.fragments.SendFragment;
import com.dcrandroid.service.SyncService;
import com.dcrandroid.util.CoinFormat;
import com.dcrandroid.util.DcrConstants;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.TransactionsResponse;
import com.dcrandroid.util.TransactionsResponse.TransactionInput;
import com.dcrandroid.util.TransactionsResponse.TransactionOutput;
import com.dcrandroid.util.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;

import mobilewallet.Mobilewallet;
import mobilewallet.SpvSyncResponse;
import mobilewallet.TransactionListener;

public class MainActivity extends AppCompatActivity implements TransactionListener,
        SpvSyncResponse, View.OnClickListener {

    public int pageID;
    private TextView chainStatus, bestBlockTime, connectionStatus, totalBalance;
    private ImageView rescanImage, stopScan, syncIndicator;
    private Fragment fragment;
    private DcrConstants constants;
    private PreferenceUtil util;
    private NotificationManager notificationManager;
    private Animation rotateAnimation;
    private SoundPool alertSound;
    private int bestBlock = 0, peerCount, blockNotificationSound;
    private long bestBlockTimestamp;
    private boolean scanning = false, isForeground;
    private Thread blockUpdate;
    private ArrayList<NavigationBarItem> items;
    private NavigationListAdapter listAdapter;
    private ListView mListView;
    private SendFragment sendFragment = new SendFragment();
    private SecurityFragment securityFragment = new SecurityFragment();

    private Handler handler = new Handler();
    private Intent broadcastIntent = null;

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
        rescanImage = findViewById(R.id.iv_rescan_blocks);
        stopScan = findViewById(R.id.iv_stop_rescan);
        totalBalance = findViewById(R.id.tv_total_balance);
        syncIndicator = findViewById(R.id.iv_sync_indicator);

        if (BuildConfig.IS_TESTNET) {
            findViewById(R.id.tv_testnet).setVisibility(View.VISIBLE);
        }

        mListView = findViewById(R.id.lv_nav);

        String[] itemTitles = getResources().getStringArray(R.array.nav_list_titles);
        int[] itemIcons = new int[]{R.drawable.overview, R.drawable.history, R.mipmap.send,
                R.mipmap.receive, R.drawable.account, R.drawable.security, R.drawable.settings, R.drawable.help};
        items = new ArrayList<>();
        for (int i = 0; i < itemTitles.length; i++) {
            NavigationBarItem item = new NavigationBarItem(itemTitles[i], itemIcons[i]);
            items.add(item);
        }

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switchFragment(position);
            }
        });

        mListView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);

        listAdapter = new NavigationListAdapter(this, items);

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
        connectionStatus.setOnClickListener(this);
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
        constants = DcrConstants.getInstance();

        if (constants.wallet == null) {
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

        constants.wallet.transactionNotification(this);

        displayBalance();

        constants.wallet.addSyncResponse(this);

        connectToDecredNetwork();
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

        if(constants.peers == 0 && constants.synced){
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
            final ArrayList<com.dcrandroid.data.Account> accounts = Account.parse(constants.wallet.getAccounts(util.getBoolean(Constants.SPEND_UNCONFIRMED_FUNDS) ? 0 : Constants.REQUIRED_CONFIRMATIONS));
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

    private void connectToDecredNetwork() {
        if (Integer.parseInt(util.get(Constants.NETWORK_MODES, "0")) == 0) {
            setConnectionStatus(getString(R.string.connecting_to_peers));
        } else {
            setConnectionStatus(getString(R.string.connecting_to_rpc_server));
        }
        Intent syncIntent = new Intent(this, SyncService.class);
        startService(syncIntent);
    }

    private void startBlockUpdate() {
        if (blockUpdate != null) {
            return;
        }
        updatePeerCount();

        blockUpdate = new Thread() {
            public void run() {
                while (!this.isInterrupted()) {
                    try {
                        if (!scanning && constants.synced) {
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
                bestBlockTime.setText(Utils.calculateTime((System.currentTimeMillis() / 1000) - seconds, MainActivity.this));
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

        if (constants.wallet != null) {
            constants.wallet.shutdown();
        }
        finish();
    }

    public boolean switchFragment(int position) {
        switch (position) {
            case 0:
                fragment = new OverviewFragment();
                break;
            case 1:
                fragment = new HistoryFragment();
                break;
            case 2:
                fragment = sendFragment;
                break;
            case 3:
                fragment = new ReceiveFragment();
                break;
            case 4:
                fragment = new AccountsFragment();
                break;
            case 5:
                fragment = securityFragment;
                break;
            case 6:
                fragment = new SettingsActivity.MainPreferenceFragment();
                break;
            case 7:
                fragment = new HelpFragment();
                break;
            default:
                return false;
        }

        pageID = position;

        //Change the fragment
        getSupportFragmentManager().beginTransaction().replace(R.id.frame, fragment).commit();

        //Close Navigation Drawer
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        return true;
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

            if (fragment instanceof OverviewFragment) {
                OverviewFragment overviewFragment = (OverviewFragment) fragment;
                overviewFragment.newTransaction(transaction);
            } else if (fragment instanceof HistoryFragment) {
                HistoryFragment historyFragment = (HistoryFragment) fragment;
                historyFragment.newTransaction(transaction);
            }

            if (util.getBoolean(Constants.TRANSACTION_NOTIFICATION, true)) {
                double fee = obj.getDouble(Constants.FEE);
                if (fee == 0) {
                    BigDecimal satoshi = BigDecimal.valueOf(obj.getLong(Constants.AMOUNT));

                    BigDecimal amount = satoshi.divide(BigDecimal.valueOf(1e8), new MathContext(100));
                    DecimalFormat format = new DecimalFormat(getString(R.string.you_received) + " #.######## DCR");
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
                if (fragment instanceof OverviewFragment) {
                    OverviewFragment overviewFragment = (OverviewFragment) fragment;
                    overviewFragment.transactionConfirmed(hash, height);
                } else if (fragment instanceof HistoryFragment) {
                    HistoryFragment historyFragment = (HistoryFragment) fragment;
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
        if (constants.synced) {
            String status = String.format(Locale.getDefault(), "%s: %d", getString(R.string.latest_block), bestBlock);
            setChainStatus(status);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    displayBalance();
                    connectionStatus.setBackgroundColor(getResources().getColor(R.color.greenLightTextColor));
                }
            });
            setBestBlockTime(bestBlockTimestamp);
        }
        if (fragment instanceof OverviewFragment) {
            OverviewFragment overviewFragment = (OverviewFragment) fragment;
            overviewFragment.blockAttached(height);
        } else if (fragment instanceof HistoryFragment) {
            HistoryFragment historyFragment = (HistoryFragment) fragment;
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

        //Sum ascii to prevent duplicate notifications
        notificationManager.notify(nonce, notification);
        notificationManager.notify(Constants.TRANSACTION_SUMMARY_ID, groupSummary);
    }

    @Override
    public void onSyncError(long l, final Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onSynced(boolean b) {
        constants.synced = b;
        if (b) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    displayBalance();
                    ((AnimationDrawable) syncIndicator.getBackground()).stop();
                    syncIndicator.setVisibility(View.GONE);
                    totalBalance.setVisibility(View.VISIBLE);
                    connectionStatus.setBackgroundColor(getResources().getColor(R.color.greenLightTextColor));
                    updatePeerCount();

                    bestBlock = constants.wallet.getBestBlock();
                    bestBlockTimestamp = constants.wallet.getBestBlockTimeStamp();
                    String status = String.format(Locale.getDefault(), "%s: %d", getString(R.string.latest_block), constants.wallet.getBestBlock());
                    setChainStatus(status);
                    setBestBlockTime(bestBlockTimestamp);
                }
            });

            broadcastIntent = new Intent(Constants.SYNCED);

            if (isForeground) {
                sendBroadcast(broadcastIntent);
                broadcastIntent = null;
            }

            startBlockUpdate();
        }
    }

    @Override
    public void onFetchedHeaders(int fetchedHeadersCount, long lastHeaderTime, String state) {
        if (state.equals(Mobilewallet.START)) {
            setConnectionStatus(getString(R.string.fetching_headers));
        } else if (state.equals(Mobilewallet.PROGRESS)) {
            setConnectionStatus(getString(R.string.fetching_headers));
            long currentTime = System.currentTimeMillis() / 1000;
            long estimatedBlocks = ((currentTime - bestBlockTimestamp) / BuildConfig.TargetTimePerBlock) + constants.wallet.getBestBlock();
            float fetchedPercentage = (float) constants.wallet.getBestBlock() / estimatedBlocks * 100;
            fetchedPercentage = fetchedPercentage > 100 ? 100 : fetchedPercentage;
            String status = String.format(Locale.getDefault(), "%.1f%% %s", fetchedPercentage, getString(R.string.fetched));
            //Nanoseconds to seconds
            setBestBlockTime(lastHeaderTime);
            setChainStatus(status);
        } else if (state.equals(Mobilewallet.FINISH)) {
            updatePeerCount();
        }
    }

    @Override
    public void onFetchMissingCFilters(int missingCFiltersStart, int missingCFiltersEnd, String state) {
        if (state.equals(Mobilewallet.START)) {
            setConnectionStatus(getString(R.string.fetching_missing_cfilters));
        } else if (state.equals(Mobilewallet.PROGRESS)) {
            setConnectionStatus(getString(R.string.fetching_missing_cfilters));
            System.out.println("CFilters start: " + missingCFiltersStart + " CFilters end: " + missingCFiltersEnd);
            String status = String.format(Locale.getDefault(), "%s %d %s", getString(R.string.fetched), missingCFiltersEnd, getString(R.string.cfilters));
            setChainStatus(status);
        }
    }

    @Override
    public void onDiscoveredAddresses(String state) {
        setChainStatus(null);
        setBestBlockTime(-1);
        if (state.equals(Mobilewallet.START)) {
            setConnectionStatus(getString(R.string.discovering_used_addresses));
            return;
        }
        updatePeerCount();
    }

    @Override
    public void onRescan(int rescannedThrough, String state) {
        if (state.equals(Mobilewallet.START)) {
            setConnectionStatus(getString(R.string.rescanning_in_progress));
        } else if (state.equals(Mobilewallet.PROGRESS)) {
            setConnectionStatus(getString(R.string.rescanning_in_progress));
            int bestBlock = constants.wallet.getBestBlock();
            int scannedPercentage = Math.round(((float) rescannedThrough / bestBlock) * 100);
            String status = String.format(Locale.getDefault(), "%s: %d(%d%%)", getString(R.string.latest_block), constants.wallet.getBestBlock(), scannedPercentage);
            setChainStatus(status);
        } else {
            updatePeerCount();
        }
    }

    @Override
    public void onPeerConnected(int peerCount) {
        this.peerCount = peerCount;
        constants.peers = peerCount;
        if (constants.synced) updatePeerCount();
    }

    @Override
    public void onPeerDisconnected(int peerCount) {
        this.peerCount = peerCount;
        constants.peers = peerCount;
        if (constants.synced) updatePeerCount();
    }

    private void updatePeerCount() {
        setConnectionStatus((constants.synced ? getString(R.string.synced) : getString(R.string.syncing)) + " " + getString(R.string.with) + " " + peerCount + " " + (peerCount == 1 ? getString(R.string.peer) : getString(R.string.peers)));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_connection_status:
                Toast.makeText(this, R.string.re_establishing_connection, Toast.LENGTH_SHORT).show();
                setConnectionStatus(getString(R.string.connecting_to_peers));
                connectionStatus.setBackgroundColor(getResources().getColor(R.color.lightOrangeBackgroundColor));
                constants.synced = false;
                constants.wallet.dropSpvConnection();
                sendBroadcast(new Intent(Constants.SYNCED));

                syncIndicator.setVisibility(View.VISIBLE);
                totalBalance.setVisibility(View.GONE);
                syncIndicator.post(new Runnable() {
                    @Override
                    public void run() {
                        AnimationDrawable syncAnimation = (AnimationDrawable) syncIndicator.getBackground();
                        syncAnimation.start();
                    }
                });
                connectToDecredNetwork();
                break;
        }
    }
}