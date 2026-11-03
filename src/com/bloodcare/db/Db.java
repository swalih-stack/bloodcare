package com.bloodcare.db;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Db - the single place that opens JDBC connections to MySQL.
 * Every DAO calls Db.get() instead of repeating connection code.
 * Settings are read from db.properties (url, user, password).
 * Db.init() creates the tables (if missing) and a default admin account.
 */
public final class Db {
    private static final Properties CONFIG = new Properties();

    private Db() {}

    /* Reads db.properties from the working directory. Called once at startup. */
    private static void loadConfig() throws SQLException {
        try (FileInputStream in = new FileInputStream("db.properties")) {
            CONFIG.load(in);
        } catch (IOException e) {
            throw new SQLException("Cannot read db.properties: " + e.getMessage(), e);
        }
    }

    /** Opens a new connection. Callers close it with try-with-resources. */
    public static Connection get() throws SQLException {
        if (CONFIG.isEmpty()) loadConfig();
        return DriverManager.getConnection(
                CONFIG.getProperty("url"), CONFIG.getProperty("user"), CONFIG.getProperty("password", ""));
    }

    // ---- small helpers so every DAO does not repeat the same open / prepare / read / close code ----

    private static PreparedStatement prepare(Connection c, String sql, Object[] params) throws SQLException {
        PreparedStatement ps = c.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
        return ps;
    }

    /** Runs a query that returns a single number (COUNT, SUM ...). */
    public static int queryInt(String sql, Object... params) throws SQLException {
        try (Connection c = get(); PreparedStatement ps = prepare(c, sql, params); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Runs a query and returns every row as an Object[] (whole numbers are always Integer, ready for a JTable). */
    public static List<Object[]> queryRows(String sql, Object... params) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        try (Connection c = get(); PreparedStatement ps = prepare(c, sql, params); ResultSet rs = ps.executeQuery()) {
            int columns = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                Object[] row = new Object[columns];
                for (int i = 0; i < columns; i++) {
                    Object v = rs.getObject(i + 1);
                    // COUNT() and SUM() come back as Long / BigDecimal; the screens expect Integer
                    row[i] = (v instanceof Long || v instanceof java.math.BigDecimal) ? Integer.valueOf(((Number) v).intValue()) : v;
                }
                rows.add(row);
            }
        }
        return rows;
    }

    /** Runs an INSERT / UPDATE / DELETE and returns how many rows changed. */
    public static int update(String sql, Object... params) throws SQLException {
        try (Connection c = get(); PreparedStatement ps = prepare(c, sql, params)) {
            return ps.executeUpdate();
        }
    }

    /** Creates the tables, adds newer columns to older databases, and seeds the default admin. */
    public static void init() throws SQLException {
        String[] ddl = {
            "CREATE TABLE IF NOT EXISTS users ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL, "
                + "email VARCHAR(120) NOT NULL UNIQUE, phone VARCHAR(20), blood_group VARCHAR(3) NOT NULL, "
                + "city VARCHAR(60), password_hash VARCHAR(200) NOT NULL, "
                + "role VARCHAR(10) NOT NULL DEFAULT 'USER', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
            "CREATE TABLE IF NOT EXISTS blood_requests ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, user_id INT NOT NULL, patient_name VARCHAR(100) NOT NULL, "
                + "blood_group VARCHAR(3) NOT NULL, units INT NOT NULL, hospital VARCHAR(120) NOT NULL, "
                + "city VARCHAR(60) NOT NULL, urgency VARCHAR(10) NOT NULL DEFAULT 'NORMAL', "
                + "status VARCHAR(12) NOT NULL DEFAULT 'PENDING', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)",
            "CREATE TABLE IF NOT EXISTS donations ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, user_id INT NOT NULL, blood_group VARCHAR(3) NOT NULL, "
                + "units INT NOT NULL, donation_date DATE NOT NULL, status VARCHAR(10) NOT NULL DEFAULT 'PENDING', "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)",
            "CREATE TABLE IF NOT EXISTS blood_banks ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(120) NOT NULL, city VARCHAR(60) NOT NULL, "
                + "phone VARCHAR(20), address VARCHAR(200), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
            // units is signed: donations add stock (+), fulfilled requests remove stock (-)
            "CREATE TABLE IF NOT EXISTS blood_inventory ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, bank_id INT NULL, blood_group VARCHAR(3) NOT NULL, "
                + "units INT NOT NULL, note VARCHAR(160), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (bank_id) REFERENCES blood_banks(id) ON DELETE SET NULL)",
            // a donor's answer to an approved request (one answer per donor per request)
            "CREATE TABLE IF NOT EXISTS request_responses ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, request_id INT NOT NULL, donor_id INT NOT NULL, "
                + "status VARCHAR(10) NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "UNIQUE KEY one_answer (request_id, donor_id), "
                + "FOREIGN KEY (request_id) REFERENCES blood_requests(id) ON DELETE CASCADE, "
                + "FOREIGN KEY (donor_id) REFERENCES users(id) ON DELETE CASCADE)",
            // chat between the requester and an accepted donor; the conversation is the response row
            "CREATE TABLE IF NOT EXISTS chat_messages ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, response_id INT NOT NULL, sender_id INT NOT NULL, "
                + "body VARCHAR(500) NOT NULL, is_read BOOLEAN NOT NULL DEFAULT FALSE, "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (response_id) REFERENCES request_responses(id) ON DELETE CASCADE, "
                + "FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE)",
            // messages a user writes to the admin
            "CREATE TABLE IF NOT EXISTS messages ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, user_id INT NOT NULL, body VARCHAR(500) NOT NULL, "
                + "is_read BOOLEAN NOT NULL DEFAULT FALSE, is_admin_reply BOOLEAN NOT NULL DEFAULT FALSE, "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)",
            // user_id NULL means the message is for the admin inbox
            "CREATE TABLE IF NOT EXISTS notifications ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, user_id INT NULL, message VARCHAR(300) NOT NULL, "
                + "is_read BOOLEAN NOT NULL DEFAULT FALSE, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)"
        };
        try (Connection c = get(); Statement st = c.createStatement()) {
            for (String sql : ddl) st.execute(sql);
            addColumnIfMissing(c, "donations", "donor_age", "INT NOT NULL DEFAULT 0");
            addColumnIfMissing(c, "donations", "weight_kg", "INT NOT NULL DEFAULT 0");
            addColumnIfMissing(c, "messages", "is_admin_reply", "BOOLEAN NOT NULL DEFAULT FALSE");
        }
        UserDao.ensureDefaultAdmin();
    }

    /** Lets an older database (created by an earlier version of the app) pick up new columns. */
    private static void addColumnIfMissing(Connection c, String table, String column, String definition)
            throws SQLException {
        String check = "SELECT 1 FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement ps = c.prepareStatement(check)) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return;
            }
        }
        try (Statement st = c.createStatement()) {
            st.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }
}
