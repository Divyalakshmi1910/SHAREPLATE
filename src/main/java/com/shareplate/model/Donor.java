package com.shareplate.model;

import java.sql.Timestamp;

/**
 * Child class representing a Food Donor (Restaurant, College Canteen, Bakery, Caterer, etc.).
 * Demonstrates Inheritance and Polymorphic method overriding.
 */
public class Donor extends User {
    private static final long serialVersionUID = 1L;

    public Donor() {
        super();
        setRole("DONOR");
    }

    public Donor(int userId, String name, String email, String passwordHash, String phone, Timestamp createdAt) {
        super(userId, name, email, passwordHash, phone, "DONOR", createdAt);
    }

    // Polymorphism: Overriding parent methods with Donor-specific behaviors
    @Override
    public String getDashboardUrl() {
        return "donor-dashboard.html";
    }

    @Override
    public boolean canCreateDonation() {
        return true;
    }

    @Override
    public boolean canRequestDonation() {
        return false;
    }

    @Override
    public String getRoleDisplayName() {
        return "Food Donor";
    }
}
