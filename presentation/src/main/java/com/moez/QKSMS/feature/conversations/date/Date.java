package com.moez.QKSMS.feature.conversations.date;

public class Date {
    private String dateString;
    private long date;

    public Date(String dateString, long date) {
        this.dateString = dateString;
        this.date = date;
    }

    public String getDateString() {
        return dateString;
    }

    public void setDateString(String dateString) {
        this.dateString = dateString;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }
}
