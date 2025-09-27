package com.example.smarthotelapp;

public class Task {
    private int id;
    private String title;
    private String description;
    private String status;
    private String createdAt;
    private String updatedAt;
    private String statusText;
    private String statusColor;
    private String priorityLevel;

    // Constructor
    public Task() {
    }

    public Task(int id, String title, String description, String status, 
                String createdAt, String updatedAt, String statusText, 
                String statusColor, String priorityLevel) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.statusText = statusText;
        this.statusColor = statusColor;
        this.priorityLevel = priorityLevel;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public String getStatusText() {
        return statusText;
    }

    public String getStatusColor() {
        return statusColor;
    }

    public String getPriorityLevel() {
        return priorityLevel;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public void setStatusColor(String statusColor) {
        this.statusColor = statusColor;
    }

    public void setPriorityLevel(String priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    // Helper methods
    public int getStatusColorInt() {
        try {
            return android.graphics.Color.parseColor(statusColor);
        } catch (Exception e) {
            return android.graphics.Color.parseColor("#9E9E9E"); // Default grey
        }
    }

    public String getFormattedDate() {
        if (createdAt != null && createdAt.length() >= 10) {
            return createdAt.substring(0, 10); // Return YYYY-MM-DD format
        }
        return createdAt;
    }

    public boolean isPending() {
        return "pending".equals(status);
    }

    public boolean isInProgress() {
        return "in_progress".equals(status);
    }

    public boolean isDone() {
        return "done".equals(status);
    }
}
