package com.shareplate.model;

import java.sql.Timestamp;

/**
 * Child class representing a Registered Non-Governmental Organization / Charity.
 * Demonstrates Inheritance and Polymorphic method overriding.
 */
public class NGO extends User {
    private static final long serialVersionUID = 1L;

    public NGO() {
        super();
        setRole("NGO");
    }

    public NGO(int userId, String name, String email, String passwordHash, String phone, Timestamp createdAt) {
        super(userId, name, email, passwordHash, phone, "NGO", createdAt);
    }

    // Polymorphism: Overriding parent methods with NGO-specific behaviors
    @Override
    public String getDashboardUrl() {
        return "ngo-dashboard.html";
    }

    @Override
    public boolean canCreateDonation() {
        return false;
    }

    @Override
    public boolean canRequestDonation() {
        return true;
    }

    @Override
    public String getRoleDisplayName() {
        return "Non-Governmental Organization (NGO)";
    }
}
