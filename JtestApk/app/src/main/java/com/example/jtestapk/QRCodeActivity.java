package com.example.jtestapk;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.utils.PropertiesUtils;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.safetynet.SafeBrowsingThreat;
import com.google.android.gms.safetynet.SafetyNet;
import com.google.android.gms.safetynet.SafetyNetApi;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Tasks;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.bson.Document;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QRCodeActivity extends AppCompatActivity {

    private IntentIntegrator qrScan;

    private Properties appProperties;
    private String SAFE_BROWSING_API_KEY;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();

        //get app.properties
        try {
            appProperties = PropertiesUtils.getAppProperties(getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
            finish();
            return;
        }

        SAFE_BROWSING_API_KEY = appProperties.getProperty("google.safe.browsing.api.key");

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
                    //Perform SafetyNet Checking Url
                    SafetyNet.getClient(this).lookupUri(result.getContents(), SAFE_BROWSING_API_KEY, SafeBrowsingThreat.TYPE_POTENTIALLY_HARMFUL_APPLICATION,
                            SafeBrowsingThreat.TYPE_SOCIAL_ENGINEERING).addOnSuccessListener(this, new OnSuccessListener<SafetyNetApi.SafeBrowsingResponse>() {
                        @Override
                        public void onSuccess(SafetyNetApi.SafeBrowsingResponse sbResponse) {
                            if (sbResponse.getDetectedThreats().isEmpty()) {
                                // No threats found. Check utm code
                                Pattern pattern = Pattern.compile("(?<=&|\\?)utm_.*?(&|$)");
                                Matcher matcher = pattern.matcher(result.getContents());
                                if (matcher.find()) {
                                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(QRCodeActivity.this);
                                    dialogBuilder.setTitle("UTM track code found");
                                    dialogBuilder.setMessage("Do you want to remove utm parameter in the url?");
                                    dialogBuilder.setPositiveButton(getResources().getString(R.string.dialog_positive), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            webView.loadUrl(result.getContents().replaceAll("(?<=&|\\?)utm_.*?(&|$)", ""));
                                            Toast.makeText(getApplicationContext(), "utm Removed.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                    dialogBuilder.setNegativeButton(getResources().getString(R.string.dialog_negative), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            webView.loadUrl(result.getContents());
                                            Toast.makeText(getApplicationContext(), "utm Kept.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                    dialogBuilder.show();
                                } else {
                                    webView.loadUrl(result.getContents());
                                }
                            } else {
                                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(QRCodeActivity.this);
                                dialogBuilder.setTitle("THREATS FOUND via Google Safe Browsing API");
                                dialogBuilder.setMessage("[NO]Discard and Back is HIGHLY RECOMMENDED. \r\n[YES]Go into the url AS YOUR OWN RISK.");
                                dialogBuilder.setPositiveButton(getResources().getString(R.string.dialog_positive), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        webView.loadUrl(result.getContents());
                                        Toast.makeText(getApplicationContext(), "WARNING: THREATS FOUND", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                dialogBuilder.setNegativeButton(getResources().getString(R.string.dialog_negative), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                        Toast.makeText(getApplicationContext(), "Discard and Back.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                dialogBuilder.show();
                            }
                        }
                    }).addOnFailureListener(this, new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            if (e instanceof ApiException) {
                                ApiException apiException = (ApiException) e;
                                Log.d("SafetyNet FAILED", "Error: " + CommonStatusCodes.getStatusCodeString(apiException.getStatusCode()));
                                Toast.makeText(getApplicationContext(), "SafetyNet FAILED: " + CommonStatusCodes.getStatusCodeString(apiException.getStatusCode()), Toast.LENGTH_SHORT).show();
                            } else {
                                // A different, unknown type of error occurred.
                                Log.d("SafetyNet FAILED", "Error: " + e.getMessage());
                                Toast.makeText(getApplicationContext(), "SafetyNet FAILED: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                            finish();
                        }
                    });
                    setContentView(webView);
                } else {
                    ScrollView scrollView = new ScrollView(this);
                    TextView textView = new TextView(this);
                    textView.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL);
                    textView.setTextSize(24);
                    textView.setText(result.getContents());
                    scrollView.addView(textView);
                    setContentView(scrollView);
                }
//                Toast.makeText(getApplicationContext(),result.getFormatName(), Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

}