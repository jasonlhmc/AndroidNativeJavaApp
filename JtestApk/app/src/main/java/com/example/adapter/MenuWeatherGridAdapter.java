package com.example.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.jtestapk.R;
import com.example.model.WeatherForecast;
import com.example.utils.CustomAnimationUtils;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;

public class MenuWeatherGridAdapter extends BaseAdapter {

    Context context;
    List<WeatherForecast> weatherForecastList;
    LayoutInflater inflater;

    private SharedPreferences sharedPrefWeather;
    private boolean isWeatherEn;

    CustomAnimationUtils customAnimationUtils = new CustomAnimationUtils();

    SimpleDateFormat fromFmt = new SimpleDateFormat("yyyyMMdd");
    SimpleDateFormat toFmt = new SimpleDateFormat("yyyy-MM-dd");

    public MenuWeatherGridAdapter(Context applicationContext, List<WeatherForecast> weatherForecastList) {
        this.context = applicationContext;
        this.weatherForecastList = weatherForecastList;
        inflater = (LayoutInflater.from(applicationContext));
    }

    @Override
    public int getCount() {
        return weatherForecastList.size();
    }

    @Override
    public Object getItem(int i) {
        return weatherForecastList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        sharedPrefWeather = context.getSharedPreferences("weatherSetting", Context.MODE_PRIVATE);
        isWeatherEn = sharedPrefWeather.getBoolean("isWeatherEn", true);
        WeatherForecast weatherForecast = weatherForecastList.get(i);
//        Log.v("WEATHER", "weatherForecast = " + weatherForecast.toString());
        view = inflater.inflate(R.layout.menu_weather_grid, viewGroup, false);
        TextView forecastWeather = view.findViewById(R.id.forecastWeather);
        forecastWeather.setText(weatherForecast.getForecastWeather());
        TextView forecastWind = view.findViewById(R.id.forecastWind);
        forecastWind.setText(weatherForecast.getForecastWind());
        TextView forecastDate = view.findViewById(R.id.forecastDate);
        try {
            if (isWeatherEn) {
                forecastDate.setText(toFmt.format(fromFmt.parse(weatherForecast.getForecastDate())) + "(" + weatherForecast.getWeek().substring(0, 3) + ")");
            } else {
                forecastDate.setText(toFmt.format(fromFmt.parse(weatherForecast.getForecastDate())) + "(" + weatherForecast.getWeek().substring(2, 3) + ")");
            }
        } catch (Exception e) {
            if (isWeatherEn) {
                forecastDate.setText(weatherForecast.getForecastDate() + "(" + weatherForecast.getWeek().substring(0, 3) + ")");
            } else {
                forecastDate.setText(weatherForecast.getForecastDate() + "(" + weatherForecast.getWeek().substring(2, 3) + ")");
            }
        }
        TextView forecastTemp = view.findViewById(R.id.forecastTemp);
        forecastTemp.setText(weatherForecast.getForecastMintemp().getValue() + "°C - " + weatherForecast.getForecastMaxtemp().getValue() + "°C");
        ImageView weatherImageView = view.findViewById(R.id.weatherImageView);
        String imageUrl = "https://www.hko.gov.hk/images/HKOWxIconOutline/pic" + weatherForecast.getForecastIcon() + ".png";
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(imageUrl);
//                    Log.v("IMAGE", "url = " + url);
                    Bitmap mIcon_val = BitmapFactory.decodeStream(url.openConnection() .getInputStream());
                    new Handler(Looper.getMainLooper()).post(new Runnable(){
                        @Override
                        public void run() {
                            weatherImageView.setImageBitmap(mIcon_val);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    new Handler(Looper.getMainLooper()).post(new Runnable(){
                        @Override
                        public void run() {
                            weatherImageView.setImageResource(R.drawable.ic_baseline_cancel_24);
                        }
                    });
                }
            }
        }).start();
        view.setAnimation(customAnimationUtils.fadeInAnimationDefault(context));
        return view;
    }
}
