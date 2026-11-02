-- ====================================================================
-- SharePlate: Food Donation & Redistribution System
-- Database Schema for MySQL 8.0+
-- ====================================================================

CREATE DATABASE IF NOT EXISTS shareplate_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE shareplate_db;

-- --------------------------------------------------------------------
-- Table: users
-- Stores Donors, NGOs, and Administrators
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(25) NOT NULL,
    role ENUM('DONOR', 'NGO', 'ADMIN') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------------------
-- Table: donations
-- Stores surplus food donation listings created by Donors
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS donations (
    donation_id INT AUTO_INCREMENT PRIMARY KEY,
    donor_id INT NOT NULL,
    food_name VARCHAR(150) NOT NULL,
    category VARCHAR(60) NOT NULL,
    quantity VARCHAR(100) NOT NULL,
    preparation_datetime DATETIME NOT NULL,
    safe_collection_deadline DATETIME NOT NULL,
    pickup_location TEXT NOT NULL,
    storage_conditions VARCHAR(100) NOT NULL,
    allergen_information VARCHAR(255) DEFAULT 'None reported',
    status ENUM('Available', 'Requested', 'Approved', 'Collected', 'Delivered') NOT NULL DEFAULT 'Available',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_donations_donor FOREIGN KEY (donor_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------------------
-- Table: donation_requests
-- Stores food requests submitted by NGOs for donations
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS donation_requests (
    request_id INT AUTO_INCREMENT PRIMARY KEY,
    donation_id INT NOT NULL,
    ngo_id INT NOT NULL,
    request_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    request_status ENUM('PENDING', 'APPROVED', 'REJECTED', 'COLLECTED', 'DELIVERED') NOT NULL DEFAULT 'PENDING',
    beneficiary_count INT DEFAULT 10,
    request_notes TEXT,
    CONSTRAINT fk_requests_donation FOREIGN KEY (donation_id) REFERENCES donations(donation_id) ON DELETE CASCADE,
    CONSTRAINT fk_requests_ngo FOREIGN KEY (ngo_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_donations_status ON donations(status);
CREATE INDEX IF NOT EXISTS idx_donations_donor ON donations(donor_id);
CREATE INDEX IF NOT EXISTS idx_requests_donation ON donation_requests(donation_id);
CREATE INDEX IF NOT EXISTS idx_requests_ngo ON donation_requests(ngo_id);
