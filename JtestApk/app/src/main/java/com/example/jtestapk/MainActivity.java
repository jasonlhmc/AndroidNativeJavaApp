package com.example.jtestapk;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.adapter.MenuViewPager2Adapter;
import com.example.adapter.MenuWeatherGridAdapter;
import com.example.model.GoogleNewsRssModelObject;
import com.example.model.WeatherObject;
import com.example.utils.CustomAnimationUtils;
import com.example.utils.PropertiesUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import io.realm.Realm;
import io.realm.mongodb.App;
import io.realm.mongodb.AppConfiguration;
import io.realm.mongodb.Credentials;
import io.realm.mongodb.User;

public class MainActivity extends AppCompatActivity {

    private Properties appProperties;
    private SharedPreferences sharedPrefWeather;
    private SharedPreferences sharedPrefMongoDb;
    private SharedPreferences sharedPrefFirebase;
    private SharedPreferences sharedPrefGoogleApi;

    private boolean isFabVisable;
    private boolean isWeatherEn;
    private boolean isAuthenticated;
    private String url;
    private boolean isWeatherPagerSetup;
    private boolean isGoogleRssSetup;

    private WeatherObject weatherObject;
    private List<GoogleNewsRssModelObject> googleNewsRssModelObjectList;

    private TabLayout tabLayout;
    private ViewPager2 menuViewPager;
    private FloatingActionButton fabJotNotes;
    private FloatingActionButton fabWeatherTranslate;
    private TextView fabTextWeatherTranslate;
    private SwipeRefreshLayout weatherPagerRefresh, newsPagerRefresh;
    private ProgressBar commonProgressBar;
    private LinearLayout newsCardList;

    private App mongoApp;

    LayoutInflater inflater;

    CustomAnimationUtils customAnimationUtils = new CustomAnimationUtils();

    SimpleDateFormat fromFmt = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'");
    SimpleDateFormat toFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public interface WeatherCallback {
        void onSuccess(String response);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_menu);

