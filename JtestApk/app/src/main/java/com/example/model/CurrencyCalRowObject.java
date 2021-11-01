package com.example.model;

public class CurrencyCalRowObject {

    private String curCode;
    private int rowTextViewId;
    private int rowInputTextId;

    public String getCurCode() {
        return curCode;
    }

    public void setCurCode(String curCode) {
        this.curCode = curCode;
    }

    public int getRowTextViewId() {
        return rowTextViewId;
    }

    public void setRowTextViewId(int rowTextViewId) {
        this.rowTextViewId = rowTextViewId;
    }

    public int getRowInputTextId() {
        return rowInputTextId;
    }

    public void setRowInputTextId(int rowInputTextId) {
        this.rowInputTextId = rowInputTextId;
    }

    @Override
    public String toString() {
        return "CurrencyCalRowObject{" +
                "curName='" + curCode + '\'' +
                ", rowTextViewId=" + rowTextViewId +
                ", rowInputTextId=" + rowInputTextId +
                '}';
    }
}
