package com.shareplate.model;

import java.sql.Timestamp;

/**
 * Child class representing a System Administrator.
 * Demonstrates Inheritance and Polymorphic method overriding.
 */
public class Admin extends User {
    private static final long serialVersionUID = 1L;

    public Admin() {
        super();
        setRole("ADMIN");
    }

    public Admin(int userId, String name, String email, String passwordHash, String phone, Timestamp createdAt) {
        super(userId, name, email, passwordHash, phone, "ADMIN", createdAt);
    }

    // Polymorphism: Overriding parent methods with Admin-specific behaviors
    @Override
    public String getDashboardUrl() {
        return "admin-dashboard.html";
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
        return "System Administrator";
    }
}
