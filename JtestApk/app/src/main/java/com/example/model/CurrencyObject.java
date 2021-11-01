package com.example.model;

public class CurrencyObject {

    private String flag;
    private String country;
    private String curName;
    private String curCode;
    private String symbol;
    private String source;
    private String localeCur;
    private double localeRate;
    private double roundedRate;
    private double rate;
    private double originalRate;
    private double amountRate;

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCurName() {
        return curName;
    }

    public void setCurName(String curName) {
        this.curName = curName;
    }

    public String getCurCode() {
        return curCode;
    }

    public void setCurCode(String curCode) {
        this.curCode = curCode;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getLocaleCur() {
        return localeCur;
    }

    public void setLocaleCur(String localeCur) {
        this.localeCur = localeCur;
    }

    public double getLocaleRate() {
        return localeRate;
    }

    public void setLocaleRate(double localeRate) {
        this.localeRate = localeRate;
    }

    public double getRoundedRate() {
        return roundedRate;
    }

    public void setRoundedRate(double roundedRate) {
        this.roundedRate = roundedRate;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public double getOriginalRate() {
        return originalRate;
    }

    public void setOriginalRate(double originalRate) {
        this.originalRate = originalRate;
    }

    public double getAmountRate() {
        return amountRate;
    }

    public void setAmountRate(double amountRate) {
        this.amountRate = amountRate;
    }

    @Override
    public String toString() {
        return "CurrencyObject{" +
                "flag='" + flag + '\'' +
                ", country='" + country + '\'' +
                ", curName='" + curName + '\'' +
                ", curCode='" + curCode + '\'' +
                ", symbol='" + symbol + '\'' +
                ", source='" + source + '\'' +
                ", localeCur='" + localeCur + '\'' +
                ", localeRate=" + localeRate +
                ", roundedRate=" + roundedRate +
                ", rate=" + rate +
                ", originalRate=" + originalRate +
                ", amountRate=" + amountRate +
                '}';
    }
}
