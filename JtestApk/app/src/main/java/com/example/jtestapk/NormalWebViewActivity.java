package com.example.jtestapk;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Window;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class NormalWebViewActivity extends AppCompatActivity {

    private String url = "";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();

        url = getIntent().getStringExtra("url");

        if (URLUtil.isValidUrl(url)) {
            WebView webView = new WebView(NormalWebViewActivity.this);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
            webView.getSettings().setDomStorageEnabled(true);
            webView.setWebViewClient(new WebViewClient());
            webView.loadUrl(url);
            setContentView(webView);
        } else {
            Toast.makeText(getApplicationContext(), "INVALID URL", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}