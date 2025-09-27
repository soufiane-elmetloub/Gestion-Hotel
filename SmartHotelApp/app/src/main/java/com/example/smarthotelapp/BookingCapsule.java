package com.example.smarthotelapp;

public class BookingCapsule {
    public int id;
    public String firstName;
    public String lastName;
    public String checkIn;      // yyyy-MM-dd
    public String checkOut;     // yyyy-MM-dd
    public String roomNumber;   // as string
    public int numberOfGuests;  // number of guests
    public String status;       // reserved, checked_in, checked_out, cancelled
    public String paymentStatus;// pending, paid, partial
    public String totalAmount;  // as string

    public String fullName() {
        String f = firstName == null ? "" : firstName;
        String l = lastName == null ? "" : lastName;
        return (f + " " + l).trim();
    }

    public String initials() {
        String f = (firstName == null || firstName.isEmpty()) ? "" : firstName.substring(0,1).toUpperCase();
        String l = (lastName == null || lastName.isEmpty()) ? "" : lastName.substring(0,1).toUpperCase();
        String s = (f + l).trim();
        return s.isEmpty() ? "?" : s;
    }
}
