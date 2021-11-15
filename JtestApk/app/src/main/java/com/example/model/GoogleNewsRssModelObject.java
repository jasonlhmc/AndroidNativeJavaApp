package com.example.model;

import java.util.Date;

public class GoogleNewsRssModelObject {

    private String title;
    private String link;
    private String pubDateStr;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getPubDateStr() {
        return pubDateStr;
    }

    public void setPubDateStr(String pubDateStr) {
        this.pubDateStr = pubDateStr;
    }

    @Override
    public String toString() {
        return "GoogleNewsRssModelObject{" +
                "title='" + title + '\'' +
                ", link='" + link + '\'' +
                ", pubDateStr='" + pubDateStr + '\'' +
                '}';
    }
}
