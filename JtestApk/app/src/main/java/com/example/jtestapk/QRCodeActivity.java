package com.example.jtestapk;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class QRCodeActivity extends AppCompatActivity {

    private IntentIntegrator qrScan;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        qrScan = new IntentIntegrator(this);
        qrScan.setOrientationLocked(false);
        qrScan.setCaptureActivity(CaptureActivityPortrait.class);
        qrScan.initiateScan();
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode,
                resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                finish();
                Toast.makeText(getApplicationContext(),"Back to main menu.", Toast.LENGTH_SHORT).show();
            } else {
                //scan success
                if (URLUtil.isValidUrl(result.getContents())) {
                    WebView webView = new WebView(this);
                    webView.getSettings().setJavaScriptEnabled(true);
                    webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
                    webView.getSettings().setDomStorageEnabled(true);
                    webView.setWebViewClient(new WebViewClient());
                    webView.loadUrl(result.getContents());
                    setContentView(webView);
                } else {
                    ScrollView scrollView = new ScrollView(this);
                    TextView textView = new TextView(this);
                    textView.setGravity(Gravity.CENTER_VERTICAL);
                    textView.setTextSize(36);
                    textView.setText(result.getContents());
                    scrollView.addView(textView);
                    setContentView(scrollView);
                }
                Toast.makeText(getApplicationContext(),result.getFormatName(), Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

}