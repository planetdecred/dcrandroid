package com.dcrandroid.activities;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.dcrandroid.R;
import com.dcrandroid.util.Utils;

/**
 * Created by Macsleven on 21/01/2018.
 */

public class WebviewActivity extends AppCompatActivity {
    private WebView webView;
    ProgressDialog pd;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.webview_activity);
        getWindow().setFeatureInt( Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);
        webView = (WebView) findViewById(R.id.webView1);
        webView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress)
            {
                setTitle("Loading...");
                setProgress(progress * 100);
                if(progress == 100)
                    pd.dismiss();
                    setTitle(R.string.app_name);
            }
        });
        webView.setWebViewClient(new HelloWebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
      //  webView.getSettings().setJavaScriptEnabled(true);
        getIntent().getStringExtra("TxHash");
        pd = Utils.getProgressDialog(WebviewActivity.this, false,false,getString(R.string.loading));
        pd.show();
        webView.loadUrl(getIntent().getStringExtra("TxHash"));
    }
    private class HelloWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
