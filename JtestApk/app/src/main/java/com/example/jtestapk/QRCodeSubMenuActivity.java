package com.example.jtestapk;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;

import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QRCodeSubMenuActivity extends AppCompatActivity {

    private LinearLayout linearLayout;
    private ImageView imageView;
    private static final int SELECT_PICTURE = 1;
    private String qrCodeToStrResult;

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

        //setup layout programmatically
        linearLayout = new LinearLayout(this);
        LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        linearLayout.setLayoutParams(linearLayoutParams);
        linearLayout.setGravity(Gravity.CENTER_VERTICAL);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams buttonLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonLayoutParams.gravity = Gravity.CENTER_HORIZONTAL;
        buttonLayoutParams.setMargins(40, 40, 40, 40);
        Button generateQRCodeButton = new Button(this);
        generateQRCodeButton.setText(getResources().getString(R.string.button_gen_qrcode));
        generateQRCodeButton.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        linearLayout.addView(generateQRCodeButton, buttonLayoutParams);
        Button readFromImgButton = new Button(this);
        readFromImgButton.setText(getResources().getString(R.string.button_read_image));
        readFromImgButton.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        linearLayout.addView(readFromImgButton, buttonLayoutParams);
        setContentView(linearLayout);

        generateQRCodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setupLayoutForInputText();
            }
        });
        readFromImgButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setDataAndType(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                startActivityForResult(intent, SELECT_PICTURE);
            }
        });
    }

    private void setupLayoutForInputText() {
        linearLayout = new LinearLayout(this);
        LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        linearLayout.setLayoutParams(linearLayoutParams);
        linearLayout.setGravity(Gravity.BOTTOM);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        imageView = new ImageView(this);
        LinearLayout.LayoutParams imageLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        imageView.setLayoutParams(imageLayoutParams);
        linearLayout.addView(imageView);
        LinearLayout.LayoutParams textInLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        textInLayoutParams.setMargins(40, 40, 40, 40);
        TextInputEditText textInputEditText = new TextInputEditText(this);
        textInputEditText.setHint(getResources().getString(R.string.hint_string_input));
        textInputEditText.setLayoutParams(textInLayoutParams);
        linearLayout.addView(textInputEditText);
        setContentView(linearLayout);

        textInputEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                try {
                    if (charSequence != "") {
                        Bitmap bitmap = strInToImage(charSequence.toString(), 600, 600);
                        imageView.setImageBitmap(bitmap);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(),"Error :" + e.toString(), Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupLayoutAfterImageConversion(String result) {
        if (URLUtil.isValidUrl(result)) {
            WebView webView = new WebView(this);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
            webView.getSettings().setDomStorageEnabled(true);
            webView.setWebViewClient(new WebViewClient());
            //Perform SafetyNet Checking Url
            SafetyNet.getClient(this).lookupUri(result, SAFE_BROWSING_API_KEY, SafeBrowsingThreat.TYPE_POTENTIALLY_HARMFUL_APPLICATION,
                    SafeBrowsingThreat.TYPE_SOCIAL_ENGINEERING).addOnSuccessListener(this, new OnSuccessListener<SafetyNetApi.SafeBrowsingResponse>() {
                @Override
                public void onSuccess(SafetyNetApi.SafeBrowsingResponse sbResponse) {
                    if (sbResponse.getDetectedThreats().isEmpty()) {
                        // No threats found. Check utm code
                        Pattern pattern = Pattern.compile("(?<=&|\\?)utm_.*?(&|$)");
                        Matcher matcher = pattern.matcher(result);
                        if (matcher.find()) {
                            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(QRCodeSubMenuActivity.this);
                            dialogBuilder.setTitle(getResources().getString(R.string.dialog_msg_utm));
                            dialogBuilder.setMessage(getResources().getString(R.string.dialog_msg_utm));
                            dialogBuilder.setPositiveButton(getResources().getString(R.string.dialog_positive), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    webView.loadUrl(result.replaceAll("(?<=&|\\?)utm_.*?(&|$)", ""));
                                    Toast.makeText(getApplicationContext(), "utm Removed.", Toast.LENGTH_SHORT).show();
                                }
                            });
                            dialogBuilder.setNegativeButton(getResources().getString(R.string.dialog_negative), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    webView.loadUrl(result);
                                    Toast.makeText(getApplicationContext(), "utm Kept.", Toast.LENGTH_SHORT).show();
                                }
                            });
                            dialogBuilder.show();
                        } else {
                            webView.loadUrl(result);
                        }
                    } else {
                        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(QRCodeSubMenuActivity.this);
                        dialogBuilder.setTitle(getResources().getString(R.string.dialog_title_safe_browsing));
                        dialogBuilder.setMessage(getResources().getString(R.string.dialog_msg_safe_browsing));
                        dialogBuilder.setPositiveButton(getResources().getString(R.string.dialog_positive), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                webView.loadUrl(result);
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
            textView.setGravity(Gravity.CENTER_VERTICAL);
            textView.setTextSize(36);
            textView.setText(result);
            scrollView.addView(textView);
            setContentView(scrollView);
        }
    }

    private Bitmap strInToImage(String text, int width, int height) throws Exception {
        BitMatrix bitMatrix;
        try {
            bitMatrix = new MultiFormatWriter().encode(text, BarcodeFormat.DATA_MATRIX.QR_CODE,
                    width, height, null);
        } catch (IllegalArgumentException Illegalargumentexception) {
            return null;
        }

        int bitMatrixWidth = bitMatrix.getWidth();
        int bitMatrixHeight = bitMatrix.getHeight();
        int[] pixels = new int[bitMatrixWidth * bitMatrixHeight];

        int colorWhite = 0xFFFFFFFF;
        int colorBlack = 0xFF000000;

        for (int y = 0; y < bitMatrixHeight; y++) {
            int offset = y * bitMatrixWidth;
            for (int x = 0; x < bitMatrixWidth; x++) {
                pixels[offset + x] = bitMatrix.get(x, y) ? colorBlack : colorWhite;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(bitMatrixWidth, bitMatrixHeight, Bitmap.Config.ARGB_4444);

        bitmap.setPixels(pixels, 0, width, 0, 0, bitMatrixWidth, bitMatrixHeight);
        return bitmap;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                Uri selectedImageUri = data.getData();
                try {
                    InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    qrCodeToStrResult = qrCodeImageToStr(bitmap);
                    setupLayoutAfterImageConversion(qrCodeToStrResult);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(),"Error :" + e.toString(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public static String qrCodeImageToStr(Bitmap bMap) throws Exception {
        String contents = null;

        int[] intArray = new int[bMap.getWidth()*bMap.getHeight()];
        bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(), bMap.getHeight());

        LuminanceSource source = new RGBLuminanceSource(bMap.getWidth(), bMap.getHeight(), intArray);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        Reader reader = new MultiFormatReader();
        Result result = reader.decode(bitmap);
        contents = result.getText();
        return contents;
    }

}
