package com.shareplate.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility for secure salted password hashing using SHA-256.
 * Complies with OWASP security guidelines without requiring external libraries.
 */
public class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Hashes a plain-text password using a cryptographically random salt and SHA-256.
     * @param password Plain-text password
     * @return Formatted string: "saltBase64:hashBase64"
     */
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        try {
            byte[] salt = new byte[16];
            RANDOM.nextBytes(salt);

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available in current JVM", e);
        }
    }

    /**
     * Verifies a plain-text password against a stored salted hash.
     * @param password Plain-text password to verify
     * @param storedHash Stored hash from database ("saltBase64:hashBase64")
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(String password, String storedHash) {
        if (password == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }

        // Salted hash format: "salt:hash"
        if (storedHash.contains(":")) {
            String[] parts = storedHash.split(":", 2);
            if (parts.length != 2) return false;

            try {
                byte[] salt = Base64.getDecoder().decode(parts[0]);
                byte[] expectedHash = Base64.getDecoder().decode(parts[1]);

                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(salt);
                byte[] actualHash = md.digest(password.getBytes(StandardCharsets.UTF_8));

                return MessageDigest.isEqual(actualHash, expectedHash);
            } catch (Exception e) {
                return false;
            }
        }

        // Plain text fallback (for transitional development databases)
        return storedHash.equals(password);
    }
}
