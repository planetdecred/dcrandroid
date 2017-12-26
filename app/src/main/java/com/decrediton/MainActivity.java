package com.decrediton;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
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
import android.widget.Toast;

import com.decrediton.fragments.AccountsFragment;
import com.decrediton.fragments.HelpFragment;
import com.decrediton.fragments.HistoryFragment;
import com.decrediton.fragments.OverviewFragment;
import com.decrediton.fragments.ReceiveFragment;
import com.decrediton.fragments.SendFragment;
import com.decrediton.fragments.SettingsFragment;
import com.decrediton.fragments.TicketsFragment;

import org.json.JSONException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;


import dcrwallet.Dcrwallet;
public class MainActivity extends AppCompatActivity implements  NavigationView.OnNavigationItemSelectedListener{

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView =  (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //add this line to display menu1 when the activity is loaded
        displaySelectedScreen(R.id.nav_overview);

     /*   findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Method method = Dcrwallet.class.getDeclaredMethod("createWallet", String.class);
                    Method callback = MainActivity.this.getClass().getDeclaredMethod("createWalletCallback", String.class);
                    ProgressDialog pd = Utils.getProgressDialog(MainActivity.this, false, false,"Creating Wallet...");
                    new BackgroundWorker(callback, pd, MainActivity.this).execute(method);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
        });
        findViewById(R.id.btn_open).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Method method = Dcrwallet.class.getDeclaredMethod("openWallet");
                    Method callback = MainActivity.this.getClass().getDeclaredMethod("openWalletCallback", String.class);
                    ProgressDialog pd = Utils.getProgressDialog(MainActivity.this, false, false,"Creating Wallet...");
                    new BackgroundWorker(callback, pd, MainActivity.this).execute(method);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
        });

        findViewById(R.id.btn_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Method method = Dcrwallet.class.getDeclaredMethod("closeWallet");
                    Method callback = MainActivity.this.getClass().getDeclaredMethod("closeWalletCallback", String.class);
                    ProgressDialog pd = Utils.getProgressDialog(MainActivity.this, false, false,"Creating Wallet...");
                    new BackgroundWorker(callback, pd, MainActivity.this).execute(method);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    //Called by Method.Invoke
    public void createWalletCallback(String responseJson){
        try {
            DcrResponse response = DcrResponse.parse(responseJson);
            if(response.errorOccurred){
                Toast.makeText(this, "Error Occurred: "+response.content, Toast.LENGTH_SHORT).show();
            }else{
                new AlertDialog.Builder(this)
                        .setMessage(response.content)
                        .setPositiveButton("OK", null)
                        .show();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void openWalletCallback(String responseJson){
        try {
            DcrResponse response = DcrResponse.parse(responseJson);
            if(response.errorOccurred){
                Toast.makeText(this, "Error Occurred: "+response.content, Toast.LENGTH_LONG).show();
            }else{
                if(response.content.equals("true")) {
                    Toast.makeText(this, "Wallet Opened", Toast.LENGTH_LONG).show();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void closeWalletCallback(String responseJson){
        try {
            DcrResponse response = DcrResponse.parse(responseJson);
            if(response.errorOccurred){
                Toast.makeText(this, "Error Occurred: "+response.content, Toast.LENGTH_LONG).show();
            }else{
                if(response.content.equals("true")) {
                    Toast.makeText(this, "Wallet Closed", Toast.LENGTH_LONG).show();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }*/
    }
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void displaySelectedScreen(int itemId) {

        //creating fragment object
        Fragment fragment = null;

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
            case R.id.nav_tickets:
                fragment = new TicketsFragment();
                break;
            case R.id.nav_help:
                fragment = new HelpFragment();
                break;
            case R.id.nav_settings:
                fragment = new SettingsFragment();
                break;

        }

        //replacing the fragment
        if (fragment != null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.frame, fragment);
            ft.commit();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        //calling the method displayselectedscreen and passing the id of selected menu
        displaySelectedScreen(item.getItemId());
        //make this method blank
        return true;
    }

}
