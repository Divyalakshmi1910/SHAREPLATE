package com.shareplate.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Model representing a Food Donation listing created by a Donor.
 * Demonstrates Encapsulation with private fields and public accessors.
 */
public class Donation implements Serializable {
    private static final long serialVersionUID = 1L;

    // Encapsulated fields matching database schema
    private int donationId;
    private int donorId;
    private String foodName;
    private String category;
    private String quantity;
    private Timestamp preparationDatetime;
    private Timestamp safeCollectionDeadline;
    private String pickupLocation;
    private String storageConditions;
    private String allergenInformation;
    private String status; // Available, Requested, Approved, Collected, Delivered
    private Timestamp createdAt;

    // Joined fields from users table for display convenience
    private String donorName;
    private String donorPhone;
    private String donorEmail;

    public Donation() {}

    // Encapsulated Getters & Setters
    public int getDonationId() {
        return donationId;
    }

    public void setDonationId(int donationId) {
        this.donationId = donationId;
    }

    // Backwards compatibility aliases
    public int getId() {
        return donationId;
    }

    public void setId(int id) {
        this.donationId = id;
    }

    public int getDonorId() {
        return donorId;
    }

    public void setDonorId(int donorId) {
        this.donorId = donorId;
    }

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public Timestamp getPreparationDatetime() {
        return preparationDatetime;
    }

    public void setPreparationDatetime(Timestamp preparationDatetime) {
        this.preparationDatetime = preparationDatetime;
    }

    public Timestamp getSafeCollectionDeadline() {
        return safeCollectionDeadline;
    }

    public void setSafeCollectionDeadline(Timestamp safeCollectionDeadline) {
        this.safeCollectionDeadline = safeCollectionDeadline;
    }

    // Alias for getCollectionDeadline
    public Timestamp getCollectionDeadline() {
        return safeCollectionDeadline;
    }

    public void setCollectionDeadline(Timestamp deadline) {
        this.safeCollectionDeadline = deadline;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public String getStorageConditions() {
        return storageConditions;
    }

    public void setStorageConditions(String storageConditions) {
        this.storageConditions = storageConditions;
    }

    public String getAllergenInformation() {
        return allergenInformation;
    }

    public void setAllergenInformation(String allergenInformation) {
        this.allergenInformation = allergenInformation;
    }

    // Alias for getAllergenInfo
    public String getAllergenInfo() {
        return allergenInformation;
    }

    public void setAllergenInfo(String allergenInfo) {
        this.allergenInformation = allergenInfo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getDonorName() {
        return donorName;
    }

    public void setDonorName(String donorName) {
        this.donorName = donorName;
    }

    public String getDonorPhone() {
        return donorPhone;
    }

    public void setDonorPhone(String donorPhone) {
        this.donorPhone = donorPhone;
    }

    public String getDonorEmail() {
        return donorEmail;
    }

    public void setDonorEmail(String donorEmail) {
        this.donorEmail = donorEmail;
    }
}
