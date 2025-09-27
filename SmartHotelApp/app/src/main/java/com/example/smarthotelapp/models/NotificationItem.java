package com.example.smarthotelapp.models;

public class NotificationItem {
    public int id;
    public int employeeId;
    public String title;
    public String message;
    public boolean isRead;
    public String createdAt;

    public NotificationItem(int id, int employeeId, String title, String message, boolean isRead, String createdAt) {
        this.id = id;
        this.employeeId = employeeId;
        this.title = title;
        this.message = message;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }
}
