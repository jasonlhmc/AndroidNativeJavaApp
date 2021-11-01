package com.example.model;

public class NoteTaskModelObject {
    private int rowId;
    private int toggleButtonId;
    private int taskInputId;
    private int cancelButtonId;

    public int getRowId() {
        return rowId;
    }

    public void setRowId(int rowId) {
        this.rowId = rowId;
    }

    public int getToggleButtonId() {
        return toggleButtonId;
    }

    public void setToggleButtonId(int toggleButtonId) {
        this.toggleButtonId = toggleButtonId;
    }

    public int getTaskInputId() {
        return taskInputId;
    }

    public void setTaskInputId(int taskInputId) {
        this.taskInputId = taskInputId;
    }

    public int getCancelButtonId() {
        return cancelButtonId;
    }

    public void setCancelButtonId(int cancelButtonId) {
        this.cancelButtonId = cancelButtonId;
    }

    @Override
    public String toString() {
        return "noteTaskModelObject{" +
                "rowId=" + rowId +
                ", toggleButtonId=" + toggleButtonId +
                ", taskInputId=" + taskInputId +
                ", cancelButtonId=" + cancelButtonId +
                '}';
    }
}
