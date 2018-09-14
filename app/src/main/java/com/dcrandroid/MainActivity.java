package com.dcrandroid;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.*;
import android.os.*;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
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

import com.dcrandroid.activities.*;
import com.dcrandroid.adapter.NavigationListAdapter;
import com.dcrandroid.adapter.NavigationListAdapter.NavigationBarItem;
import com.dcrandroid.data.Account;
import com.dcrandroid.data.Constants;
import com.dcrandroid.fragments.*;
import com.dcrandroid.util.*;
import com.dcrandroid.util.TransactionsResponse.TransactionInput;
import com.dcrandroid.util.TransactionsResponse.TransactionOutput;
import com.dcrandroid.util.Utils;

import org.json.*;

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;
import java.util.*;

import mobilewallet.*;

public class MainActivity extends AppCompatActivity implements TransactionListener, BlockScanResponse,
        BlockNotificationError, SpvSyncResponse {

    private TextView chainStatus, bestBlockTime, connectionStatus, totalBalance;
    private ImageView rescanImage, stopScan;

    public int pageID, menuAdd = 0;
    public static MenuItem menuOpen;
    private Fragment fragment;
    private DcrConstants constants;
    private PreferenceUtil util;
    private NotificationManager notificationManager;
    private Animation animRotate;
    private MainApplication mainApplication;
    private SoundPool alertSound;
    private int bestBlock = 0, peerCount, blockNotificationSound;
    private long bestBlockTimestamp;
    private boolean scanning = false, synced = false;
    private Thread blockUpdate;
    private ArrayList<NavigationBarItem> items;
    private NavigationListAdapter listAdapter;

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        System.out.println("Memory Trim: "+level);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View view = getCurrentFocus();
        if (view != null && (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_MOVE) && view instanceof EditText && !view.getClass().getName().startsWith("android.webkit.")) {
            int scrcoords[] = new int[2];
            view.getLocationOnScreen(scrcoords);
            float x = ev.getRawX() + view.getLeft() - scrcoords[0];
            float y = ev.getRawY() + view.getTop() - scrcoords[1];
            if (x < view.getLeft() || x > view.getRight() || y < view.getTop() || y > view.getBottom())
                ((InputMethodManager)this.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow((this.getWindow().getDecorView().getApplicationWindowToken()), 0);
        }
        return super.dispatchTouchEvent(ev);
    }

    private void initViews(){
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        connectionStatus = findViewById(R.id.tv_connection_status);
        bestBlockTime = findViewById(R.id.best_block_time);
        chainStatus = findViewById(R.id.chain_status);
        rescanImage = findViewById(R.id.iv_rescan_blocks);
        stopScan = findViewById(R.id.iv_stop_rescan);
        totalBalance = findViewById(R.id.tv_total_balance);

        ListView mListView = findViewById(R.id.lv_nav);

        String[] itemTitles = getResources().getStringArray(R.array.nav_list_titles);
        int[] itemIcons = new int[]{R.mipmap.overview, R.mipmap.accounts, R.mipmap.send,
                R.mipmap.receive, R.mipmap.history, R.mipmap.settings, R.mipmap.help};
        items = new ArrayList<>();
        for(int i = 0; i < itemTitles.length; i++){
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

        if(itemTitles.length > 0){
            mListView.setItemChecked(0, true);
        }

        displayOverview();

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        animRotate = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.anim_rotate);
        animRotate.setRepeatCount(-1);

        stopScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanning = false;
            }
        });
    }

    private void restartApp(){
        PackageManager packageManager = getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(getPackageName());
        if (intent != null) {
            ComponentName componentName = intent.getComponent();
            Intent mainIntent = Intent.makeRestartActivityTask(componentName);
            startActivity(mainIntent);
            Runtime.getRuntime().exit(0);
        }
    }

    private void registerNotificationChannel(){
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

        mainApplication = (MainApplication) getApplicationContext();

        setContentView(R.layout.activity_main);
        
        initViews();

        registerNotificationChannel();
        
        util = new PreferenceUtil(this);
        constants = DcrConstants.getInstance();
        
        if(constants.wallet == null){
            System.out.println("Restarting app");
            restartApp();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            SoundPool.Builder builder = new SoundPool.Builder().setMaxStreams(3);
            AudioAttributes attributes = new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION).build();
            builder.setAudioAttributes(attributes);
            alertSound = builder.build();
        }else{
            alertSound = new SoundPool(3, AudioManager.STREAM_NOTIFICATION,0);
        }

        blockNotificationSound = alertSound.load(MainActivity.this, R.raw.beep, 1);

        constants.wallet.transactionNotification(this);

        displayBalance();

        connectToDecredNetwork();
    }

    private void displayBalance(){
        try {
            final ArrayList<com.dcrandroid.data.Account> accounts = Account.parse(constants.wallet.getAccounts(util.getBoolean(Constants.SPEND_UNCONFIRMED_FUNDS) ? 0 : Constants.REQUIRED_CONFIRMATIONS));
            long walletBalance = 0;
            for(int i = 0; i < accounts.size(); i++){
                walletBalance += accounts.get(i).getBalance().getTotal();
            }
            totalBalance.setText(CoinFormat.Companion.format(Utils.formatDecred(walletBalance) + " DCR"));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void connectToDecredNetwork(){
        Bundle b = getIntent().getExtras();
        String passPhrase = null;
        if (b != null){
            passPhrase = b.getString("passphrase");
        }
        final String finalPassPhrase = passPhrase;
        if (Integer.parseInt(util.get(Constants.NETWORK_MODES, "0")) == 0){
            System.out.println("Starting SPV Connection");
            setConnectionStatus("Not Synced");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        constants.wallet.spvSync(MainActivity.this, Utils.getPeerAddress(util), finalPassPhrase != null, finalPassPhrase == null ? null : finalPassPhrase.getBytes());
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }).start();
        }else{
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (finalPassPhrase != null){
                        try {
                            constants.wallet.unlockWallet(finalPassPhrase.getBytes());
                        } catch (Exception e) {
                            if (util.getBoolean(Constants.DEBUG_MESSAGES)) {
                                showText("Failed to unlock wallet" + e.getMessage());
                            }
                            e.printStackTrace();
                        }
                    }
                    connectToRPCServer();
                }
            }).start();
        }
    }

    private void startBlockUpdate(){
        if (blockUpdate != null) {
            return;
        }
        updatePeerCount();

        blockUpdate = new Thread(){
            public void run(){
                while(!this.isInterrupted()){
                    try {
                        if(!scanning && synced) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    long currentTime = System.currentTimeMillis() / 1000;
                                    long estimatedBlocks = ((currentTime - bestBlockTimestamp) / 120) + bestBlock;

                                    setBestBlockTime(bestBlockTimestamp);
                                    //6 Minutes
                                    if ((currentTime - bestBlockTimestamp) > 360) {
                                        String status = String.format(Locale.getDefault(), "Latest Block: %d of %d", bestBlock, estimatedBlocks);
                                        chainStatus.setText(status);
                                    }
                                }
                            });
                        }
                    }catch (Exception e){
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

    private void rescanBlocks(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int rescanHeight = util.getInt(PreferenceUtil.RESCAN_HEIGHT, 0);
                int blockHeight = constants.wallet.getBestBlock();
                if(rescanHeight < blockHeight){
                    constants.wallet.rescan(rescanHeight, MainActivity.this);
                    rescanImage.startAnimation(animRotate);
                    rescanImage.setEnabled(false);
                    chainStatus.setVisibility(View.GONE);
                    stopScan.setVisibility(View.VISIBLE);
                    scanning = true;
                }
            }
        });
    }

    private void setConnectionStatus(final String str){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectionStatus.setText(str);
            }
        });
    }

    private void setChainStatus(final String str){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                chainStatus.setText(str);
            }
        });
    }

    private void setBestBlockTime(final long seconds){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (seconds == -1){
                    bestBlockTime.setText(null);
                    return;
                }
                bestBlockTime.setText(Utils.calculateTime((System.currentTimeMillis() / 1000) - seconds));
            }
        });
    }

    private void showText(final String str){
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
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if(pageID == 0) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.exit_app_prompt_title)
                    .setMessage(R.string.exit_app_prompt_message)
                    .setNegativeButton(android.R.string.no, null)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface arg0, int arg1) {
                            MainActivity.super.onBackPressed();
                        }
                    }).create().show();
        }
        else {
            displayOverview();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(blockUpdate != null && !blockUpdate.isInterrupted()){
            blockUpdate.interrupt();
        }
        if(constants.wallet != null){
            constants.wallet.shutdown();
        }
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1 && resultCode == 0){
            if(fragment instanceof AccountsFragment){
                AccountsFragment accountsFragment = (AccountsFragment) fragment;
                accountsFragment.prepareAccountData();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_page, menu);
        super.onCreateOptionsMenu(menu);
        if(menuAdd != 1) {
            menuOpen = menu.findItem(R.id.action_add);
            menuOpen.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_add) {
            Intent intent = new Intent(this, AddAccountActivity.class);
            startActivityForResult(intent,1);
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean switchFragment(int position) {
        switch (position) {
            case 0:
                fragment = new OverviewFragment();
                break;
            case 1:
                fragment = new AccountsFragment();
                break;
            case 2:
                fragment = new SendFragment();
                break;
            case 3:
                fragment = new ReceiveFragment();
                break;
            case 4:
                fragment = new HistoryFragment();
                break;
            case 5:
                fragment = new SettingsActivity.MainPreferenceFragment();
                break;
            case 6:
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

    public void displayOverview(){
        switchFragment(0);
    }

    public void displayHistory(){
        switchFragment(4);
    }

    @Override
    public void onTransaction(String s) {
        System.out.println("Notification Received: "+s);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                displayBalance();
            }
        });
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
            for(int i = 0; i < debits.length(); i++){
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
            for(int i = 0; i < credits.length(); i++){
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

            if(fragment instanceof OverviewFragment){
                OverviewFragment overviewFragment = (OverviewFragment) fragment;
                overviewFragment.newTransaction(transaction);
            }else if(fragment instanceof HistoryFragment){
                HistoryFragment historyFragment = (HistoryFragment) fragment;
                historyFragment.newTransaction(transaction);
            }

            if(util.getBoolean(Constants.TRANSACTION_NOTIFICATION, true)) {
                double fee = obj.getDouble(Constants.FEE);
                if (fee == 0) {
                    BigDecimal satoshi = BigDecimal.valueOf(obj.getLong(Constants.AMOUNT));

                    BigDecimal amount = satoshi.divide(BigDecimal.valueOf(1e8), new MathContext(100));
                    String hash = obj.getString(Constants.HASH);
                    DecimalFormat format = new DecimalFormat("You received #.######## DCR");
                    sendNotification(format.format(amount), hash);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            if(util.getBoolean(Constants.DEBUG_MESSAGES)) {
                showText(e.getMessage());
            }
        }
    }

    @Override
    public void onTransactionConfirmed(String hash, int height) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                displayBalance();
            }
        });
        if(fragment instanceof OverviewFragment){
            OverviewFragment overviewFragment = (OverviewFragment) fragment;
            overviewFragment.transactionConfirmed(hash, height);
        }else if (fragment instanceof HistoryFragment){
            HistoryFragment historyFragment = (HistoryFragment) fragment;
            historyFragment.transactionConfirmed(hash, height);
        }
    }

    @Override
    public void onBlockAttached(int height, long timestamp) {
        this.bestBlock = height;
        this.bestBlockTimestamp = timestamp / 1000000000;
        if(util.getBoolean(Constants.NEW_BLOCK_NOTIFICATION, false)) {
            alertSound.play(blockNotificationSound, 1, 1, 1, 0, 1);
        }
        if(synced) {
            String status = String.format(Locale.getDefault(), "Latest Block: %d", bestBlock);
            setChainStatus(status);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    displayBalance();
                    connectionStatus.setBackgroundColor(Color.parseColor("#2DD8A3"));
                }
            });
            setBestBlockTime(bestBlockTimestamp);
        }
        if(fragment instanceof OverviewFragment){
            OverviewFragment overviewFragment = (OverviewFragment) fragment;
            overviewFragment.blockAttached(height);
        } else if(fragment instanceof HistoryFragment){
            HistoryFragment historyFragment = (HistoryFragment) fragment;
            historyFragment.blockAttached(height);
        }
    }

    private void sendNotification(String amount, String hash){
        Intent launchIntent = new Intent(this,MainActivity.class);
        PendingIntent launchPendingIntent = PendingIntent.getActivity(this, 1, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder(this, "new transaction")
                .setContentTitle("New Transaction")
                .setContentText(amount)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setOngoing(false)
                .setAutoCancel(true)
                .setGroup(Constants.TRANSACTION_NOTIFICATION_GROUP)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(launchPendingIntent)
                .build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            System.out.println("Group: "+notification.getGroup());
        }
        Notification groupSummary = new NotificationCompat.Builder(this, "new transaction")
                .setContentTitle("New Transaction")
                .setContentText("You have some new transactions")
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setGroup(Constants.TRANSACTION_NOTIFICATION_GROUP)
                .setGroupSummary(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();
        notificationManager.notify(new Random().nextInt(), notification);
        notificationManager.notify(Constants.TRANSACTION_SUMMARY_ID, groupSummary);
    }

    @Override
    public void onEnd(int i, boolean b) {
        System.out.println("Done: "+i+"/"+constants.wallet.getBestBlock());

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                displayBalance();
            }
        });

        if(fragment instanceof OverviewFragment){
            OverviewFragment overviewFragment = (OverviewFragment) fragment;
            overviewFragment.prepareHistoryData();
        }else if(fragment instanceof HistoryFragment){
            HistoryFragment historyFragment = (HistoryFragment) fragment;
            historyFragment.prepareHistoryData();
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //rescanHeight.setText("");
                animRotate.cancel();
                animRotate.reset();
                rescanImage.setEnabled(true);
                chainStatus.setVisibility(View.VISIBLE);
                stopScan.setVisibility(View.GONE);
                scanning = false;
            }
        });
    }

    @Override
    public void onError(int i, String s) {
        System.out.println("Block scan error: "+s);

        if(fragment instanceof OverviewFragment){
            OverviewFragment overviewFragment = (OverviewFragment) fragment;
            overviewFragment.prepareHistoryData();
        }else if(fragment instanceof HistoryFragment){
            HistoryFragment historyFragment = (HistoryFragment) fragment;
            historyFragment.prepareHistoryData();
        }

        if(util.getBoolean(Constants.DEBUG_MESSAGES)) {
            showText(s);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                animRotate.cancel();
                animRotate.reset();
                rescanImage.setEnabled(true);
                chainStatus.setVisibility(View.VISIBLE);
                stopScan.setVisibility(View.GONE);
                scanning = false;
            }
        });
    }

    @Override
    public boolean onScan(final int i) {
        if(util.getInt(PreferenceUtil.RESCAN_HEIGHT, 0) < i){
            util.setInt(PreferenceUtil.RESCAN_HEIGHT, i);
        }
        System.out.println("Scanning: "+i+"/"+constants.wallet.getBestBlock());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int bestBlock = constants.wallet.getBestBlock();
                int scannedPercentage = (i/bestBlock) * 100;
                String status = String.format(Locale.getDefault(), "Latest Block: %d(%d%%)", constants.wallet.getBestBlock(), scannedPercentage);
                chainStatus.setText(status);
            }
        });
        return scanning;
    }

    @Override
    public void onBlockNotificationError(Exception e) {
        System.out.println("Error received: "+e.getMessage());
        showText(e.getMessage());
        setConnectionStatus("Disconnected");
        connectToRPCServer();
    }

    private void connectToRPCServer(){
        Thread connectionThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    setConnectionStatus("Connecting to RPC Server");
                    String dcrdAddress = Utils.getNetworkAddress(MainActivity.this, mainApplication);
                    int i = 0;
                    for (; ; ) {
                        try {
                            if (util.getBoolean(Constants.DEBUG_MESSAGES)) {
                                showText("Connecting attempt " + (++i));
                            }
                            constants.wallet.startRPCClient(dcrdAddress, "dcrwallet", "dcrwallet", Utils.getRemoteCertificate(MainActivity.this).getBytes());
                            break;
                        } catch (Exception e) {
                            e.printStackTrace();
                            if (util.getBoolean(Constants.DEBUG_MESSAGES)) {
                                showText("RPC Connection Failed: " + e.getMessage());
                            }
                        }
                        Thread.sleep(2500);
                    }
                    if (util.getBoolean(Constants.DEBUG_MESSAGES)) {
                        showText("Subscribe to block notification");
                    }
                    constants.wallet.subscribeToBlockNotifications(MainActivity.this);
                    setConnectionStatus(getString(R.string.discovering_used_addresses));
                    if (util.getBoolean(Constants.DEBUG_MESSAGES)) {
                        showText("Discover addresses");
                    }
                    constants.wallet.discoverActiveAddresses();
                    constants.wallet.loadActiveDataFilters();
                    setConnectionStatus(getString(R.string.fetching_headers));
                    if (util.getBoolean(Constants.DEBUG_MESSAGES)) {
                        showText("Fetching Headers");
                    }
                    long rescanHeight = constants.wallet.fetchHeaders();
                    if (rescanHeight != -1) {
                        util.setInt(PreferenceUtil.RESCAN_HEIGHT, (int) rescanHeight);
                    }
                    setConnectionStatus(getString(R.string.publish_unmined_transaction));
                    constants.wallet.publishUnminedTransactions();
                    setConnectionStatus("Connected To Remote Node");
                    rescanBlocks();
                    synced = true;
                    startBlockUpdate();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
        connectionThread.setDaemon(true);
        connectionThread.start();
    }

    @Override
    public void onSyncError(long l, final Exception e) {
        e.printStackTrace();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Sync Error: "+ e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onSynced(boolean b) {
        synced = b;
        if (b){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    displayBalance();
                    connectionStatus.setBackgroundColor(Color.parseColor("#2DD8A3"));
                }
            });
            bestBlock = constants.wallet.getBestBlock();
            bestBlockTimestamp = constants.wallet.getBestBlockTimeStamp();
            String status = String.format(Locale.getDefault(), "Latest Block: %d", constants.wallet.getBestBlock());
            setChainStatus(status);
            setBestBlockTime(bestBlockTimestamp);

            System.out.println("SYNCEEED: "+fragment);

            if(fragment instanceof OverviewFragment){
                OverviewFragment overviewFragment = (OverviewFragment) fragment;
                overviewFragment.prepareHistoryData();
            }else if(fragment instanceof HistoryFragment){
                System.out.println("Calling history fragment");
                HistoryFragment historyFragment = (HistoryFragment) fragment;
                historyFragment.prepareHistoryData();
            }

            startBlockUpdate();
        }
    }

    @Override
    public void onFetchedHeaders(int fetchedHeadersCount, long lastHeaderTime, boolean finished) {
        if(finished){
            updatePeerCount();
            return;
        }
        setConnectionStatus(getString(R.string.fetching_headers));
        String status = String.format(Locale.getDefault() , "Fetched %d Headers", fetchedHeadersCount);
        //Nanoseconds to seconds
        setBestBlockTime(lastHeaderTime);
        setChainStatus(status);
    }

    @Override
    public void onFetchMissingCFilters(int missingCFiltersStart, int missingCFiltersEnd, boolean finished) {
        if(finished){
            updatePeerCount();
            return;
        }
        setConnectionStatus("Fetching Missing CFilters");
        System.out.println("CFilters start: "+missingCFiltersStart + " CFilters end: "+ missingCFiltersEnd);
        String status = String.format(Locale.getDefault() , "Fetched %d CFilters", missingCFiltersEnd);
        setChainStatus(status);
    }

    @Override
    public void onDiscoveredAddresses(boolean finished) {
        setChainStatus(null);
        setBestBlockTime(-1);
        if (!finished) {
            setConnectionStatus(getString(R.string.discovering_used_addresses));
            return;
        }
        updatePeerCount();
    }

    @Override
    public void onRescanProgress(int rescannedThrough, boolean finished) {
        if(finished){
            updatePeerCount();
            return;
        }
        setConnectionStatus("Rescanning in progress...");
        int bestBlock = constants.wallet.getBestBlock();
        int scannedPercentage = Math.round(((float) rescannedThrough/bestBlock) * 100);
        String status = String.format(Locale.getDefault(), "Latest Block: %d(%d%%)", constants.wallet.getBestBlock(), scannedPercentage);
        float ff = (float)rescannedThrough/bestBlock;
        ff *= 100;
        System.out.println(status +" "+ ff);
        setChainStatus(status);
    }

    @Override
    public void onPeerConnected(int peerCount) {
        this.peerCount = peerCount;
        if(synced)
        updatePeerCount();
    }

    @Override
    public void onPeerDisconnected(int peerCount) {
        this.peerCount = peerCount;
        if(synced)
        updatePeerCount();
    }

    private void updatePeerCount(){
        setConnectionStatus((synced ? "Synced" : "Syncing") + " with " + peerCount + " " + (peerCount == 1 ? "peer" : "peers"));
    }
}