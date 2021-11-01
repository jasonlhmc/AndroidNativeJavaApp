package com.example.model;

public class DiceModelObject {

    private int diceId;
    private int diceNumberInt;
    private String diceNumberStr;
    private boolean isSelected;
    private boolean isRolled;
    private boolean isLocked;

    public int getDiceId() {
        return diceId;
    }

    public void setDiceId(int diceId) {
        this.diceId = diceId;
    }

    public int getDiceNumberInt() {
        return diceNumberInt;
    }

    public void setDiceNumberInt(int diceNumberInt) {
        this.diceNumberInt = diceNumberInt;
    }

    public String getDiceNumberStr() {
        return diceNumberStr;
    }

    public void setDiceNumberStr(String diceNumberStr) {
        this.diceNumberStr = diceNumberStr;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public boolean isRolled() {
        return isRolled;
    }

    public void setRolled(boolean rolled) {
        isRolled = rolled;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    @Override
    public String toString() {
        return "DiceModelObject{" +
                "diceId=" + diceId +
                ", diceNumberInt=" + diceNumberInt +
                ", diceNumberStr='" + diceNumberStr + '\'' +
                ", isSelected=" + isSelected +
                ", isRolled=" + isRolled +
                ", isLocked=" + isLocked +
                '}';
    }

    public String getDiceNumber() {
        return "diceNumberInt=" + diceNumberInt;
    }
}
