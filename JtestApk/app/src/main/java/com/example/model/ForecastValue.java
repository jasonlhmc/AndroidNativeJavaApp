package com.example.model;

public class ForecastValue {

    private int value;
    private String unit;

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    @Override
    public String toString() {
        return "ForecastValue{" +
                "value=" + value +
                ", unit='" + unit + '\'' +
                '}';
    }
}
