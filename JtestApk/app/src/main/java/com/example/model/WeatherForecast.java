package com.example.model;

import java.util.Arrays;

public class WeatherForecast {

    private String forecastDate;
    private String week;
    private String forecastWind;
    private String forecastWeather;
    private ForecastValue forecastMaxtemp;
    private ForecastValue forecastMintemp;
    private ForecastValue forecastMaxrh;
    private ForecastValue forecastMinrh;
    private String ForecastIcon;
    private String PSR;

    public String getForecastDate() {
        return forecastDate;
    }

    public void setForecastDate(String forecastDate) {
        this.forecastDate = forecastDate;
    }

    public String getWeek() {
        return week;
    }

    public void setWeek(String week) {
        this.week = week;
    }

    public String getForecastWind() {
        return forecastWind;
    }

    public void setForecastWind(String forecastWind) {
        this.forecastWind = forecastWind;
    }

    public String getForecastWeather() {
        return forecastWeather;
    }

    public void setForecastWeather(String forecastWeather) {
        this.forecastWeather = forecastWeather;
    }

    public ForecastValue getForecastMaxtemp() {
        return forecastMaxtemp;
    }

    public void setForecastMaxtemp(ForecastValue forecastMaxtemp) {
        this.forecastMaxtemp = forecastMaxtemp;
    }

    public ForecastValue getForecastMintemp() {
        return forecastMintemp;
    }

    public void setForecastMintemp(ForecastValue forecastMintemp) {
        this.forecastMintemp = forecastMintemp;
    }

    public ForecastValue getForecastMaxrh() {
        return forecastMaxrh;
    }

    public void setForecastMaxrh(ForecastValue forecastMaxrh) {
        this.forecastMaxrh = forecastMaxrh;
    }

    public ForecastValue getForecastMinrh() {
        return forecastMinrh;
    }

    public void setForecastMinrh(ForecastValue forecastMinrh) {
        this.forecastMinrh = forecastMinrh;
    }

    public String getForecastIcon() {
        return ForecastIcon;
    }

    public void setForecastIcon(String forecastIcon) {
        ForecastIcon = forecastIcon;
    }

    public String getPSR() {
        return PSR;
    }

    public void setPSR(String PSR) {
        this.PSR = PSR;
    }

    @Override
    public String toString() {
        return "WeatherForecast{" +
                "forecastDate='" + forecastDate + '\'' +
                ", week='" + week + '\'' +
                ", forecastWind='" + forecastWind + '\'' +
                ", forecastWeather='" + forecastWeather + '\'' +
                ", forecastMaxtemp=" + forecastMaxtemp +
                ", forecastMintemp=" + forecastMintemp +
                ", forecastMaxrh=" + forecastMaxrh +
                ", forecastMinrh=" + forecastMinrh +
                ", ForecastIcon='" + ForecastIcon + '\'' +
                ", PSR='" + PSR + '\'' +
                '}';
    }
}
