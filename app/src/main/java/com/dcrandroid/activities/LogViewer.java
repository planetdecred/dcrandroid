/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.dcrandroid.BuildConfig;
import com.dcrandroid.R;
import com.dcrandroid.util.Utils;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;

/**
 * Created by collins on 2/17/18.
 */

public class LogViewer extends BaseActivity {
    private static final int MENU_ITEM = 1;

    private TextView logTextView;
    Thread buffer = new Thread() {
        public void run() {
            try {
                String logPath = getFilesDir() + BuildConfig.LogDir;
                File file = new File(logPath);
                if (!file.exists()) {
                    Snackbar.make(logTextView, R.string.log_file_not_found, Snackbar.LENGTH_LONG).setAction(R.string.dismiss, null).show();
                    return;
                }

                Process p = Runtime.getRuntime().exec("tail -f -n500 " + file);
                java.io.BufferedReader input = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()));
                String line;
                while ((line = input.readLine()) != null && !interrupted()) {
                    addLine("\n" + line);
                }
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_log_viewer);
        logTextView = findViewById(R.id.log_text);

        buffer.start();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (buffer != null && !buffer.isInterrupted()) {
            buffer.interrupt();
        }
    }

    private void addLine(final String line) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (buffer != null && !buffer.isInterrupted()) {
                    logTextView.append(line);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_ITEM, Menu.NONE, "Copy").setIcon(R.drawable.ic_copy).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ITEM:
                Utils.copyToClipboard(getCurrentFocus(), logTextView.getText().toString(), R.string.wallet_log_copied); // TODO:
                return true;

            default:
                return false;
        }
    }
}
