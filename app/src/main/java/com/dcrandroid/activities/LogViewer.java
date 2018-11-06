package com.dcrandroid.activities;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.dcrandroid.R;

import java.io.File;

/**
 * Created by collins on 2/17/18.
 */

public class LogViewer extends AppCompatActivity {

    private TextView logTextView;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS |
                    View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        setContentView(R.layout.activity_log_viewer);
        logTextView = findViewById(R.id.log_text);

        buffer.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(buffer != null && !buffer.isInterrupted()){
            buffer.interrupt();
        }
    }

    Thread buffer = new Thread(){
        public void run(){
            try {
                String logPath = getIntent().getExtras().getString("log_path");
                if(logPath == null){
                    return;
                }
                File file = new File(logPath);
                if(!file.exists()){
                    Snackbar.make(logTextView, R.string.log_file_not_found, Snackbar.LENGTH_LONG).setAction(R.string.dismiss, null).show();
                    return;
                }

                Process p = Runtime.getRuntime().exec("tail -f -n500 "+file);
                java.io.BufferedReader input = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()));
                String line;
                while((line = input.readLine()) != null && !interrupted()){
                    addLine("\n" + line);
                }
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
    };

    private void addLine(final String line){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(buffer != null && !buffer.isInterrupted()) {
                    logTextView.append(line);
                }
            }
        });
    }
}
