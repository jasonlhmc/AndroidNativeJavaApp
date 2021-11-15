package com.example.model;

import android.graphics.Bitmap;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class NoteModelObject {

    private String content;
    private GsonCompatibleDate date;
    private GsonCompatibleEditDate editDate;
    private String dateStr;
    private String editDateStr;
    private String title;
    private List<String> linkQuery;
    private List<String> linkedNotes;
    private List<String> linkedPaints;
    private List<String> embeddedPaints;
    private boolean isPaint;
    private boolean isFPLock;
    private boolean isPinLock;
    private String encryptedPin;
    private boolean isTask;
    private List<NoteTaskObject> taskList;
    private Bitmap gridViewPaint;

    public NoteModelObject() {

    }

    public NoteModelObject(String content,
                           String dateStr,
                           String editDateStr,
                           String title,
                           boolean isPaint,
                           boolean isFPLock,
                           boolean isPinLock,
                           String encryptedPin,
                           boolean isTask,
                           List<NoteTaskObject> taskList) {
        this.content = content;
        this.dateStr = dateStr;
        this.editDateStr = editDateStr;
        this.title = title;
        this.isPaint = isPaint;
        this.isFPLock = isFPLock;
        this.isPinLock = isPinLock;
        this.encryptedPin = encryptedPin;
        this.isTask = isTask;
        this.taskList = taskList;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public GsonCompatibleDate getDate() {
        return date;
    }

    public void setDate(GsonCompatibleDate date) {
        this.date = date;
    }

    public GsonCompatibleEditDate getEditDate() {
        return editDate;
    }

    public void setEditDate(GsonCompatibleEditDate editDate) {
        this.editDate = editDate;
    }

    public String getDateStr() {
        return dateStr;
    }

    public void setDateStr(String dateStr) {
        this.dateStr = dateStr;
    }

    public String getEditDateStr() {
        return editDateStr;
    }

    public void setEditDateStr(String editDateStr) {
        this.editDateStr = editDateStr;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getLinkQuery() {
        return linkQuery;
    }

    public void setLinkQuery(List<String> linkQuery) {
        this.linkQuery = linkQuery;
    }

    public List<String> getLinkedNotes() {
        return linkedNotes;
    }

    public void setLinkedNotes(List<String> linkedNotes) {
        this.linkedNotes = linkedNotes;
    }

    public List<String> getLinkedPaints() {
        return linkedPaints;
    }

    public void setLinkedPaints(List<String> linkedPaints) {
        this.linkedPaints = linkedPaints;
    }

    public List<String> getEmbeddedPaints() {
        return embeddedPaints;
    }

    public void setEmbeddedPaints(List<String> embeddedPaints) {
        this.embeddedPaints = embeddedPaints;
    }

    public boolean isPaint() {
        return isPaint;
    }

    public void setPaint(boolean paint) {
        isPaint = paint;
    }

    public boolean isFPLock() {
        return isFPLock;
    }

    public void setFPLock(boolean FPLock) {
        isFPLock = FPLock;
    }

    public boolean isPinLock() {
        return isPinLock;
    }

    public void setPinLock(boolean pinLock) {
        isPinLock = pinLock;
    }

    public String getEncryptedPin() {
        return encryptedPin;
    }

    public void setEncryptedPin(String encryptedPin) {
        this.encryptedPin = encryptedPin;
    }

    public boolean isTask() {
        return isTask;
    }

    public void setTask(boolean task) {
        isTask = task;
    }

    public List<NoteTaskObject> getTaskList() {
        return taskList;
    }

    public void setTaskList(List<NoteTaskObject> taskList) {
        this.taskList = taskList;
    }

    public Bitmap getGridViewPaint() {
        return gridViewPaint;
    }

    public void setGridViewPaint(Bitmap gridViewPaint) {
        this.gridViewPaint = gridViewPaint;
    }

    @Override
    public String toString() {
        return "NoteModelObject{" +
                "content='" + content + '\'' +
                ", date=" + date +
                ", editDate=" + editDate +
                ", dateStr='" + dateStr + '\'' +
                ", editDateStr='" + editDateStr + '\'' +
                ", title='" + title + '\'' +
                ", linkQuery=" + linkQuery +
                ", linkedNotes=" + linkedNotes +
                ", linkedPaints=" + linkedPaints +
                ", embeddedPaints=" + embeddedPaints +
                ", isPaint=" + isPaint +
                ", isFPLock=" + isFPLock +
                ", isPinLock=" + isPinLock +
                ", encryptedPin='" + encryptedPin + '\'' +
                ", isTask=" + isTask +
                ", taskList=" + taskList +
                ", gridViewPaint=" + gridViewPaint +
                '}';
    }
}
