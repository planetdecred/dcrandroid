package com.dcrandroid;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.design.widget.NavigationView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dcrandroid.activities.AddAccountActivity;
import com.dcrandroid.activities.SettingsActivity;
import com.dcrandroid.data.Constants;
import com.dcrandroid.fragments.AccountsFragment;
import com.dcrandroid.fragments.HelpFragment;
import com.dcrandroid.fragments.HistoryFragment;
import com.dcrandroid.fragments.OverviewFragment;
import com.dcrandroid.fragments.ReceiveFragment;
import com.dcrandroid.fragments.SendFragment;
import com.dcrandroid.util.AccountResponse;
import com.dcrandroid.util.BlockNotificationProxy;
import com.dcrandroid.util.DcrConstants;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

import mobilewallet.BlockScanResponse;
import mobilewallet.LibWallet;
import mobilewallet.Mobilewallet;
import mobilewallet.ProcessListener;
import mobilewallet.TransactionListener;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, TransactionListener, BlockScanResponse, Animation.AnimationListener, BlockNotificationProxy, ProcessListener {

    public int pageID, menuAdd = 0;
    public static MenuItem menuOpen;
    private Fragment fragment;
    private NavigationView navigationView;
    private DcrConstants constants;
    private PreferenceUtil util;
    private NotificationManager notificationManager;
    private TextView rescanHeight, chainStatus, connectionStatus, bestBlockHeight, latestBlock;
    private Animation animRotate;
    private ImageView rescanImage, stopScan;
    private MainApplication mainApplication;
    private SoundPool alertSound;
    private int lastBestBlock = 0;
    private boolean scanning = false, isProcessRunning = false, addressDiscovery = false;
    private Thread blockUpdate;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("new transaction", getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setImportance(NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }
        mainApplication = (MainApplication) getApplicationContext();
        util = new PreferenceUtil(this);
        constants = DcrConstants.getInstance();
        if(constants.wallet == null){
            PackageManager packageManager = getPackageManager();
            Intent intent = packageManager.getLaunchIntentForPackage(getPackageName());
            ComponentName componentName = intent.getComponent();
            Intent mainIntent = Intent.makeRestartActivityTask(componentName);
            startActivity(mainIntent);
            Runtime.getRuntime().exit(0);
        }
        constants.wallet.transactionNotification(this);
        constants.notificationProxy = this;
        rescanHeight = findViewById(R.id.rescan_height);
        latestBlock = findViewById(R.id.tv_latest_block);
        connectionStatus = findViewById(R.id.tv_connection_status);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //add this line to display menu1 when the activity is loaded
        displaySelectedScreen(R.id.nav_overview);

        alertSound = new SoundPool(3, AudioManager.STREAM_NOTIFICATION,0);
        final int soundId = alertSound.load(MainActivity.this, R.raw.beep, 1);

        //final TextView bestBlockHeight = findViewById(R.id.best_block_height);
        bestBlockHeight = findViewById(R.id.best_block_height);
        chainStatus = findViewById(R.id.chain_status);
        rescanImage = findViewById(R.id.iv_rescan_blocks);
        stopScan = findViewById(R.id.iv_stop_rescan);
        animRotate = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.anim_rotate);
        animRotate.setRepeatCount(-1);
        animRotate.setAnimationListener(this);
        if(!constants.wallet.isNetBackendNil()){
            if(util.getBoolean(Constants.KEY_DEBUG_MESSAGES)) {
                showText("Already connected to RPC server");
            }
            setConnectionStatus("Connected");
            constants.wallet.startSPVConnection(Utils.getPeerAddress(util));
            constants.wallet.processNotification(MainActivity.this);
            if (Integer.parseInt(util.get(Constants.KEY_NETWORK_MODES, "0")) != 0) {
                rescanBlocks();
            }
        }else{
            if(util.getBoolean(Constants.KEY_DEBUG_MESSAGES)) {
                showText("Connecting to RPC server");
            }
            connectToRPCServer();
        }

        blockUpdate = new Thread(){
            public void run(){
                while(!this.isInterrupted()){
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                int bestBlock = constants.wallet.getBestBlock();
                                if (scanning) {
                                    bestBlockHeight.setText(String.valueOf(bestBlock));
                                    return;
                                }
                                long lastBlockTime = constants.wallet.getBestBlockTimeStamp();
                                long currentTime = System.currentTimeMillis() / 1000;
                                long estimatedBlocks = ((currentTime - lastBlockTime) / 120) + bestBlock;
                                if (estimatedBlocks > bestBlock) {
                                    bestBlockHeight.setText(bestBlock + " of " + estimatedBlocks);
                                    chainStatus.setText("");
                                } else {
                                    bestBlockHeight.setText(String.valueOf(bestBlock));
                                    chainStatus.setText(Utils.calculateTime((System.currentTimeMillis() / 1000) - lastBlockTime));
                                    if ((lastBestBlock == 0 || lastBestBlock != bestBlock) && util.getBoolean(Constants.NEW_BLOCK_NOTIFICATION, false)) {
                                        alertSound.play(soundId, 1, 1, 1, 0, 1);
                                    }
                                    lastBestBlock = bestBlock;
                                }
                            }
                        });
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            }
        };
        blockUpdate.start();
        stopScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanning = false;
            }
        });
    }

    private long estimateBlocks(){
        return (((System.currentTimeMillis() / 1000) - constants.wallet.getBestBlockTimeStamp()) / 120) + constants.wallet.getBestBlock();
    }

    private void rescanBlocks(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int rescanHeight = util.getInt(PreferenceUtil.RESCAN_HEIGHT);
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
        } else if(pageID == R.id.nav_overview) {
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
            displaySelectedScreen(R.id.nav_overview);
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
        ActivityCompat.finishAffinity(MainActivity.this);
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

    public void displaySelectedScreen(int itemId) {
        //initializing the fragment object which is selected
        pageID = itemId;
        switch (itemId) {
            case R.id.nav_overview:
                fragment = new OverviewFragment();
                break;
            case R.id.nav_accounts:
                fragment = new AccountsFragment();
                break;
            case R.id.nav_send:
                fragment = new SendFragment();
                break;
            case R.id.nav_receive:
                fragment = new ReceiveFragment();
                break;
            case R.id.nav_history:
                fragment = new HistoryFragment();
                break;
           /* case R.id.nav_tickets:
                fragment = new TicketsFragment();
                break;*/
            case R.id.nav_help:
                fragment = new HelpFragment();
                break;
            case R.id.nav_settings:
                fragment = new SettingsActivity.MainPreferenceFragment();
                break;
        }
        //replacing the fragment
        if (fragment != null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.frame, fragment);
            ft.commit();
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        displaySelectedScreen(item.getItemId());
        return true;
    }

    @Override
    public void onTransaction(String s) {
        System.out.println("Notification Received: "+s);
        try {
            JSONObject obj = new JSONObject(s);

            Intent newTransactionIntent = new Intent(Constants.ACTION_NEW_TRANSACTION)
                    .putExtra(Constants.EXTRA_TRANSACTION_TIMESTAMP,obj.getLong(Constants.TIMESTAMP))
                    .putExtra(Constants.EXTRA_TRANSACTION_FEE, obj.getLong(Constants.EXTRA_TRANSACTION_FEE))
                    .putExtra(Constants.EXTRA_TRANSACTION_TYPE, obj.getString(Constants.TYPE))
                    .putExtra(Constants.EXTRA_TRANSACTION_HASH, obj.getString(Constants.EXTRA_TRANSACTION_HASH))
                    .putExtra(Constants.EXTRA_BLOCK_HEIGHT, obj.getInt("Height"))
                    .putExtra(Constants.EXTRA_AMOUNT, obj.getLong(Constants.EXTRA_AMOUNT))
                    .putExtra(Constants.EXTRA_TRANSACTION_DIRECTION, obj.getInt("Direction"));
            long totalInput = 0, totalOutput = 0;
            ArrayList<String> usedInput = new ArrayList<>();
            JSONArray debits = obj.getJSONArray("Debits");
            for(int i = 0; i < debits.length(); i++){
                JSONObject debit = debits.getJSONObject(i);
                totalInput += debit.getLong("PreviousAmount");
                usedInput.add(debit.getString("AccountName") + "\n" + Utils.formatDecred(debit.getLong("PreviousAmount") / AccountResponse.SATOSHI));
            }
            ArrayList<String> walletOutput = new ArrayList<>();
            JSONArray credits = obj.getJSONArray("Credits");
            for(int i = 0; i < credits.length(); i++){
                JSONObject credit = credits.getJSONObject(i);
                totalOutput += credit.getLong("Amount");
                walletOutput.add(credit.getString("Address") + "\n" + Utils.formatDecred(credit.getLong("Amount") / AccountResponse.SATOSHI));
            }
            newTransactionIntent.putExtra(Constants.EXTRA_TRANSACTION_TOTAL_INPUT, totalInput)
                    .putExtra(Constants.EXTRA_TRANSACTION_INPUTS, usedInput)
                    .putExtra(Constants.EXTRA_TRANSACTION_TOTAL_OUTPUT, totalOutput)
                    .putExtra(Constants.EXTRA_TRANSACTION_OUTPUTS, walletOutput);
            sendBroadcast(newTransactionIntent);
        if(util.getBoolean(Constants.KEY_TRANSACTION_NOTIFICATION, true)) {
            double fee = obj.getDouble(Constants.EXTRA_TRANSACTION_FEE);
            if (fee == 0) {
                BigDecimal satoshi = BigDecimal.valueOf(obj.getLong(Constants.EXTRA_AMOUNT));

                BigDecimal amount = satoshi.divide(BigDecimal.valueOf(1e8), new MathContext(100));
                String hash = obj.getString(Constants.EXTRA_TRANSACTION_HASH);
                DecimalFormat format = new DecimalFormat("You received #.######## DCR");
                sendNotification(format.format(amount), hash);
            }
        }
        } catch (JSONException e) {
            e.printStackTrace();
            if(util.getBoolean(Constants.KEY_DEBUG_MESSAGES)) {
                showText(e.getMessage());
            }
        }
    }

    @Override
    public void onTransactionConfirmed(String hash, int height){
        Intent confirmedTransactionIntent = new Intent(Constants.ACTION_TRANSACTION_CONFRIMED)
                .putExtra(Constants.EXTRA_TRANSACTION_HASH, hash)
                .putExtra(Constants.EXTRA_BLOCK_HEIGHT, height);
        sendBroadcast(confirmedTransactionIntent);
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
        sendBroadcast(new Intent(Constants.ACTION_BLOCK_SCAN_COMPLETE));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                rescanHeight.setText("");
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
        sendBroadcast(new Intent(Constants.ACTION_BLOCK_SCAN_COMPLETE));
        if(util.getBoolean(Constants.KEY_DEBUG_MESSAGES)) {
            showText(s);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                rescanHeight.setText("");
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
        if(util.getInt(PreferenceUtil.RESCAN_HEIGHT) < i){
            util.setInt(PreferenceUtil.RESCAN_HEIGHT, i);
        }
        System.out.println("Scanning: "+i+"/"+constants.wallet.getBestBlock());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                rescanHeight.setText(i+"/");
            }
        });
        return scanning;
    }

    @Override
    public void onAnimationStart(Animation animation) {

    }

    @Override
    public void onAnimationEnd(Animation animation) {

    }

    @Override
    public void onAnimationRepeat(Animation animation) {

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
                    if (mainApplication.getNetworkMode() != 0) {
                        int i = 0;
                        for (; ; ) {
                            try {
                                if(util.getBoolean(Constants.KEY_DEBUG_MESSAGES)) {
                                    showText("Connecting attempt " + (++i));
                                }
                                constants.wallet.startRPCClient(dcrdAddress, "dcrwallet", "dcrwallet", Utils.getRemoteCertificate(MainActivity.this).getBytes());
                                    break;
                            } catch (Exception e) {
                                e.printStackTrace();
                                if(util.getBoolean(Constants.KEY_DEBUG_MESSAGES)) {
                                    showText(e.getMessage());
                                }
                            }
                            Thread.sleep(2500);
                        }
                        if(util.getBoolean(Constants.KEY_DEBUG_MESSAGES)) {
                            showText("Subscribe to block notification");
                        }
                        constants.wallet.subscribeToBlockNotifications(constants.notificationError);
                        setConnectionStatus(getString(R.string.discovering_used_addresses));
                        if(util.getBoolean(Constants.KEY_DEBUG_MESSAGES)) {
                            showText("Discover addresses");
                        }
                        constants.wallet.discoverActiveAddresses(false,null);
                        constants.wallet.loadActiveDataFilters();
                        setConnectionStatus(getString(R.string.fetching_headers));
                        if(util.getBoolean(Constants.KEY_DEBUG_MESSAGES)) {
                            showText("Fetch Headers");
                        }
                        long rescanHeight = constants.wallet.fetchHeaders();
                        if (rescanHeight != -1) {
                            util.setInt(PreferenceUtil.RESCAN_HEIGHT, (int) rescanHeight);
                        }
                        setConnectionStatus(getString(R.string.publish_unmined_transaction));
                        constants.wallet.publishUnminedTransactions();
                        setConnectionStatus("Connected");
                        rescanBlocks();
                    } else {
                        constants.wallet.processNotification(MainActivity.this);
                        constants.wallet.startSPVConnection(Utils.getPeerAddress(util));
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
        connectionThread.setDaemon(true);
        connectionThread.start();
    }

    @Override
    public void onProcessCallback(final long processType, final long state, final String params) {
        System.out.println("Process Received: "+processType+" State: "+state);
        if(state == Mobilewallet.ProcessStateEnd){
            isProcessRunning = false;
            scanning = false;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    addressDiscovery = false;
                    rescanHeight.setText("");
                    latestBlock.setVisibility(View.VISIBLE);
                    if(processType == Mobilewallet.ProcessTypeRescan){
                        sendBroadcast(new Intent(Constants.ACTION_BLOCK_SCAN_COMPLETE));
                    }
                }
            });
            if(processType == Mobilewallet.ProcessTypeFetchHeaders || processType == Mobilewallet.ProcessTypeAddressDiscovery){
                //Utils.backupWalletDB(MainActivity.this);
            }
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(state == Mobilewallet.ProcessStateStart) {
                    latestBlock.setVisibility(View.GONE);
                    isProcessRunning = true;
                    bestBlockHeight.setText(String.format(Locale.getDefault(), " of %d", estimateBlocks()));
                    if(processType == Mobilewallet.ProcessTypeFetchCFilters){
                        connectionStatus.setText("Fetching CFilter...");
                    }else if(processType == Mobilewallet.ProcessTypeFetchHeaders){
                        connectionStatus.setText(getString(R.string.fetching_headers));
                    }else if(processType == Mobilewallet.ProcessTypeRescan){
                        scanning = true;
                        connectionStatus.setText("Rescanning");
                    }else if(processType == Mobilewallet.ProcessTypeAddressDiscovery){
                        addressDiscovery = true;
                        setConnectionStatus(getString(R.string.discovering_used_addresses));
                    }
                }else if(state == Mobilewallet.ProcessStateUpdate){
                    isProcessRunning = true;
                    String[] args = params.split(";");
                    if(processType == Mobilewallet.ProcessTypeFetchCFilters){
                        connectionStatus.setText("Fetching CFilter "+args[0]+" - "+ args[1]);
                        bestBlockHeight.setText(String.format(Locale.getDefault(), " of %d", estimateBlocks()));
                    }else if(processType == Mobilewallet.ProcessTypeFetchHeaders){
                        connectionStatus.setText(getString(R.string.fetching_headers));
                        rescanHeight.setText(args[0]);
                        bestBlockHeight.setText(String.format(Locale.getDefault(), " of %d", estimateBlocks()));
                    }else if(processType == Mobilewallet.ProcessTypeRescan){
                        scanning = true;
                        connectionStatus.setText("Rescanning");
                        rescanHeight.setText(args[0]);
                        bestBlockHeight.setText(String.format(Locale.getDefault(), " of %d", constants.wallet.getBestBlock()));
                    }else if(processType == Mobilewallet.ProcessTypeAddressDiscovery){
                        addressDiscovery = true;
                        if(args[0].equals("0")){
                            setConnectionStatus(getString(R.string.discovering_used_accounts));
                        }else{
                            setConnectionStatus(getString(R.string.discovering_used_addresses));
                        }

                    }
                }
            }
        });
    }
}