package com.example.smarthotelapp;

public class RevenueTableItem {
    public final String dateDM; // e.g., │17/08
    public final String daily;
    public final String weekly;
    public final String monthly;

    public RevenueTableItem(String dateDM, String daily, String weekly, String monthly) {
        this.dateDM = dateDM;
        this.daily = daily;
        this.weekly = weekly;
        this.monthly = monthly;
    }
}
