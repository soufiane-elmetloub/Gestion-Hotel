package com.example.smarthotelapp;

public class ClientStats {
    private int total;
    private int active;
    private int inactive;

    public ClientStats() {}

    public ClientStats(int total, int active, int inactive) {
        this.total = total;
        this.active = active;
        this.inactive = inactive;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getActive() {
        return active;
    }

    public void setActive(int active) {
        this.active = active;
    }

    public int getInactive() {
        return inactive;
    }

    public void setInactive(int inactive) {
        this.inactive = inactive;
    }

    @Override
    public String toString() {
        return "ClientStats{" +
                "total=" + total +
                ", active=" + active +
                ", inactive=" + inactive +
                '}';
    }
}
