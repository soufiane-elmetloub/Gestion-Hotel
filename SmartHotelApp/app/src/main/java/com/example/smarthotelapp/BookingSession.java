package com.example.smarthotelapp;

public class BookingSession {
    private static int adults;
    private static int children;
    private static int infants;
    private static int clientId = -1;

    public static synchronized void setCounts(int a, int c, int i) {
        adults = Math.max(0, a);
        children = Math.max(0, c);
        infants = Math.max(0, i);
    }

    public static synchronized int getAdults() {
        return adults;
    }

    public static synchronized int getChildren() {
        return children;
    }

    public static synchronized int getInfants() {
        return infants;
    }

    public static synchronized int getTotalGuests() {
        return adults + children + infants;
    }

    public static synchronized void setClientId(int id) {
        clientId = id;
    }

    public static synchronized int getClientId() {
        return clientId;
    }

    public static synchronized void clear() {
        adults = 0;
        children = 0;
        infants = 0;
        clientId = -1;
    }
}
