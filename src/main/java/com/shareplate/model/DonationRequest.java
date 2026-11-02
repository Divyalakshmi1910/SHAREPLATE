package com.shareplate.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Model representing an NGO request for a food donation.
 * Demonstrates Encapsulation with private fields and public accessors.
 */
public class DonationRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    // Encapsulated fields matching database schema
    private int requestId;
    private int donationId;
    private int ngoId;
    private Timestamp requestDate;
    private String requestStatus; // PENDING, APPROVED, REJECTED, COLLECTED, DELIVERED
    private int beneficiaryCount;
    private String requestNotes;

    // Joined fields from donations and users tables for rich display
    private String foodName;
    private String foodQuantity;
    private String foodCategory;
    private String pickupLocation;
    private String storageConditions;
    private Timestamp safeCollectionDeadline;

    private String donorName;
    private String donorPhone;

    private String ngoName;
    private String ngoPhone;
    private String ngoEmail;

    public DonationRequest() {}

    // Encapsulated Getters & Setters
    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    // Compatibility alias
    public int getId() {
        return requestId;
    }

    public void setId(int id) {
        this.requestId = id;
    }

    public int getDonationId() {
        return donationId;
    }

    public void setDonationId(int donationId) {
        this.donationId = donationId;
    }

    public int getNgoId() {
        return ngoId;
    }

    public void setNgoId(int ngoId) {
        this.ngoId = ngoId;
    }

    public Timestamp getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(Timestamp requestDate) {
        this.requestDate = requestDate;
    }

    // Compatibility alias for createdAt
    public Timestamp getCreatedAt() {
        return requestDate;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.requestDate = createdAt;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
    }

    // Compatibility alias for status
    public String getStatus() {
        return requestStatus;
    }

    public void setStatus(String status) {
        this.requestStatus = status;
    }

    public int getBeneficiaryCount() {
        return beneficiaryCount;
    }

    public void setBeneficiaryCount(int beneficiaryCount) {
        this.beneficiaryCount = beneficiaryCount;
    }

    public String getRequestNotes() {
        return requestNotes;
    }

    public void setRequestNotes(String requestNotes) {
        this.requestNotes = requestNotes;
    }

    // Compatibility alias for requestMessage
    public String getRequestMessage() {
        return requestNotes;
    }

    public void setRequestMessage(String message) {
        this.requestNotes = message;
    }

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public String getFoodQuantity() {
        return foodQuantity;
    }

    public void setFoodQuantity(String foodQuantity) {
        this.foodQuantity = foodQuantity;
    }

    public String getFoodCategory() {
        return foodCategory;
    }

    public void setFoodCategory(String foodCategory) {
        this.foodCategory = foodCategory;
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

    public Timestamp getSafeCollectionDeadline() {
        return safeCollectionDeadline;
    }

    public void setSafeCollectionDeadline(Timestamp safeCollectionDeadline) {
        this.safeCollectionDeadline = safeCollectionDeadline;
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

    public String getNgoName() {
        return ngoName;
    }

    public void setNgoName(String ngoName) {
        this.ngoName = ngoName;
    }

    public String getNgoPhone() {
        return ngoPhone;
    }

    public void setNgoPhone(String ngoPhone) {
        this.ngoPhone = ngoPhone;
    }

    public String getNgoEmail() {
        return ngoEmail;
    }

    public void setNgoEmail(String ngoEmail) {
        this.ngoEmail = ngoEmail;
    }
}
