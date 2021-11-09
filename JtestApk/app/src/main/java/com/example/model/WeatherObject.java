package com.example.model;

import java.util.List;

public class WeatherObject {

    private String generalSituation;
    private List<WeatherForecast> weatherForecast;
    private String updateTime;

    public String getGeneralSituation() {
        return generalSituation;
    }

    public void setGeneralSituation(String generalSituation) {
        this.generalSituation = generalSituation;
    }

    public List<WeatherForecast> getWeatherForecast() {
        return weatherForecast;
    }

    public void setWeatherForecast(List<WeatherForecast> weatherForecast) {
        this.weatherForecast = weatherForecast;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "WeatherObject{" +
                "generalSituation='" + generalSituation + '\'' +
                ", weatherForecast=" + weatherForecast +
                ", updateTime='" + updateTime + '\'' +
                '}';
    }

}
