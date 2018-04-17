package com.dcrandroid;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.net.Uri;
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
import com.dcrandroid.util.BlockNotificationProxy;
import com.dcrandroid.util.DcrConstants;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Random;

import mobilewallet.BlockScanResponse;
import mobilewallet.TransactionListener;

public class MainActivity extends AppCompatActivity implements  NavigationView.OnNavigationItemSelectedListener, TransactionListener, BlockScanResponse, Animation.AnimationListener, BlockNotificationProxy {

    public String menuADD ="0";
    public static MenuItem menuOpen;
    private Fragment fragment;
    private NavigationView navigationView;
    private DcrConstants constants;
    private PreferenceUtil util;
    private NotificationManager notificationManager;
    private TextView rescanHeight, chainStatus, connectionStatus;
    private Animation animRotate;
    public MainActivity mainActivity;
    private ImageView rescanImage, stopScan;
    private boolean scanning = false;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("new transaction", "Dcrandroid", NotificationManager.IMPORTANCE_DEFAULT);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setImportance(NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }
        util = new PreferenceUtil(this);
        constants = DcrConstants.getInstance();
        constants.wallet.transactionNotification(this);
        constants.notificationProxy = this;
        rescanHeight = findViewById(R.id.rescan_height);
        connectionStatus = findViewById(R.id.tv_connection_status);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //add this line to display menu1 when the activity is loaded
        displaySelectedScreen(R.id.nav_overview);

        final TextView bestBlockHeight = findViewById(R.id.best_block_height);
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
            rescanBlocks();
        }else{
            if(util.getBoolean(Constants.KEY_DEBUG_MESSAGES)) {
                showText("Connecting to RPC server");
            }
            connectToRPCServer();
        }
        new Thread(){
            public void run(){
                while(true){
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                int bestBlock = constants.wallet.getBestBlock();
                                if(scanning){
                                    bestBlockHeight.setText(String.valueOf(bestBlock));
                                    return;
                                }
                                long lastBlockTime = constants.wallet.getBestBlockTimeStamp();
                                long currentTime = System.currentTimeMillis() / 1000;
                                long estimatedBlocks = ((currentTime - lastBlockTime) / 120) + bestBlock;
                                if(estimatedBlocks > bestBlock){
                                    bestBlockHeight.setText(bestBlock +" of "+estimatedBlocks);
                                    chainStatus.setText("");
                                }else{
                                    bestBlockHeight.setText(String.valueOf(bestBlock));
                                    chainStatus.setText(Utils.calculateTime((System.currentTimeMillis()/1000) - lastBlockTime));
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
                    }
                }
            }
        }.start();
        rescanImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                constants.wallet.rescan(0, MainActivity.this);
                rescanImage.startAnimation(animRotate);
                rescanImage.setEnabled(false);
                chainStatus.setVisibility(View.GONE);
                stopScan.setVisibility(View.VISIBLE);
                scanning = true;
            }
        });
        stopScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanning = false;
            }
        });
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
        } else {
            displaySelectedScreen(R.id.nav_overview);
        } 
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.exit(0);
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
        if(!menuADD.equals("1") ) {
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

    public  void displaySelectedScreen(int itemId) {

        //initializing the fragment object which is selected
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
//            case R.id.nav_settings:
//                fragment = new SettingsFragment();
//                break;
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
        //calling the method displayselectedscreen and passing the id of selected menu
        if(item.getItemId() == R.id.nav_settings){
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }else {
            displaySelectedScreen(item.getItemId());
        }
        //make this method blank
        return true;
    }

    @Override
    public void onTransaction(String s) {
        System.out.println("Notification Received");
        System.out.println(s);
        sendBroadcast(new Intent(Constants.ACTION_BLOCK_SCAN_COMPLETE));
        if(util.getBoolean(Constants.KEY_TRANSACTION_NOTIFICATION, true)) {
            try {
                JSONObject obj = new JSONObject(s);
                double fee = obj.getDouble("Fee");
                if (fee == 0) {
                    float amount = obj.getLong("Amount") / 100000000;
                    String hash = obj.getString("Hash");
                    DecimalFormat format = new DecimalFormat("#.########");
                    sendNotification(format.format(amount), hash);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                if(util.getBoolean(Constants.KEY_DEBUG_MESSAGES)) {
                    showText(e.getMessage());
                }
            }
        }
    }

    private void sendNotification(String amount, String hash){
        Intent launchIntent = new Intent(this,MainActivity.class);
        PendingIntent launchPendingIntent = PendingIntent.getActivity(this, 1, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://testnet.dcrdata.org/tx/"+hash));
        PendingIntent pi = PendingIntent.getActivity(this, 2, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Icon icon = Icon.createWithResource(this, R.drawable.ic_menu_share);
            Notification.Action action = new Notification.Action.Builder(icon,"VIEW ON DCRDATA", pi).build();
            notification = new Notification.Builder(this, "new transaction")
                    .setContentTitle("New Transaction")
                    .setContentText("You received "+amount+" dcr")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setOngoing(false)
                    .setAutoCancel(true)
                    .addAction(action)
                    .setContentIntent(launchPendingIntent)
                    .build();
        }else{
            NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.drawable.ic_menu_share,"VIEW ON DCRDATA", pi).build();
            notification = new NotificationCompat.Builder(this)
                    .setContentTitle("New Transaction")
                    .setContentText("You received "+amount+" dcr")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setOngoing(false)
                    .setAutoCancel(true)
                    .addAction(action)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(launchPendingIntent)
                    .build();
        }
        notificationManager.notify(new Random().nextInt(), notification);
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    setConnectionStatus("Connecting to RPC Server");
                    String dcrdAddress = Utils.getDcrdNetworkAddress(MainActivity.this);
                    if (Integer.parseInt(util.get(Constants.KEY_NETWORK_MODES, "0")) != 0) {
                        int i = 0;
                        for (; ; ) {
                            try {
                                if(util.getBoolean(Constants.KEY_DEBUG_MESSAGES)) {
                                    showText("Connecting attempt " + (++i));
                                }
                                constants.wallet.startRPCClient(dcrdAddress, "dcrwallet", "dcrwallet", Utils.getConnectionCertificate(MainActivity.this).getBytes());
                                    break;
                            } catch (Exception e) {
                                e.printStackTrace();
                                showText(e.getMessage());
                            }
                            Thread.sleep(2500);
                        }
                    } else {
                        //Spv connection will be handled here
                        setConnectionStatus("SPV not yet implemented");
                        return;
                    }
                    if(util.getBoolean(Constants.KEY_DEBUG_MESSAGES)) {
                        showText("Subscribe to block notification");
                    }
                    constants.wallet.subscribeToBlockNotifications(constants.notificationError);
                    setConnectionStatus(getString(R.string.discovering_address));
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
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();

    }
}