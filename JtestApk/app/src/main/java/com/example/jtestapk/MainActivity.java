package com.example.jtestapk;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.adapter.MenuWeatherGridAdapter;
import com.example.model.WeatherObject;
import com.example.utils.CustomAnimationUtils;
import com.example.utils.PropertiesUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
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

    private boolean isFabVisable;
    private boolean isWeatherEn;
    private boolean isAuthenticated;
    private String url;

    private ViewPager2 viewPager2;
    private FloatingActionButton fabJotNotes;

    private App mongoApp;

    LayoutInflater inflater;

    CustomAnimationUtils customAnimationUtils = new CustomAnimationUtils();

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
        weatherLayoutSetup();

        //init inflater
        inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        //setup viewPager2
//        viewPager2 = findViewById(R.id.viewpager);
//        MenuViewPagerAdapter menuViewPagerAdapter = new MenuViewPagerAdapter(this);
//        viewPager2.setAdapter(menuViewPagerAdapter);
//        viewPager2.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
//        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
//            @Override
//            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
//            }
//            @Override
//            public void onPageSelected(int position) {
//                super.onPageSelected(position);
//            }
//            @Override
//            public void onPageScrollStateChanged(int state) {
//                super.onPageScrollStateChanged(state);
//            }
//        });

        //init Floating Action Buttons
        FloatingActionButton fabCheckCur = findViewById(R.id.fabCheckCur);
        TextView fabTextCheckCur = findViewById(R.id.fabTextCheckCur);
        FloatingActionButton fabQrCode = findViewById(R.id.fabQrCode);
        TextView fabTextQrCode = findViewById(R.id.fabTextQrCode);
        fabJotNotes = findViewById(R.id.fabJotNotes);
        TextView fabTextJotNotes = findViewById(R.id.fabTextJotNotes);
        FloatingActionButton fabWeatherTranslate = findViewById(R.id.fabWeatherTranslate);
        TextView fabTextWeatherTranslate = findViewById(R.id.fabTextWeatherTranslate);
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
                    weatherLayoutSetup();
                } else {
                    isWeatherEn = true;
                    sharedPrefWeather.edit().putBoolean("isWeatherEn", true).commit();
                    weatherLayoutSetup();
                }
            }
        });
        fabAppSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v("INFO", "App Setting button CLICKED");
                View popupView = inflater.inflate(R.layout.menu_app_setting_popup, null);
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
        });

        //get MongoDB Auth for the first time running this App
        sharedPrefMongoDb = getSharedPreferences("MongoDb", MODE_PRIVATE);
        isAuthenticated = sharedPrefMongoDb.getBoolean("isAuthenticated", false);
        if (!isAuthenticated) {
            if (!mongoDbAuth()) {
                fabJotNotes.setEnabled(false);
            }
        }

    }

    private void weatherLayoutSetup() {
        //get weather forecast json resoponse
        if (sharedPrefWeather.getBoolean("isWeatherEn", true)) {
            url = appProperties.getProperty("weather.forecast.en.api");
        } else {
            url = appProperties.getProperty("weather.forecast.tc.api");
        }
        try {
            getJsonResponse(new WeatherCallback() {
                @Override
                public void onSuccess(String result) {
                    //setup object
                    JsonObject jsonObject = JsonParser.parseString(result).getAsJsonObject();
                    WeatherObject weatherObject = new Gson().fromJson(jsonObject, WeatherObject.class);

                    //layout update
                    TextView generalSituation = (TextView) findViewById(R.id.generalSituation);
                    generalSituation.setText(weatherObject.getGeneralSituation());
                    TextView updateTime = (TextView) findViewById(R.id.updateTime);
                    updateTime.setText(weatherObject.getUpdateTime());
                    //setup weather forecast grid
                    GridView weatherForecastGrid = (GridView) findViewById(R.id.weatherForecastGrid);
                    MenuWeatherGridAdapter menuWeatherGridAdapter = new MenuWeatherGridAdapter(getApplicationContext(), weatherObject.getWeatherForecast());
                    weatherForecastGrid.setAdapter(menuWeatherGridAdapter);
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
