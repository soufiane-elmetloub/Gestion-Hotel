package com.example.smarthotelapp.models;

public class Client {
    private int id;
    private String firstName;
    private String lastName;
    private String phone;
    private String nationalId;
    private String email;
    private String createdAt;
    private String status;

    // Default constructor
    public Client() {}

    // Constructor with 5 parameters (used in AddBookingActivity and AddGuestsActivity)
    public Client(int id, String firstName, String lastName, String phone, String nationalId) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.nationalId = nationalId;
        this.email = "";
        this.createdAt = "";
        this.status = "Active";
    }

    // Constructor with all parameters
    public Client(int id, String firstName, String lastName, String phone, String nationalId, String email, String createdAt, String status) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.nationalId = nationalId;
        this.email = email;
        this.createdAt = createdAt;
        this.status = status;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getNationalId() {
        return nationalId;
    }

    public void setNationalId(String nationalId) {
        this.nationalId = nationalId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Helper methods
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getInitials() {
        String firstInitial = firstName != null && !firstName.isEmpty() ? firstName.substring(0, 1) : "";
        String lastInitial = lastName != null && !lastName.isEmpty() ? lastName.substring(0, 1) : "";
        return (firstInitial + lastInitial).toUpperCase();
    }

    public boolean isActive() {
        return "نشط".equals(status) || "Active".equals(status);
    }

    @Override
    public String toString() {
        return "Client{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", phone='" + phone + '\'' +
                ", nationalId='" + nationalId + '\'' +
                ", email='" + email + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
