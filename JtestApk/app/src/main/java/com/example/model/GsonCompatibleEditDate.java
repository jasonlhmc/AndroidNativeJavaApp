package com.example.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class GsonCompatibleEditDate {
    @SerializedName("$EditDate")
    public Long editDate;

    public GsonCompatibleEditDate(Long date) {
        this.editDate = date;
    }

    public Date getDate() {
        return new Date(editDate);
    }

    public void setDate(Date date) {
        this.editDate = date.getTime();
    }

    @Override
    public String toString() {
        return "GsonCompatibleDate{" +
                "editDate=" + editDate +
                '}';
    }
}
