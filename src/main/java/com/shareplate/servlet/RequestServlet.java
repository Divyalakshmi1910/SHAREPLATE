package com.shareplate.servlet;

import com.shareplate.dao.RequestDao;
import com.shareplate.model.DonationRequest;
import com.shareplate.model.User;
import com.shareplate.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

/**
 * Handles Donation Request endpoints: /api/requests
 * Implements transaction safety, duplicate claim prevention,
 * and donor approval/rejection workflows.
 */
@WebServlet(name = "RequestServlet", urlPatterns = {"/api/requests/*"})
public class RequestServlet extends HttpServlet {

    private final RequestDao requestDao = new RequestDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        User sessionUser = (session != null) ? (User) session.getAttribute("user") : null;

        if (sessionUser == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Login required to view requests.");
            return;
        }

        String role = sessionUser.getRole();
        int userId = sessionUser.getUserId();

        if ("NGO".equalsIgnoreCase(role)) {
            List<DonationRequest> list = requestDao.getByNgoId(userId);
            JsonUtil.sendSuccess(resp, "Your NGO requests retrieved", list);
        } else if ("DONOR".equalsIgnoreCase(role)) {
            List<DonationRequest> list = requestDao.getByDonorId(userId);
            JsonUtil.sendSuccess(resp, "Incoming donation requests retrieved", list);
        } else if ("ADMIN".equalsIgnoreCase(role)) {
            List<DonationRequest> list = requestDao.getAll();
            JsonUtil.sendSuccess(resp, "All system requests retrieved", list);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Unauthorized role.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        User sessionUser = (session != null) ? (User) session.getAttribute("user") : null;

        if (sessionUser == null || (!sessionUser.canRequestDonation() && !"NGO".equalsIgnoreCase(sessionUser.getRole()))) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Only registered NGOs can request food donations.");
            return;
        }

        try {
            DonationRequest request = JsonUtil.fromJson(req, DonationRequest.class);
            if (request == null || request.getDonationId() <= 0) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Valid Donation ID is required.");
                return;
            }

            // Enforce authenticated NGO ID
            request.setNgoId(sessionUser.getUserId());

            // Check duplicate request
            if (requestDao.hasNgoAlreadyRequested(request.getDonationId(), request.getNgoId())) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_CONFLICT, "You have already requested this food donation.");
                return;
            }

            // Execute transactional claim
            boolean success = requestDao.create(request);
            if (success) {
                JsonUtil.sendSuccess(resp, "Food request placed successfully! Awaiting Donor approval.", request);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_CONFLICT, 
                        "This donation is no longer available or was just reserved by another NGO.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Request processing failed: " + e.getMessage());
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
            StatusUpdateDto dto = JsonUtil.fromJson(req, StatusUpdateDto.class);
            if (dto == null || dto.getId() <= 0 || dto.getStatus() == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Request ID and target status are required.");
                return;
            }

            String action = dto.getStatus().trim().toUpperCase();
            int requestId = dto.getId();
            int userId = sessionUser.getUserId();
            String userRole = sessionUser.getRole();

            boolean updated = false;

            switch (action) {
                case "APPROVED" -> {
                    // Only donor of donation or admin can approve
                    updated = requestDao.approveRequest(requestId, "ADMIN".equalsIgnoreCase(userRole) ? -1 : userId);
                }
                case "REJECTED" -> {
                    // Only donor of donation or admin can reject
                    updated = requestDao.rejectRequest(requestId, "ADMIN".equalsIgnoreCase(userRole) ? -1 : userId);
                }
                case "COLLECTED", "DELIVERED" -> {
                    updated = requestDao.updateLifecycleStatus(requestId, action, userId, userRole);
                }
                default -> {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid status transition.");
                    return;
                }
            }

            if (updated) {
                JsonUtil.sendSuccess(resp, "Request status successfully updated to " + action, null);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Could not update status. Check permissions and current record state.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Status update error: " + e.getMessage());
        }
    }

    public static class StatusUpdateDto {
        private int id;
        private String status;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
