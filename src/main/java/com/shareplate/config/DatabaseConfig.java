package com.shareplate.config;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Manages JDBC database connectivity for SharePlate.
 * Supports credentials from Environment Variables (DB_URL, DB_USER, DB_PASSWORD)
 * with fallback to local db.properties file.
 */
public class DatabaseConfig {

    private static String url;
    private static String username;
    private static String password;
    private static String driver = "com.mysql.cj.jdbc.Driver";
    private static boolean initialized = false;

    static {
        loadConfig();
    }

    private static synchronized void loadConfig() {
        if (initialized) return;

        // 1. Load from db.properties
        try (InputStream in = DatabaseConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
            Properties props = new Properties();
            if (in != null) {
                props.load(in);
                driver = props.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");
                url = props.getProperty("db.url", "jdbc:mysql://localhost:3306/shareplate_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8");
                username = props.getProperty("db.username", "root");
                password = props.getProperty("db.password", "root123");
            }
        } catch (Exception e) {
            System.err.println("[SharePlate] Note: db.properties read warning: " + e.getMessage());
        }

        // 2. Override with Environment Variables if defined (recommended for production security)
        String envUrl = System.getenv("DB_URL");
        if (envUrl != null && !envUrl.isBlank()) url = envUrl;

        String envUser = System.getenv("DB_USER");
        if (envUser != null && !envUser.isBlank()) username = envUser;

        String envPass = System.getenv("DB_PASSWORD");
        if (envPass != null) password = envPass;

        if (url == null || url.isBlank()) {
            url = "jdbc:mysql://localhost:3306/shareplate_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";
        }
        if (username == null || username.isBlank()) {
            username = "root";
        }
        if (password == null) {
            password = "";
        }

        try {
            Class.forName(driver);
            initialized = true;
            System.out.println("[SharePlate] MySQL Driver initialized. Target URL: " + url);
        } catch (ClassNotFoundException e) {
            System.err.println("[SharePlate] MySQL JDBC Driver not found in classpath: " + e.getMessage());
        }
    }

    /**
     * Obtains an active connection to the MySQL database.
     * @return java.sql.Connection
     * @throws SQLException when connection cannot be established
     */
    public static Connection getConnection() throws SQLException {
        if (!initialized) {
            loadConfig();
        }
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * Helper to test whether the database connection is currently active.
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            return false;
        }
    }
}
