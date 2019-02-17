/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import com.dcrandroid.R;
import com.dcrandroid.adapter.ConnectionAdapter;
import com.dcrandroid.data.Connection;
import com.dcrandroid.util.RecyclerTouchListener;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by Macsleven on 05/01/2018.
 */

public class ConnectionActivity extends AppCompatActivity {
    private List<Connection> connectionList = new ArrayList<>();
    private ConnectionAdapter connectionAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS |
                    View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        setContentView(R.layout.activity_connection_page);
        setTitle(getString(R.string.connection));
        RecyclerView recyclerView = findViewById(R.id.connection_recycler_view);
        connectionAdapter = new ConnectionAdapter(connectionList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Connection connection = connectionList.get(position);
                ConnectionFunction(connection.getConnection());
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));
        recyclerView.setAdapter(connectionAdapter);
        prepareConnectionData();
    }

    public void prepareConnectionData() {
        Connection connection = new Connection(getString(R.string.get_peer_info));
        connectionList.add(connection);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void ConnectionFunction(String connection) {
        if (connection.equals("Get Peers Info")) {
            Intent intent = new Intent(ConnectionActivity.this, GetPeersActivity.class);
            startActivity(intent);
        }
    }

    public interface ClickListener {
        void onClick(View view, int position);

        void onLongClick(View view, int position);
    }
}
