package com.shareplate.dao;

import com.shareplate.config.DatabaseConfig;
import com.shareplate.model.Donation;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Food Donations using JDBC and PreparedStatements.
 * Implements CRUD operations, SQL JOIN queries, and safe parameter bindings.
 */
public class DonationDao {

    /**
     * CREATE: Adds a new food donation record.
     */
    public boolean create(Donation d) {
        String sql = "INSERT INTO donations (donor_id, food_name, category, quantity, " +
                     "preparation_datetime, safe_collection_deadline, pickup_location, " +
                     "storage_conditions, allergen_information, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, d.getDonorId());
            ps.setString(2, d.getFoodName());
            ps.setString(3, d.getCategory());
            ps.setString(4, d.getQuantity());
            ps.setTimestamp(5, d.getPreparationDatetime());
            ps.setTimestamp(6, d.getSafeCollectionDeadline());
            ps.setString(7, d.getPickupLocation());
            ps.setString(8, d.getStorageConditions());
            ps.setString(9, d.getAllergenInformation() != null && !d.getAllergenInformation().isBlank() 
                            ? d.getAllergenInformation() : "None reported");
            ps.setString(10, d.getStatus() != null ? d.getStatus() : "Available");

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        d.setDonationId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * READ: Retrieves a single donation by ID, including donor contact details.
     */
    public Donation getById(int donationId) {
        String sql = baseQuery() + " WHERE d.donation_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, donationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * READ: Retrieves all donations in the system (for Admin overview).
     */
    public List<Donation> getAll() {
        List<Donation> list = new ArrayList<>();
        String sql = baseQuery() + " ORDER BY d.donation_id DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * READ: Retrieves all available donations for NGOs to browse.
     * Filters for status = 'Available' and non-expired safe collection deadline.
     */
    public List<Donation> getAvailable() {
        List<Donation> list = new ArrayList<>();
        String sql = baseQuery() + " WHERE d.status = 'Available' AND d.safe_collection_deadline > NOW() " +
                     "ORDER BY d.safe_collection_deadline ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * READ: Retrieves donations created by a specific donor.
     */
    public List<Donation> getByDonorId(int donorId) {
        List<Donation> list = new ArrayList<>();
        String sql = baseQuery() + " WHERE d.donor_id = ? ORDER BY d.donation_id DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, donorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * UPDATE: Edits donation details.
     * Enforces ownership check: only the owning donor (or admin) can modify.
     */
    public boolean update(Donation d) {
        String sql = "UPDATE donations SET food_name = ?, category = ?, quantity = ?, " +
                     "preparation_datetime = ?, safe_collection_deadline = ?, pickup_location = ?, " +
                     "storage_conditions = ?, allergen_information = ? " +
                     "WHERE donation_id = ? AND donor_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, d.getFoodName());
            ps.setString(2, d.getCategory());
            ps.setString(3, d.getQuantity());
            ps.setTimestamp(4, d.getPreparationDatetime());
            ps.setTimestamp(5, d.getSafeCollectionDeadline());
            ps.setString(6, d.getPickupLocation());
            ps.setString(7, d.getStorageConditions());
            ps.setString(8, d.getAllergenInformation());
            ps.setInt(9, d.getDonationId());
            ps.setInt(10, d.getDonorId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * UPDATE: Updates the donation status (Available, Requested, Approved, Collected, Delivered).
     */
    public boolean updateStatus(int donationId, String status) {
        String sql = "UPDATE donations SET status = ? WHERE donation_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, donationId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * DELETE: Deletes an eligible donation.
     * Enforces ownership validation: donor_id must match, or caller must be admin.
     */
    public boolean delete(int donationId, int donorId, boolean isAdmin) {
        String sql = isAdmin ? "DELETE FROM donations WHERE donation_id = ?"
                             : "DELETE FROM donations WHERE donation_id = ? AND donor_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, donationId);
            if (!isAdmin) {
                ps.setInt(2, donorId);
            }
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Statistics Queries for Admin Dashboard
    public int countTotal() {
        String sql = "SELECT COUNT(*) FROM donations";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int countAvailable() {
        String sql = "SELECT COUNT(*) FROM donations WHERE status = 'Available'";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int countCompleted() {
        String sql = "SELECT COUNT(*) FROM donations WHERE status IN ('Collected', 'Delivered')";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private String baseQuery() {
        return "SELECT d.*, u.name AS donor_name, u.phone AS donor_phone, u.email AS donor_email " +
               "FROM donations d " +
               "JOIN users u ON d.donor_id = u.user_id";
    }

    private Donation mapRow(ResultSet rs) throws SQLException {
        Donation d = new Donation();
        d.setDonationId(rs.getInt("donation_id"));
        d.setDonorId(rs.getInt("donor_id"));
        d.setFoodName(rs.getString("food_name"));
        d.setCategory(rs.getString("category"));
        d.setQuantity(rs.getString("quantity"));
        d.setPreparationDatetime(rs.getTimestamp("preparation_datetime"));
        d.setSafeCollectionDeadline(rs.getTimestamp("safe_collection_deadline"));
        d.setPickupLocation(rs.getString("pickup_location"));
        d.setStorageConditions(rs.getString("storage_conditions"));
        d.setAllergenInformation(rs.getString("allergen_information"));
        d.setStatus(rs.getString("status"));
        d.setCreatedAt(rs.getTimestamp("created_at"));

        d.setDonorName(rs.getString("donor_name"));
        d.setDonorPhone(rs.getString("donor_phone"));
        d.setDonorEmail(rs.getString("donor_email"));
        return d;
    }
}
