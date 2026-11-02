package com.shareplate.servlet;

import com.shareplate.dao.UserDao;
import com.shareplate.model.User;
import com.shareplate.util.JsonUtil;
import com.shareplate.util.PasswordUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles Authentication endpoints:
 * - POST /api/auth/register (Registration with salted SHA-256 password hash)
 * - POST /api/auth/login (Credential verification and session establishment)
 * - POST /api/auth/logout (Session invalidation)
 * - GET  /api/auth/me (Current authenticated session user profile)
 */
@WebServlet(name = "AuthServlet", urlPatterns = {"/api/auth/*"})
public class AuthServlet extends HttpServlet {

    private final UserDao userDao = new UserDao();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) pathInfo = "";

        switch (pathInfo) {
            case "/register" -> handleRegister(req, resp);
            case "/login" -> handleLogin(req, resp);
            case "/logout" -> handleLogout(req, resp);
            default -> JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown auth action");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        if ("/me".equals(pathInfo)) {
            handleGetCurrentUser(req, resp);
        } else if ("/logout".equals(pathInfo)) {
            handleLogout(req, resp);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
        }
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            User user = JsonUtil.fromJson(req, User.class);
            if (user == null || user.getEmail() == null || user.getName() == null || user.getPassword() == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "All required registration fields must be provided.");
                return;
            }

            String email = user.getEmail().trim().toLowerCase();
            String name = user.getName().trim();
            String password = user.getPassword();
            String phone = user.getPhone() != null ? user.getPhone().trim() : "";
            String role = user.getRole() != null ? user.getRole().trim().toUpperCase() : "DONOR";

            // Validation checks
            if (name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Name, email, and phone are mandatory.");
                return;
            }

            if (password.length() < 6) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Password must be at least 6 characters.");
                return;
            }

            if (!role.equals("DONOR") && !role.equals("NGO")) {
                role = "DONOR";
            }

            // Check for duplicate email
            if (userDao.emailExists(email)) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_CONFLICT, "An account with this email address already exists.");
                return;
            }

            // Secure salted SHA-256 password hash
            String passwordHash = PasswordUtil.hashPassword(password);

            User newUser = User.create(0, name, email, passwordHash, phone, role, null);
            boolean success = userDao.register(newUser);

            if (success) {
                // Initialize session
                HttpSession session = req.getSession(true);
                newUser.setPasswordHash(null); // Never return hash to client
                session.setAttribute("user", newUser);

                Map<String, Object> data = new HashMap<>();
                data.put("user", newUser);
                data.put("dashboardUrl", newUser.getDashboardUrl());

                JsonUtil.sendSuccess(resp, "Registration successful!", data);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed in database.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Registration error: " + e.getMessage());
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            User credentials = JsonUtil.fromJson(req, User.class);
            if (credentials == null || credentials.getEmail() == null || credentials.getPassword() == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Email and password are required.");
                return;
            }

            String email = credentials.getEmail().trim().toLowerCase();
            String password = credentials.getPassword();

            User user = userDao.findByEmail(email);
            if (user == null || !PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Invalid email address or password.");
                return;
            }

            // Create authenticated session
            HttpSession session = req.getSession(true);
            user.setPasswordHash(null); // Protect hash
            session.setAttribute("user", user);

            Map<String, Object> data = new HashMap<>();
            data.put("user", user);
            data.put("dashboardUrl", user.getDashboardUrl());

            JsonUtil.sendSuccess(resp, "Login successful!", data);
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Login processing error: " + e.getMessage());
        }
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        JsonUtil.sendSuccess(resp, "Logged out successfully.", null);
    }

    private void handleGetCurrentUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            User user = (User) session.getAttribute("user");
            Map<String, Object> data = new HashMap<>();
            data.put("user", user);
            data.put("dashboardUrl", user.getDashboardUrl());
            JsonUtil.sendSuccess(resp, "Active session found.", data);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "No active user session.");
        }
    }
}
