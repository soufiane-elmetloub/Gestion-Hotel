package com.example.smarthotelapp.models;

public class Notification {
    private int id;
    private String title;
    private String message;
    private String time;
    private boolean isRead;

    public Notification(int id, String title, String message, String time) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.time = time;
        this.isRead = false;
    }

    public Notification(int id, String title, String message) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.time = "Just now";
        this.isRead = false;
    }

    public Notification(int id, String title, String message, String time, String type) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.time = time;
        this.isRead = false;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}
