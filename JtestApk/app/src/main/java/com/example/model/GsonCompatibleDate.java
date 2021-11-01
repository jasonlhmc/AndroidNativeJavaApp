package com.example.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class GsonCompatibleDate {
    @SerializedName("$date")
    public Long date;

    public GsonCompatibleDate(Long date) {
        this.date = date;
    }

    public Date getDate() {
        return new Date(date);
    }

    public void setDate(Date date) {
        this.date = date.getTime();
    }

    @Override
    public String toString() {
        return "GsonCompatibleDate{" +
                "date=" + date +
                '}';
    }
}
