package com.example.jtestapk;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.utils.CustomAnimationUtils;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.safetynet.SafeBrowsingThreat;
import com.google.android.gms.safetynet.SafetyNet;
import com.google.android.gms.safetynet.SafetyNetApi;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QRCodeActivity extends AppCompatActivity {

    private IntentIntegrator qrScan;

    private String SAFE_BROWSING_API_KEY;
    private boolean isIgnore;

    private SharedPreferences sharedPrefGoogleApi;

    LayoutInflater inflater;

    CustomAnimationUtils customAnimationUtils = new CustomAnimationUtils();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();

        sharedPrefGoogleApi = getSharedPreferences("googleApi", MODE_PRIVATE);

        //init inflater
        inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        SAFE_BROWSING_API_KEY = sharedPrefGoogleApi.getString("settingSafeBrowsingApi", "");
        isIgnore = sharedPrefGoogleApi.getBoolean("isIgnore", false);

        if (SAFE_BROWSING_API_KEY.isEmpty() && !isIgnore) {
            setupPromptSetting();
        }

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
                    if (SAFE_BROWSING_API_KEY.isEmpty() && isIgnore) {
                        Toast.makeText(getApplicationContext(), "Notice: Safe Browsing API is not setup yet", Toast.LENGTH_LONG).show();
                        Pattern pattern = Pattern.compile("(?<=&|\\?)utm_.*?(&|$)");
                        Matcher matcher = pattern.matcher(result.getContents());
                        if (matcher.find()) {
                            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(QRCodeActivity.this);
                            dialogBuilder.setTitle(getResources().getString(R.string.dialog_msg_utm));
                            dialogBuilder.setMessage(getResources().getString(R.string.dialog_msg_utm));
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
                                        dialogBuilder.setTitle(getResources().getString(R.string.dialog_msg_utm));
                                        dialogBuilder.setMessage(getResources().getString(R.string.dialog_msg_utm));
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
                                    dialogBuilder.setTitle(getResources().getString(R.string.dialog_title_safe_browsing));
                                    dialogBuilder.setMessage(getResources().getString(R.string.dialog_msg_safe_browsing));
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
                            }
                        });
                    }
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

    private void setupPromptSetting() {
        View popupView = inflater.inflate(R.layout.qr_code_api_confirm_popup, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(QRCodeActivity.this);
        alertDialogBuilder.setView(popupView);
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alertDialog.getWindow().getAttributes().windowAnimations = R.style.customDialogInOutAnimation;
        alertDialog.show();
        CheckBox doNotShowAgain = popupView.findViewById(R.id.doNotShowAgain);
        if (!SAFE_BROWSING_API_KEY.isEmpty()) {
            doNotShowAgain.setChecked(true);
            doNotShowAgain.setEnabled(false);
        }
        Button qrPromptConfirmBtn = popupView.findViewById(R.id.qrPromptConfirmBtn);
        qrPromptConfirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (doNotShowAgain.isChecked()) {
                    sharedPrefGoogleApi.edit().putBoolean("isIgnore", true).apply();
                } else {
                    sharedPrefGoogleApi.edit().putBoolean("isIgnore", false).apply();
                }
                setupGoogleApiSetting();
            }
        });
        Button qrPromptCancelBtn = popupView.findViewById(R.id.qrPromptCancelBtn);
        qrPromptCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (doNotShowAgain.isChecked()) {
                    sharedPrefGoogleApi.edit().putBoolean("isIgnore", true).apply();
                } else {
                    sharedPrefGoogleApi.edit().putBoolean("isIgnore", false).apply();
                }
                popupView.setAnimation(customAnimationUtils.fadeOutAnimationDefault(popupView.getContext()));
                alertDialog.dismiss();
            }
        });
    }

    private void setupGoogleApiSetting() {
        View popupView = inflater.inflate(R.layout.menu_app_setting_popup_google_api, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(QRCodeActivity.this);
        alertDialogBuilder.setView(popupView);
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alertDialog.getWindow().getAttributes().windowAnimations = R.style.customDialogInOutAnimation;
        alertDialog.show();
        TextInputEditText settingSafeBrowsingApiInput = popupView.findViewById(R.id.settingSafeBrowsingApi);
        settingSafeBrowsingApiInput.setText(sharedPrefGoogleApi.getString("settingSafeBrowsingApi", ""));
        TextView settingPrompt = popupView.findViewById(R.id.settingPrompt);
        Button settingSaveBtn = popupView.findViewById(R.id.settingSaveBtn);
        settingSaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean validate = true;
                if (settingSafeBrowsingApiInput.getText().toString().isEmpty()) {
                    settingSafeBrowsingApiInput.setBackgroundResource(R.drawable.textinput_surface_yellow);
                    validate = false;
                }
                if (!validate) {
                    settingPrompt.setVisibility(View.VISIBLE);
                } else {
                    settingPrompt.setVisibility(View.INVISIBLE);
                    sharedPrefGoogleApi.edit()
                            .putString("settingSafeBrowsingApi", settingSafeBrowsingApiInput.getText().toString())
                            .putBoolean("isCompleted", true)
                            .apply();
                    Toast.makeText(getApplicationContext(), "Setting updated.", Toast.LENGTH_SHORT).show();
                    alertDialog.dismiss();
                }
                restartRefresh();
            }
        });
        Button settingClearBtn = popupView.findViewById(R.id.settingClearBtn);
        settingClearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settingSafeBrowsingApiInput.getText().clear();
                settingSafeBrowsingApiInput.setBackgroundResource(R.drawable.view_underline);
                settingPrompt.setVisibility(View.INVISIBLE);
            }
        });
        Button settingCancelBtn = popupView.findViewById(R.id.settingCancelBtn);
        settingCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupView.setAnimation(customAnimationUtils.fadeOutAnimationDefault(popupView.getContext()));
                alertDialog.dismiss();
            }
        });
    }

    private void restartRefresh() {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

}