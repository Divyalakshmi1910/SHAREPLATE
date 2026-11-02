package com.shareplate.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Parent User class implementing Encapsulation and Abstraction.
 * Specialized by Donor, NGO, and Admin child classes.
 */
public class User implements RoleAccessible, Serializable {
    private static final long serialVersionUID = 1L;

    // Encapsulation: private member variables
    private int userId;
    private String name;
    private String email;
    private String passwordHash;
    private String phone;
    private String role; // DONOR, NGO, ADMIN
    private Timestamp createdAt;

    public User() {}

    public User(int userId, String name, String email, String passwordHash, String phone, String role, Timestamp createdAt) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.phone = phone;
        this.role = role != null ? role.toUpperCase() : "DONOR";
        this.createdAt = createdAt;
    }

    /**
     * Polymorphic Factory Method: Instantiates the appropriate subclass (Donor, NGO, Admin)
     * based on user role.
     */
    public static User create(int userId, String name, String email, String passwordHash, String phone, String role, Timestamp createdAt) {
        if (role == null) role = "DONOR";
        return switch (role.toUpperCase()) {
            case "DONOR" -> new Donor(userId, name, email, passwordHash, phone, createdAt);
            case "NGO" -> new NGO(userId, name, email, passwordHash, phone, createdAt);
            case "ADMIN" -> new Admin(userId, name, email, passwordHash, phone, createdAt);
            default -> new User(userId, name, email, passwordHash, phone, role, createdAt);
        };
    }

    // RoleAccessible Abstraction implementations (overridden in subclasses)
    @Override
    public String getDashboardUrl() {
        if ("DONOR".equalsIgnoreCase(role)) return "donor-dashboard.html";
        if ("NGO".equalsIgnoreCase(role)) return "ngo-dashboard.html";
        if ("ADMIN".equalsIgnoreCase(role)) return "admin-dashboard.html";
        return "index.html";
    }

    @Override
    public boolean canCreateDonation() {
        return "DONOR".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role);
    }

    @Override
    public boolean canRequestDonation() {
        return "NGO".equalsIgnoreCase(role);
    }

    @Override
    public String getRoleDisplayName() {
        return role != null ? role : "User";
    }

    // Encapsulated Getters & Setters
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    // Backwards compatibility alias for code expecting getId()
    public int getId() {
        return userId;
    }

    public void setId(int id) {
        this.userId = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    // Backwards compatibility alias
    public String getPassword() {
        return passwordHash;
    }

    public void setPassword(String password) {
        this.passwordHash = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role != null ? role.toUpperCase() : null;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
