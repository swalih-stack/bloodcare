package com.bloodcare.db;

import com.bloodcare.model.BloodInventory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * InventoryDao - blood stock. The blood_inventory table is a ledger: every row adds
 * (+) or removes (-) units, and the stock of a group is the SUM of its rows.
 * Approved donations add units; fulfilled requests remove units.
 */
public final class InventoryDao {
    private InventoryDao() {}

    public static void add(Integer bankId, String group, int units, String note) throws SQLException {
        try (Connection c = Db.get()) {
            add(c, bankId, group, units, note);
        }
    }

    /** Same as above but on an existing connection, so it can be part of a transaction. */
    public static void add(Connection c, Integer bankId, String group, int units, String note) throws SQLException {
        String sql = "INSERT INTO blood_inventory (bank_id, blood_group, units, note) VALUES (?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            if (bankId == null) ps.setNull(1, Types.INTEGER);
            else ps.setInt(1, bankId);
            ps.setString(2, group);
            ps.setInt(3, units);
            ps.setString(4, note);
            ps.executeUpdate();
        }
    }

    public static int stockOf(Connection c, String group) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COALESCE(SUM(units),0) FROM blood_inventory WHERE blood_group = ?")) {
            ps.setString(1, group);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Available units of one blood group held by one particular blood bank. */
    public static int stockOfBank(Connection c, int bankId, String group) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COALESCE(SUM(units),0) FROM blood_inventory WHERE bank_id = ? AND blood_group = ?")) {
            ps.setInt(1, bankId);
            ps.setString(2, group);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /**
     * Removes stock from one selected bank. The availability check and the negative ledger entry
     * happen in a single transaction, so a bank's balance can never be reduced below zero here.
     * Returns null when successful, otherwise a readable reason.
     */
    public static String removeFromBank(int bankId, String group, int units) throws SQLException {
        try (Connection c = Db.get()) {
            c.setAutoCommit(false);
            try {
                int available = stockOfBank(c, bankId, group);
                if (available < units) {
                    c.rollback();
                    return "This bank has only " + available + " unit(s) of " + group + ".";
                }
                add(c, bankId, group, -units, "Removed by admin");
                c.commit();
                return null;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** The current stock of all eight groups as a BloodInventory object. */
    public static BloodInventory load() throws SQLException {
        Map<String, Integer> stock = new HashMap<>();
        for (Object[] r : Db.queryRows("SELECT blood_group, SUM(units) FROM blood_inventory GROUP BY blood_group")) {
            stock.put((String) r[0], (Integer) r[1]);
        }
        return new BloodInventory(stock);
    }

    /** Rows: id, bank, group, units (+/-), note, date. */
    public static List<Object[]> log() throws SQLException {
        return Db.queryRows("SELECT i.id, COALESCE(b.name,'Direct (no bank)'), i.blood_group, i.units, i.note, "
                + "DATE_FORMAT(i.created_at,'%Y-%m-%d %H:%i') FROM blood_inventory i "
                + "LEFT JOIN blood_banks b ON b.id = i.bank_id ORDER BY i.id DESC");
    }
}
