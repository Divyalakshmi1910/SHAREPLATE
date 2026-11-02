package com.shareplate.servlet;

import com.shareplate.dao.DonationDao;
import com.shareplate.dao.RequestDao;
import com.shareplate.dao.UserDao;
import com.shareplate.model.Donation;
import com.shareplate.model.DonationRequest;
import com.shareplate.model.User;
import com.shareplate.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles Admin Oversight endpoints: /api/admin/*
 * Protected by Role-Based Access Control (RBAC).
 */
@WebServlet(name = "AdminServlet", urlPatterns = {"/api/admin/*"})
public class AdminServlet extends HttpServlet {

    private final UserDao userDao = new UserDao();
    private final DonationDao donationDao = new DonationDao();
    private final RequestDao requestDao = new RequestDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        User sessionUser = (session != null) ? (User) session.getAttribute("user") : null;

        // Role-Based Access Control check
        if (sessionUser == null || !"ADMIN".equalsIgnoreCase(sessionUser.getRole())) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied. Administrator privileges required.");
            return;
        }

        String pathInfo = req.getPathInfo();
        if (pathInfo == null) pathInfo = "";

        switch (pathInfo) {
            case "/stats" -> handleStats(req, resp);
            case "/users" -> handleUsers(req, resp);
            case "/donations" -> handleDonations(req, resp);
            case "/requests" -> handleRequests(req, resp);
            default -> JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Admin endpoint not found.");
        }
    }

    private void handleStats(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Object> stats = new HashMap<>();

        int totalUsers = userDao.countTotalUsers();
        int totalDonors = userDao.countByRole("DONOR");
        int totalNgos = userDao.countByRole("NGO");

        int totalDonations = donationDao.countTotal();
        int availableCount = donationDao.countAvailable();
        int completedCount = donationDao.countCompleted();
        int pendingRequests = requestDao.countPending();

        stats.put("totalUsers", totalUsers);
        stats.put("totalDonors", totalDonors);
        stats.put("totalNgos", totalNgos);
        stats.put("totalDonations", totalDonations);
        stats.put("availableCount", availableCount);
        stats.put("completedCount", completedCount);
        stats.put("pendingRequests", pendingRequests);

        JsonUtil.sendSuccess(resp, "Platform statistics retrieved from MySQL", stats);
    }

    private void handleUsers(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<User> users = userDao.getAllUsers();
        JsonUtil.sendSuccess(resp, "Registered users list retrieved", users);
    }

    private void handleDonations(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<Donation> donations = donationDao.getAll();
        JsonUtil.sendSuccess(resp, "Master donations list retrieved", donations);
    }

    private void handleRequests(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<DonationRequest> requests = requestDao.getAll();
        JsonUtil.sendSuccess(resp, "Master donation requests retrieved", requests);
    }
}
