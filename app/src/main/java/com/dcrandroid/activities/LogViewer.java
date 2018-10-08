package com.dcrandroid.activities;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
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
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS |
                    View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        setContentView(R.layout.activity_log_viewer);
        TextView logTextView = findViewById(R.id.log_text);
        String logPath = getIntent().getExtras().getString("log_path");
        if(logPath != null){
            File f = new File(logPath);

            logTextView.setText(getLastNLogLine(f));
        }
    }

    public String getLastNLogLine(File file) {
        StringBuilder s = new StringBuilder();
        try {
            Process p = Runtime.getRuntime().exec("tail -n500 "+file);
            java.io.BufferedReader input = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()));
            String line;
            int i = 0;
            while((line = input.readLine()) != null){
                s.append(line);
                s.append("\n");
                i++;
                System.out.println("++i"+i);
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return s.toString();
    }
}
