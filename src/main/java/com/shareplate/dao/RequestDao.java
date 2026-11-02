package com.shareplate.dao;

import com.shareplate.config.DatabaseConfig;
import com.shareplate.model.DonationRequest;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Donation Requests using JDBC.
 * Implements transaction management, duplicate request prevention,
 * and concurrent reservation locks.
 */
public class RequestDao {

    /**
     * Checks if an NGO has already submitted a pending or approved request for a donation.
     * Prevents duplicate requests.
     */
    public boolean hasNgoAlreadyRequested(int donationId, int ngoId) {
        String sql = "SELECT COUNT(*) FROM donation_requests " +
                     "WHERE donation_id = ? AND ngo_id = ? AND request_status IN ('PENDING', 'APPROVED')";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, donationId);
            ps.setInt(2, ngoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * CREATE: Creates a new food request with transaction management.
     * Prevents multiple NGOs from successfully reserving the same donation concurrently.
     * Returns true on success, false if already reserved or failed.
     */
    public boolean create(DonationRequest req) {
        if (hasNgoAlreadyRequested(req.getDonationId(), req.getNgoId())) {
            return false; // Prevent duplicate
        }

        String reserveDonationSql = "UPDATE donations SET status = 'Requested' " +
                                    "WHERE donation_id = ? AND status = 'Available'";
        String insertRequestSql = "INSERT INTO donation_requests (donation_id, ngo_id, " +
                                  "request_status, beneficiary_count, request_notes) " +
                                  "VALUES (?, ?, 'PENDING', ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psReserve = conn.prepareStatement(reserveDonationSql);
                 PreparedStatement psInsert = conn.prepareStatement(insertRequestSql, Statement.RETURN_GENERATED_KEYS)) {

                // Atomic reservation step
                psReserve.setInt(1, req.getDonationId());
                int rows = psReserve.executeUpdate();
                if (rows == 0) {
                    conn.rollback();
                    return false; // Donation already reserved or no longer Available
                }

                // Insert request record
                psInsert.setInt(1, req.getDonationId());
                psInsert.setInt(2, req.getNgoId());
                psInsert.setInt(3, req.getBeneficiaryCount() > 0 ? req.getBeneficiaryCount() : 10);
                psInsert.setString(4, req.getRequestNotes() != null ? req.getRequestNotes() : "");

                int affected = psInsert.executeUpdate();
                if (affected > 0) {
                    try (ResultSet rs = psInsert.getGeneratedKeys()) {
                        if (rs.next()) {
                            req.setRequestId(rs.getInt(1));
                        }
                    }
                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Approves an NGO's request and marks the donation as Approved.
     * Uses database transactions to ensure consistency.
     */
    public boolean approveRequest(int requestId, int donorId) {
        DonationRequest req = getById(requestId);
        if (req == null) return false;

        String verifySql = "SELECT donor_id FROM donations WHERE donation_id = ?";
        String updateReqSql = "UPDATE donation_requests SET request_status = 'APPROVED' WHERE request_id = ?";
        String updateDonSql = "UPDATE donations SET status = 'Approved' WHERE donation_id = ?";
        String rejectOthersSql = "UPDATE donation_requests SET request_status = 'REJECTED' " +
                                 "WHERE donation_id = ? AND request_id != ? AND request_status = 'PENDING'";

        try (Connection conn = DatabaseConfig.getConnection()) {
            // Verify donor ownership
            try (PreparedStatement psVerify = conn.prepareStatement(verifySql)) {
                psVerify.setInt(1, req.getDonationId());
                try (ResultSet rs = psVerify.executeQuery()) {
                    if (!rs.next() || (donorId > 0 && rs.getInt("donor_id") != donorId)) {
                        return false; // Unauthorized donor
                    }
                }
            }

            conn.setAutoCommit(false);
            try (PreparedStatement psReq = conn.prepareStatement(updateReqSql);
                 PreparedStatement psDon = conn.prepareStatement(updateDonSql);
                 PreparedStatement psOthers = conn.prepareStatement(rejectOthersSql)) {

                psReq.setInt(1, requestId);
                psReq.executeUpdate();

                psDon.setInt(1, req.getDonationId());
                psDon.executeUpdate();

                psOthers.setInt(1, req.getDonationId());
                psOthers.setInt(2, requestId);
                psOthers.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Rejects an NGO's request and resets donation status back to Available.
     */
    public boolean rejectRequest(int requestId, int donorId) {
        DonationRequest req = getById(requestId);
        if (req == null) return false;

        String verifySql = "SELECT donor_id FROM donations WHERE donation_id = ?";
        String updateReqSql = "UPDATE donation_requests SET request_status = 'REJECTED' WHERE request_id = ?";
        String updateDonSql = "UPDATE donations SET status = 'Available' WHERE donation_id = ?";

        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement psVerify = conn.prepareStatement(verifySql)) {
                psVerify.setInt(1, req.getDonationId());
                try (ResultSet rs = psVerify.executeQuery()) {
                    if (!rs.next() || (donorId > 0 && rs.getInt("donor_id") != donorId)) {
                        return false; // Unauthorized donor
                    }
                }
            }

            conn.setAutoCommit(false);
            try (PreparedStatement psReq = conn.prepareStatement(updateReqSql);
                 PreparedStatement psDon = conn.prepareStatement(updateDonSql)) {

                psReq.setInt(1, requestId);
                psReq.executeUpdate();

                psDon.setInt(1, req.getDonationId());
                psDon.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Updates lifecycle status to COLLECTED or DELIVERED.
     */
    public boolean updateLifecycleStatus(int requestId, String newStatus, int userId, String role) {
        DonationRequest req = getById(requestId);
        if (req == null) return false;

        String targetDonationStatus = switch (newStatus.toUpperCase()) {
            case "COLLECTED" -> "Collected";
            case "DELIVERED" -> "Delivered";
            default -> null;
        };

        if (targetDonationStatus == null) return false;

        String updateReqSql = "UPDATE donation_requests SET request_status = ? WHERE request_id = ?";
        String updateDonSql = "UPDATE donations SET status = ? WHERE donation_id = ?";

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psReq = conn.prepareStatement(updateReqSql);
                 PreparedStatement psDon = conn.prepareStatement(updateDonSql)) {

                psReq.setString(1, newStatus.toUpperCase());
                psReq.setInt(2, requestId);
                psReq.executeUpdate();

                psDon.setString(1, targetDonationStatus);
                psDon.setInt(2, req.getDonationId());
                psDon.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public DonationRequest getById(int requestId) {
        String sql = baseQuery() + " WHERE r.request_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<DonationRequest> getByNgoId(int ngoId) {
        List<DonationRequest> list = new ArrayList<>();
        String sql = baseQuery() + " WHERE r.ngo_id = ? ORDER BY r.request_id DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ngoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<DonationRequest> getByDonorId(int donorId) {
        List<DonationRequest> list = new ArrayList<>();
        String sql = baseQuery() + " WHERE d.donor_id = ? ORDER BY r.request_id DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, donorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<DonationRequest> getAll() {
        List<DonationRequest> list = new ArrayList<>();
        String sql = baseQuery() + " ORDER BY r.request_id DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int countPending() {
        String sql = "SELECT COUNT(*) FROM donation_requests WHERE request_status = 'PENDING'";
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
        return "SELECT r.*, " +
               "d.food_name, d.quantity AS food_quantity, d.category AS food_category, " +
               "d.pickup_location, d.storage_conditions, d.safe_collection_deadline, " +
               "donor.name AS donor_name, donor.phone AS donor_phone, " +
               "ngo.name AS ngo_name, ngo.phone AS ngo_phone, ngo.email AS ngo_email " +
               "FROM donation_requests r " +
               "JOIN donations d ON r.donation_id = d.donation_id " +
               "JOIN users donor ON d.donor_id = donor.user_id " +
               "JOIN users ngo ON r.ngo_id = ngo.user_id";
    }

    private DonationRequest mapRow(ResultSet rs) throws SQLException {
        DonationRequest r = new DonationRequest();
        r.setRequestId(rs.getInt("request_id"));
        r.setDonationId(rs.getInt("donation_id"));
        r.setNgoId(rs.getInt("ngo_id"));
        r.setRequestDate(rs.getTimestamp("request_date"));
        r.setRequestStatus(rs.getString("request_status"));
        r.setBeneficiaryCount(rs.getInt("beneficiary_count"));
        r.setRequestNotes(rs.getString("request_notes"));

        r.setFoodName(rs.getString("food_name"));
        r.setFoodQuantity(rs.getString("food_quantity"));
        r.setFoodCategory(rs.getString("food_category"));
        r.setPickupLocation(rs.getString("pickup_location"));
        r.setStorageConditions(rs.getString("storage_conditions"));
        r.setSafeCollectionDeadline(rs.getTimestamp("safe_collection_deadline"));

        r.setDonorName(rs.getString("donor_name"));
        r.setDonorPhone(rs.getString("donor_phone"));

        r.setNgoName(rs.getString("ngo_name"));
        r.setNgoPhone(rs.getString("ngo_phone"));
        r.setNgoEmail(rs.getString("ngo_email"));
        return r;
    }
}