        //Resize when keyboard shows up
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        //get app.properties
        try {
            appProperties = PropertiesUtils.getAppProperties(getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
            finish();
            return;
        }

        sharedPrefWeather = getSharedPreferences("weatherSetting", MODE_PRIVATE);
        isWeatherEn = sharedPrefWeather.getBoolean("isWeatherEn", true);

        //init layout
//        weatherLayoutSetup();

        //init inflater
        inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        //setup tabLayout, viewPager2, viewPager2s fragments
        isWeatherPagerSetup = false;
        isGoogleRssSetup = false;
        tabLayout = findViewById(R.id.tabLayout);
        menuViewPager = findViewById(R.id.menuViewPager);
        menuViewPager.setOffscreenPageLimit(2);
        MenuViewPager2Adapter menuViewPager2Adapter = new MenuViewPager2Adapter(this);
        menuViewPager.setAdapter(menuViewPager2Adapter);
        new TabLayoutMediator(tabLayout, menuViewPager, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                if (position == 1) {
                    tab.setText("9-day \r\nWeather Forecast");
                } else if (position == 0) {
                    tab.setText("Google News");
                }
            }
        }).attach();
        menuViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }
            @Override
            public void onPageSelected(int position) {
                if (position != 0) {
                    if (!isWeatherPagerSetup) {
                        weatherObjectLayoutSetup();
                        isWeatherPagerSetup = true;
                    }
                    fabWeatherTranslate.setEnabled(true);
                } else {
                    if (position == 0) {
                        if (!isGoogleRssSetup) {
                            getGoogleNewsRss(null);
                            isGoogleRssSetup = true;
                        }
                    }
                    fabWeatherTranslate.setEnabled(false);
                }
                super.onPageSelected(position);
            }
            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        });
        //init Floating Action Buttons
        FloatingActionButton fabCheckCur = findViewById(R.id.fabCheckCur);
        TextView fabTextCheckCur = findViewById(R.id.fabTextCheckCur);
        FloatingActionButton fabQrCode = findViewById(R.id.fabQrCode);
        TextView fabTextQrCode = findViewById(R.id.fabTextQrCode);
        fabJotNotes = findViewById(R.id.fabJotNotes);
        TextView fabTextJotNotes = findViewById(R.id.fabTextJotNotes);
        fabWeatherTranslate = findViewById(R.id.fabWeatherTranslate);
        fabTextWeatherTranslate = findViewById(R.id.fabTextWeatherTranslate);
        FloatingActionButton fabAppSetting = findViewById(R.id.fabAppSetting);
        TextView fabTextAppSetting = findViewById(R.id.fabTextAppSetting);
        isFabVisable = false;
        FloatingActionButton commonFab = findViewById(R.id.commonFab);
        commonFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isFabVisable) {
                    fabCheckCur.show();
                    fabTextCheckCur.setVisibility(View.VISIBLE);
                    fabQrCode.show();
                    fabTextQrCode.setVisibility(View.VISIBLE);
                    fabJotNotes.show();
                    fabTextJotNotes.setVisibility(View.VISIBLE);
                    fabWeatherTranslate.show();
                    fabTextWeatherTranslate.setVisibility(View.VISIBLE);
                    fabAppSetting.show();
                    fabTextAppSetting.setVisibility(View.VISIBLE);
                    isFabVisable = true;
                } else {
                    fabCheckCur.hide();
                    fabTextCheckCur.setVisibility(View.GONE);
                    fabQrCode.hide();
                    fabTextQrCode.setVisibility(View.GONE);
                    fabJotNotes.hide();
                    fabTextJotNotes.setVisibility(View.GONE);
                    fabWeatherTranslate.hide();
                    fabTextWeatherTranslate.setVisibility(View.GONE);
                    fabAppSetting.hide();
                    fabTextAppSetting.setVisibility(View.GONE);
                    isFabVisable = false;
                }
            }
        });
        fabCheckCur.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v("INFO", "Check Currency button Clicked");
                try {
                    Intent intent = new Intent(MainActivity.this, CheckCurrencyActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(),"Error :" + e.toString(), Toast.LENGTH_LONG).show();
                }
            }
        });
        fabQrCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v("INFO", "QR Code button Clicked");
                try {
                    Intent intent = new Intent(MainActivity.this, QRCodeActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(),"Error :" + e.toString(), Toast.LENGTH_LONG).show();
                }
            }
        });
        fabQrCode.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Log.v("INFO", "QR Code button LONG CLICKED");
                try {
                    Intent intent = new Intent(MainActivity.this, QRCodeSubMenuActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(),"Error :" + e.toString(), Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
        fabJotNotes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v("INFO", "Notes button Clicked");
                try {
                    Intent intent = new Intent(MainActivity.this, NotesActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(),"Error :" + e.toString(), Toast.LENGTH_LONG).show();
                }
            }
        });
        fabJotNotes.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Log.v("INFO", "Notes button LONG CLICKED");
                try {
                    Intent intent = new Intent(MainActivity.this, NotesListViewActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(),"Error :" + e.toString(), Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
        fabWeatherTranslate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v("INFO", "Translate button CLICKED");
                if (isWeatherEn) {
                    isWeatherEn = false;
                    sharedPrefWeather.edit().putBoolean("isWeatherEn", false).commit();
                    weatherObjectLayoutSetup();
                } else {
                    isWeatherEn = true;
                    sharedPrefWeather.edit().putBoolean("isWeatherEn", true).commit();
                    weatherObjectLayoutSetup();
                }
            }
        });
        fabAppSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v("INFO", "App Setting button CLICKED");
                View settingView = inflater.inflate(R.layout.menu_app_setting_select, null);
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                alertDialogBuilder.setView(settingView);
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                alertDialog.getWindow().getAttributes().windowAnimations = R.style.customDialogInOutAnimation;
                alertDialog.show();
                Button mongodbSettingBtn = settingView.findViewById(R.id.mongodbSettingBtn);
                mongodbSettingBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        setupMongoDBSetting();
                    }
                });
                Button firebaseSettingBtn = settingView.findViewById(R.id.firebaseSettingBtn);
                firebaseSettingBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        setupFirebaseSetting();
                    }
                });
                Button googleApiSettingBtn = settingView.findViewById(R.id.googleApiSettingBtn);
                googleApiSettingBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        setupGoogleApiSetting();
                    }
                });
                Button appSettingCancelBtn = settingView.findViewById(R.id.appSettingCancelBtn);
                appSettingCancelBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        alertDialog.dismiss();
                    }
                });
            }
        });

        //get MongoDB Auth for the first time running this App
        sharedPrefMongoDb = getSharedPreferences("MongoDb", MODE_PRIVATE);
        isAuthenticated = sharedPrefMongoDb.getBoolean("isAuthenticated", false);
        if (!isAuthenticated) {
            if (!mongoDbAuth()) {
                fabJotNotes.setEnabled(false);
            }
        }

        sharedPrefFirebase = getSharedPreferences("firebase", MODE_PRIVATE);
        sharedPrefGoogleApi = getSharedPreferences("googleApi", MODE_PRIVATE);

    }

    private void setupMongoDBSetting() {
        View popupView = inflater.inflate(R.layout.menu_app_setting_popup_mongodb, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setView(popupView);
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alertDialog.getWindow().getAttributes().windowAnimations = R.style.customDialogInOutAnimation;
        alertDialog.show();
        TextInputEditText settingMongoApiKey = popupView.findViewById(R.id.settingMongoApiKey);
        settingMongoApiKey.setText(sharedPrefMongoDb.getString("mongoDB.auth.api.key", ""));
        TextInputEditText settingMongoAppId = popupView.findViewById(R.id.settingMongoAppId);
        settingMongoAppId.setText(sharedPrefMongoDb.getString("mongoDB.appId", ""));
        TextInputEditText settingMongoClient = popupView.findViewById(R.id.settingMongoClient);
        settingMongoClient.setText(sharedPrefMongoDb.getString("mongoDB.client", ""));
        TextInputEditText settingMongoDatabase = popupView.findViewById(R.id.settingMongoDatabase);
        settingMongoDatabase.setText(sharedPrefMongoDb.getString("mongoDB.database", ""));
        TextInputEditText settingColNotes = popupView.findViewById(R.id.settingColNotes);
        settingColNotes.setText(sharedPrefMongoDb.getString("mongoDB.collection.notes", ""));
        TextInputEditText settingColKey = popupView.findViewById(R.id.settingColKey);
        settingColKey.setText(sharedPrefMongoDb.getString("mongoDB.collection.key", ""));
        TextView settingPrompt = popupView.findViewById(R.id.settingPrompt);
        Button settingSaveBtn = popupView.findViewById(R.id.settingSaveBtn);
        settingSaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean validate = true;
                if (settingMongoApiKey.getText().toString().isEmpty()) {
                    settingMongoApiKey.setBackgroundResource(R.drawable.textinput_surface_yellow);
                    validate = false;
                }
                if (settingMongoAppId.getText().toString().isEmpty()) {
                    settingMongoAppId.setBackgroundResource(R.drawable.textinput_surface_yellow);
                    validate = false;
                }
                if (settingMongoClient.getText().toString().isEmpty()) {
                    settingMongoClient.setBackgroundResource(R.drawable.textinput_surface_yellow);
                    validate = false;
                }
                if (settingMongoDatabase.getText().toString().isEmpty()) {
                    settingMongoDatabase.setBackgroundResource(R.drawable.textinput_surface_yellow);
                    validate = false;
                }
                if (settingColNotes.getText().toString().isEmpty()) {
                    settingColNotes.setBackgroundResource(R.drawable.textinput_surface_yellow);
                    validate = false;
                }
                if (settingColKey.getText().toString().isEmpty()) {
                    settingColKey.setBackgroundResource(R.drawable.textinput_surface_yellow);
                    validate = false;
                }
                if (!validate) {
                    settingPrompt.setVisibility(View.VISIBLE);
                } else {
                    settingPrompt.setVisibility(View.INVISIBLE);
                    sharedPrefMongoDb.edit()
                            .putString("mongoDB.auth.api.key", settingMongoApiKey.getText().toString())
                            .putString("mongoDB.appId", settingMongoAppId.getText().toString())
                            .putString("mongoDB.client", settingMongoClient.getText().toString())
                            .putString("mongoDB.database", settingMongoDatabase.getText().toString())
                            .putString("mongoDB.collection.notes", settingColNotes.getText().toString())
                            .putString("mongoDB.collection.key", settingColKey.getText().toString())
                            .commit();
                    if (!mongoDbAuth()) {
                        sharedPrefMongoDb.edit()
                                .putString("mongoDB.auth.api.key", "")
                                .putString("mongoDB.appId", "")
                                .putString("mongoDB.client", "")
                                .putString("mongoDB.database", "")
                                .putString("mongoDB.collection.notes", "")
                                .putString("mongoDB.collection.key", "")
                                .commit();
                    } else {
                        Toast.makeText(getApplicationContext(), "Setting updated \r\nAuthenticating Now..", Toast.LENGTH_SHORT).show();
                        alertDialog.dismiss();
                    }
                }
            }
        });
        Button settingClearBtn = popupView.findViewById(R.id.settingClearBtn);
        settingClearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settingMongoApiKey.getText().clear();
                settingMongoApiKey.setBackgroundResource(R.drawable.view_underline);
                settingMongoAppId.getText().clear();
                settingMongoAppId.setBackgroundResource(R.drawable.view_underline);
                settingMongoClient.getText().clear();
                settingMongoClient.setBackgroundResource(R.drawable.view_underline);
                settingMongoDatabase.getText().clear();
                settingMongoDatabase.setBackgroundResource(R.drawable.view_underline);
                settingColNotes.getText().clear();
                settingColNotes.setBackgroundResource(R.drawable.view_underline);
                settingColKey.getText().clear();
                settingColKey.setBackgroundResource(R.drawable.view_underline);
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

    private void setupFirebaseSetting() {
        View popupView = inflater.inflate(R.layout.menu_app_setting_popup_firebase, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setView(popupView);
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alertDialog.getWindow().getAttributes().windowAnimations = R.style.customDialogInOutAnimation;
        alertDialog.show();
        TextInputEditText settingFirebaseProjectId = popupView.findViewById(R.id.settingFirebaseProjectId);
        settingFirebaseProjectId.setText(sharedPrefFirebase.getString("settingFirebaseProjectId", ""));
        TextInputEditText settingFirebaseAppId = popupView.findViewById(R.id.settingFirebaseAppId);
        settingFirebaseAppId.setText(sharedPrefFirebase.getString("settingFirebaseAppId", ""));
        TextInputEditText settingFirebaseApiKey = popupView.findViewById(R.id.settingFirebaseApiKey);
        settingFirebaseApiKey.setText(sharedPrefFirebase.getString("settingFirebaseApiKey", ""));
        TextInputEditText settingFirebaseInstanceUrl = popupView.findViewById(R.id.settingFirebaseInstanceUrl);
        settingFirebaseInstanceUrl.setText(sharedPrefFirebase.getString("settingFirebaseInstanceUrl", ""));
        TextInputEditText settingChildNotes = popupView.findViewById(R.id.settingChildNotes);
        settingChildNotes.setText(sharedPrefFirebase.getString("settingChildNotes", ""));
        TextInputEditText settingChildKey = popupView.findViewById(R.id.settingChildKey);
        settingChildKey.setText(sharedPrefFirebase.getString("settingChildKey", ""));
        TextView settingPrompt = popupView.findViewById(R.id.settingPrompt);
        Button settingSaveBtn = popupView.findViewById(R.id.settingSaveBtn);
        settingSaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean validate = true;
                if (settingFirebaseProjectId.getText().toString().isEmpty()) {
                    settingFirebaseProjectId.setBackgroundResource(R.drawable.textinput_surface_yellow);
                    validate = false;
                }
                if (settingFirebaseAppId.getText().toString().isEmpty()) {
                    settingFirebaseAppId.setBackgroundResource(R.drawable.textinput_surface_yellow);
                    validate = false;
                }
                if (settingFirebaseApiKey.getText().toString().isEmpty()) {
                    settingFirebaseApiKey.setBackgroundResource(R.drawable.textinput_surface_yellow);
                    validate = false;
                }
                if (settingFirebaseInstanceUrl.getText().toString().isEmpty()) {
                    settingFirebaseInstanceUrl.setBackgroundResource(R.drawable.textinput_surface_yellow);
                    validate = false;
                }
                if (settingChildNotes.getText().toString().isEmpty()) {
                    settingChildNotes.setBackgroundResource(R.drawable.textinput_surface_yellow);
                    validate = false;
                }
                if (settingChildKey.getText().toString().isEmpty()) {
                    settingChildKey.setBackgroundResource(R.drawable.textinput_surface_yellow);
                    validate = false;
                }
                if (!validate) {
                    settingPrompt.setVisibility(View.VISIBLE);
                } else {
                    settingPrompt.setVisibility(View.INVISIBLE);
                    sharedPrefFirebase.edit()
                            .putString("settingFirebaseProjectId", settingFirebaseProjectId.getText().toString())
                            .putString("settingFirebaseAppId", settingFirebaseAppId.getText().toString())
                            .putString("settingFirebaseApiKey", settingFirebaseApiKey.getText().toString())
                            .putString("settingFirebaseInstanceUrl", settingFirebaseInstanceUrl.getText().toString())
                            .putString("settingChildNotes", settingChildNotes.getText().toString())
                            .putString("settingChildKey", settingChildKey.getText().toString())
                            .putBoolean("isCompleted", true)
                            .commit();
                    Toast.makeText(getApplicationContext(), "Setting updated.", Toast.LENGTH_SHORT).show();
                    alertDialog.dismiss();
                }
            }
        });
        Button settingClearBtn = popupView.findViewById(R.id.settingClearBtn);
        settingClearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settingFirebaseProjectId.getText().clear();
                settingFirebaseProjectId.setBackgroundResource(R.drawable.view_underline);
                settingFirebaseAppId.getText().clear();
                settingFirebaseAppId.setBackgroundResource(R.drawable.view_underline);
                settingFirebaseApiKey.getText().clear();
                settingFirebaseApiKey.setBackgroundResource(R.drawable.view_underline);
                settingFirebaseInstanceUrl.getText().clear();
                settingFirebaseInstanceUrl.setBackgroundResource(R.drawable.view_underline);
                settingChildNotes.getText().clear();
                settingChildNotes.setBackgroundResource(R.drawable.view_underline);
                settingChildKey.getText().clear();
                settingChildKey.setBackgroundResource(R.drawable.view_underline);
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

    private void setupGoogleApiSetting() {
        View popupView = inflater.inflate(R.layout.menu_app_setting_popup_google_api, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
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
                            .commit();
                    Toast.makeText(getApplicationContext(), "Setting updated.", Toast.LENGTH_SHORT).show();
                    alertDialog.dismiss();
                }
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

    private void getGoogleNewsRss(String arg) {
        commonProgressBar = (ProgressBar) findViewById(R.id.commonProgressBar);
        commonProgressBar.setVisibility(View.VISIBLE);
        googleNewsRssModelObjectList = new ArrayList<GoogleNewsRssModelObject>();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(appProperties.getProperty("google.news.rss"));
                    if (arg != null && !arg.isEmpty()) {
                        url = new URL(appProperties.getProperty("google.news.rss.search") + arg);
                    }
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setReadTimeout(10000);
                    conn.setConnectTimeout(15000);
                    conn.setRequestMethod("GET");
                    conn.setDoInput(true);
                    conn.connect();

                    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                    factory.setNamespaceAware(false);
                    XmlPullParser xpp = factory.newPullParser();
                    xpp.setInput(conn.getInputStream(),null);

                    boolean insideItem = false;
                    int eventType = xpp.getEventType();
                    GoogleNewsRssModelObject googleNewsRssModelObject = new GoogleNewsRssModelObject();
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            if (xpp.getName().equalsIgnoreCase("item")) {
                                insideItem = true;
                            } else if (xpp.getName().equalsIgnoreCase("link")) {
                                if (insideItem) googleNewsRssModelObject.setLink(xpp.nextText());
                            } else if (xpp.getName().equalsIgnoreCase("pubDate")) {
                                if (insideItem) {
                                    try {
                                        Date fromDate = fromFmt.parse(xpp.nextText());
                                        Calendar calendar = Calendar.getInstance();
                                        calendar.setTime(fromDate);
                                        calendar.add(Calendar.HOUR, 8);
                                        googleNewsRssModelObject.setPubDateStr(toFmt.format(calendar.getTime()));
                                    } catch (Exception e) {
                                        googleNewsRssModelObject.setPubDateStr(xpp.nextText());
                                    }
                                }
                            } else if (xpp.getName().equalsIgnoreCase("title")) {
                                if (insideItem) googleNewsRssModelObject.setTitle(xpp.nextText());
                            }
                        } else if (eventType == XmlPullParser.END_TAG && xpp.getName().equalsIgnoreCase("item")) {
                            insideItem = false;
                            googleNewsRssModelObjectList.add(googleNewsRssModelObject);
                            googleNewsRssModelObject = new GoogleNewsRssModelObject();
                        }
                        eventType = xpp.next();
                    }
                    conn.disconnect();
                    new Handler(Looper.getMainLooper()).post(new Runnable(){
                        @Override
                        public void run() {
                            googleNewsLayoutSetup();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void googleNewsLayoutSetup() {
        TextInputEditText newsSearchInput = (TextInputEditText) findViewById(R.id.newsSearchInput);
        newsSearchInput.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && i == KeyEvent.KEYCODE_ENTER) {
                    newsCardList.setVisibility(View.INVISIBLE);
                    getGoogleNewsRss(newsSearchInput.getText().toString());
                    Toast.makeText(getApplicationContext(),"Searching..", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        });
        newsPagerRefresh = (SwipeRefreshLayout) findViewById(R.id.newsPagerRefresh);
        newsPagerRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (newsSearchInput.getText().toString() == null || newsSearchInput.getText().toString().isEmpty()) {
                    newsCardList.setVisibility(View.INVISIBLE);
                    getGoogleNewsRss(null);
                } else {
                    newsCardList.setVisibility(View.INVISIBLE);
                    getGoogleNewsRss(newsSearchInput.getText().toString());
                }
                newsPagerRefresh.setRefreshing(false);
                Toast.makeText(getApplicationContext(),"F5", Toast.LENGTH_SHORT).show();
            }
        });
        newsCardList = (LinearLayout) findViewById(R.id.cardList);
        newsCardList.removeAllViews();
        for (GoogleNewsRssModelObject googleNewsRssModelObject : googleNewsRssModelObjectList) {
            CardView newsCard = (CardView) inflater.inflate(R.layout.menu_news_rss_card, newsCardList, false);
            TextView newsTitle = (TextView) newsCard.findViewById(R.id.newsTitle);
            newsTitle.setText(googleNewsRssModelObject.getTitle());
            TextView newsDate = (TextView) newsCard.findViewById(R.id.newsDate);
            newsDate.setText(googleNewsRssModelObject.getPubDateStr());
            newsCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(MainActivity.this, NormalWebViewActivity.class);
                    intent.putExtra("url", googleNewsRssModelObject.getLink());
                    startActivity(intent);
                }
            });
            newsCardList.addView(newsCard);
        }
        commonProgressBar.setVisibility(View.GONE);
        newsCardList.setVisibility(View.VISIBLE);
    }

    private void weatherObjectLayoutSetup() {
        //get weather forecast json response
        if (sharedPrefWeather.getBoolean("isWeatherEn", true)) {
            url = appProperties.getProperty("weather.forecast.en.api");
        } else {
            url = appProperties.getProperty("weather.forecast.tc.api");
        }
        commonProgressBar = (ProgressBar) findViewById(R.id.commonProgressBar);
        commonProgressBar.setVisibility(View.VISIBLE);
        try {
            getJsonResponse(new WeatherCallback() {
                @Override
                public void onSuccess(String result) {
                    //setup object
                    JsonObject jsonObject = JsonParser.parseString(result).getAsJsonObject();
                    weatherObject = new Gson().fromJson(jsonObject, WeatherObject.class);

                    //layout update
                    TextView generalSituation = (TextView) menuViewPager.findViewById(R.id.generalSituation);
                    generalSituation.setText(weatherObject.getGeneralSituation());
                    TextView updateTime = (TextView) menuViewPager.findViewById(R.id.updateTime);
                    updateTime.setText(weatherObject.getUpdateTime());
                    //setup weather forecast grid
                    GridView weatherForecastGrid = (GridView) menuViewPager.findViewById(R.id.weatherForecastGrid);
                    MenuWeatherGridAdapter menuWeatherGridAdapter = new MenuWeatherGridAdapter(getApplicationContext(), weatherObject.getWeatherForecast());
                    weatherForecastGrid.setAdapter(menuWeatherGridAdapter);
                    //setup swipeRefreshLayout
                    weatherPagerRefresh = menuViewPager.findViewById(R.id.weatherPagerRefresh);
                    weatherPagerRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                        @Override
                        public void onRefresh() {
                            weatherObjectLayoutSetup();
                            weatherPagerRefresh.setRefreshing(false);
                            Toast.makeText(getApplicationContext(),"F5", Toast.LENGTH_SHORT).show();
                        }
                    });
                    //make swipeRefresh affect when gridview scrolled to the top element only
                    weatherForecastGrid.setOnScrollListener(new AbsListView.OnScrollListener() {
                        @Override
                        public void onScrollStateChanged(AbsListView absListView, int i) {
                        }
                        @Override
                        public void onScroll(AbsListView absListView, int i, int i1, int i2) {
                            if (i == 0) {
                                try {
                                    View fullyShown = weatherForecastGrid.getChildAt(0);
//                                    float density = getResources().getDisplayMetrics().density;
                                    if (fullyShown != null && fullyShown.getTop() == fullyShown.getPaddingTop()) {
                                        weatherPagerRefresh.setEnabled(true);
                                    } else {
                                        weatherPagerRefresh.setEnabled(false);
                                    }
                                } catch(Exception e) {
                                    weatherPagerRefresh.setEnabled(false);
                                }
                            } else {
                                weatherPagerRefresh.setEnabled(false);
                            }
                        }
                    });
                    commonProgressBar.setVisibility(View.GONE);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),"Error :" + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private void getJsonResponse(final WeatherCallback callback) {
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                callback.onSuccess(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError e) {
                Toast.makeText(getApplicationContext(),"Error :" + e, Toast.LENGTH_LONG).show();
            }
        });
        queue.add(stringRequest);
    }

    private boolean mongoDbAuth() {
        try {
            Realm.init(this);
            if (sharedPrefMongoDb.getString("mongoDB.appId", "").isEmpty() ||
                    sharedPrefMongoDb.getString("mongoDB.auth.api.key", "").isEmpty()) {
                Toast.makeText(getApplicationContext(),"No Setting Found. \r\nPlease config App Setting", Toast.LENGTH_LONG).show();
                sharedPrefMongoDb.edit().putBoolean("isAuthenticated", false).commit();
                return false;
            }
            String appID = sharedPrefMongoDb.getString("mongoDB.appId", "");
            mongoApp = new App(new AppConfiguration.Builder(appID).build());
            Credentials apiKeyCredentials = Credentials.apiKey(sharedPrefMongoDb.getString("mongoDB.auth.api.key", ""));
            AtomicReference<User> user = new AtomicReference<User>();
            mongoApp.loginAsync(apiKeyCredentials, it -> {
                if (it.isSuccess()) {
                    user.set(mongoApp.currentUser());
                    Toast.makeText(getApplicationContext(),"Authenticated by API key", Toast.LENGTH_SHORT).show();
                    Log.v("AUTH", "Successfully authenticated using an API Key.");
                    sharedPrefMongoDb.edit().putBoolean("isAuthenticated", true).commit();
                    fabJotNotes.setEnabled(true);
                } else {
                    Log.e("AUTH", it.getError().toString());
                    Toast.makeText(getApplicationContext(),"Authenticate Error: \r\n" + it.getError().toString(), Toast.LENGTH_LONG).show();
                    sharedPrefMongoDb.edit().putBoolean("isAuthenticated", false).commit();
                    fabJotNotes.setEnabled(false);
                }
            });
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    //For back pressed twice to exit
    private static final int TIME_INTERVAL = 2000;
    private boolean doubleBackToExitPressedOnce;
    private Handler mHandler = new Handler();

    private final Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            doubleBackToExitPressedOnce = false;
        }
    };
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHandler != null) {
            mHandler.removeCallbacks(mRunnable);
        }
    }
    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Press BACK again to exit", Toast.LENGTH_SHORT).show();

        mHandler.postDelayed(mRunnable, TIME_INTERVAL);
    }
}
