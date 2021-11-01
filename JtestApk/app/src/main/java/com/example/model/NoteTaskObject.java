package com.example.model;

public class NoteTaskObject {
    boolean isFinished;
    String task;

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean finished) {
        isFinished = finished;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    @Override
    public String toString() {
        return "NoteTaskObject{" +
                "isFinished=" + isFinished +
                ", task='" + task + '\'' +
                '}';
    }
}
