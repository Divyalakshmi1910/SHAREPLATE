package com.shareplate.servlet;

import com.shareplate.dao.DonationDao;
import com.shareplate.model.Donation;
import com.shareplate.model.User;
import com.shareplate.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;

/**
 * Handles Food Donation CRUD endpoints: /api/donations
 * Implements server-side validation, ownership checks, and role enforcement.
 */
@WebServlet(name = "DonationServlet", urlPatterns = {"/api/donations/*"})
public class DonationServlet extends HttpServlet {

    private final DonationDao donationDao = new DonationDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String idParam = req.getParameter("id");
        String donorIdParam = req.getParameter("donorId");
        String scope = req.getParameter("scope");

        HttpSession session = req.getSession(false);
        User sessionUser = (session != null) ? (User) session.getAttribute("user") : null;

        // 1. Single donation retrieval
        if (idParam != null && !idParam.isBlank()) {
            try {
                int id = Integer.parseInt(idParam);
                Donation donation = donationDao.getById(id);
                if (donation != null) {
                    JsonUtil.sendSuccess(resp, "Donation found", donation);
                } else {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Donation record not found.");
                }
            } catch (NumberFormatException e) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid donation ID format.");
            }
            return;
        }

        // 2. Personal donations for current donor
        if ("my".equalsIgnoreCase(scope) || (donorIdParam == null && "DONOR".equalsIgnoreCase(sessionUser != null ? sessionUser.getRole() : ""))) {
            if (sessionUser == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Login required to view personal donations.");
                return;
            }
            List<Donation> list = donationDao.getByDonorId(sessionUser.getUserId());
            JsonUtil.sendSuccess(resp, "Personal donations retrieved", list);
            return;
        }

        // 3. Donations by specific donor ID
        if (donorIdParam != null && !donorIdParam.isBlank()) {
            try {
                int donorId = Integer.parseInt(donorIdParam);
                List<Donation> list = donationDao.getByDonorId(donorId);
                JsonUtil.sendSuccess(resp, "Donor donations retrieved", list);
            } catch (NumberFormatException e) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid donor ID.");
            }
            return;
        }

        // 4. All donations (Admin)
        if ("all".equalsIgnoreCase(scope)) {
            if (sessionUser == null || !"ADMIN".equalsIgnoreCase(sessionUser.getRole())) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Admin privileges required.");
                return;
            }
            List<Donation> list = donationDao.getAll();
            JsonUtil.sendSuccess(resp, "All donations retrieved", list);
            return;
        }

        // 5. Default: Available donations for NGOs & Public browsing
        List<Donation> list = donationDao.getAvailable();
        JsonUtil.sendSuccess(resp, "Available food donations retrieved", list);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        User sessionUser = (session != null) ? (User) session.getAttribute("user") : null;

        if (sessionUser == null || (!sessionUser.canCreateDonation() && !"DONOR".equalsIgnoreCase(sessionUser.getRole()))) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Only registered Food Donors can add donations.");
            return;
        }

        try {
            Donation donation = JsonUtil.fromJson(req, Donation.class);
            if (donation == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid donation request payload.");
                return;
            }

            // Server-side validation of all mandatory fields
            if (donation.getFoodName() == null || donation.getFoodName().isBlank() ||
                donation.getCategory() == null || donation.getCategory().isBlank() ||
                donation.getQuantity() == null || donation.getQuantity().isBlank() ||
                donation.getPickupLocation() == null || donation.getPickupLocation().isBlank() ||
                donation.getStorageConditions() == null || donation.getStorageConditions().isBlank()) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Please provide all required food donation details.");
                return;
            }

            // Enforce donor_id from authenticated session (prevents spoofing)
            donation.setDonorId(sessionUser.getUserId());

            // Validate timestamps
            if (donation.getPreparationDatetime() == null) {
                donation.setPreparationDatetime(new Timestamp(System.currentTimeMillis()));
            }
            if (donation.getSafeCollectionDeadline() == null) {
                donation.setSafeCollectionDeadline(new Timestamp(System.currentTimeMillis() + (6 * 3600 * 1000L)));
            }

            if (donation.getSafeCollectionDeadline().before(donation.getPreparationDatetime())) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Collection deadline must be after preparation date & time.");
                return;
            }

            donation.setStatus("Available");

            boolean success = donationDao.create(donation);
            if (success) {
                JsonUtil.sendSuccess(resp, "Food donation created successfully in MySQL!", donation);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to save donation in database.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Donation submission error: " + e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        User sessionUser = (session != null) ? (User) session.getAttribute("user") : null;

        if (sessionUser == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required.");
            return;
        }

        try {
            Donation donation = JsonUtil.fromJson(req, Donation.class);
            if (donation == null || donation.getDonationId() <= 0) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Valid donation ID is required for editing.");
                return;
            }

            // Check existing record
            Donation existing = donationDao.getById(donation.getDonationId());
            if (existing == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Donation does not exist.");
                return;
            }

            // Ownership check: must be owner or admin
            boolean isAdmin = "ADMIN".equalsIgnoreCase(sessionUser.getRole());
            if (!isAdmin && existing.getDonorId() != sessionUser.getUserId()) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "You do not have permission to edit this donation.");
                return;
            }

            donation.setDonorId(existing.getDonorId());
            boolean success = donationDao.update(donation);
            if (success) {
                JsonUtil.sendSuccess(resp, "Donation updated successfully in MySQL.", donation);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to update donation.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Donation update error: " + e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        User sessionUser = (session != null) ? (User) session.getAttribute("user") : null;

        if (sessionUser == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required.");
            return;
        }

        String idParam = req.getParameter("id");
        if (idParam == null || idParam.isBlank()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Donation ID parameter is required.");
            return;
        }

        try {
            int donationId = Integer.parseInt(idParam);
            Donation existing = donationDao.getById(donationId);
            if (existing == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Donation not found.");
                return;
            }

            boolean isAdmin = "ADMIN".equalsIgnoreCase(sessionUser.getRole());
            if (!isAdmin && existing.getDonorId() != sessionUser.getUserId()) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "You do not own this donation.");
                return;
            }

            boolean deleted = donationDao.delete(donationId, sessionUser.getUserId(), isAdmin);
            if (deleted) {
                JsonUtil.sendSuccess(resp, "Donation deleted successfully from MySQL.", null);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to delete donation.");
            }
        } catch (NumberFormatException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid donation ID format.");
        }
    }
}
