package com.example.smarthotelapp;

import java.util.ArrayList;
import java.util.List;

public class RoomDetail {
    public int id;
    public int roomId;
    public String roomNumber = "";
    public String roomType = "";
    public int floor;
    public String view = "";
    public double pricePerNight;
    public List<String> features = new ArrayList<>();
    public String capacity = "";
    public int maxOccupancy;
    public String status = "";
    public String statusColor = "#E8F5E8";
    public boolean isAvailable;
    public String cardColor = "#F5F5F5";
}
