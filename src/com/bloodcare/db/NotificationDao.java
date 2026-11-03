package com.bloodcare.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

/**
 * NotificationDao - the notifications table.
 * user_id = NULL means the message belongs to the admin inbox.
 * The versions that take a Connection are used inside other DAOs' transactions,
 * so the notification is saved together with the change that caused it.
 */
public final class NotificationDao {
    private static final String LIST = "SELECT id, message, DATE_FORMAT(created_at,'%Y-%m-%d %H:%i'), "
            + "IF(is_read,'READ','NEW') FROM notifications WHERE ";

    private NotificationDao() {}

    public static void toAdmins(Connection c, String message) throws SQLException {
        insert(c, null, message);
    }

    public static void toUser(Connection c, int userId, String message) throws SQLException {
        insert(c, userId, message);
    }

    private static void insert(Connection c, Integer userId, String message) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO notifications (user_id, message) VALUES (?,?)")) {
            if (userId == null) ps.setNull(1, Types.INTEGER);
            else ps.setInt(1, userId);
            ps.setString(2, message);
            ps.executeUpdate();
        }
    }

    /** Sends one message to every USER account, or only to users of one blood group. Returns how many got it. */
    public static int broadcast(String message, String bloodGroupOrNull) throws SQLException {
        String sql = "INSERT INTO notifications (user_id, message) SELECT id, ? FROM users WHERE role='USER'";
        return bloodGroupOrNull == null
                ? Db.update(sql, message)
                : Db.update(sql + " AND blood_group = ?", message, bloodGroupOrNull);
    }

    /** Rows: id, message, received, status (NEW / READ). */
    public static List<Object[]> forUser(int userId) throws SQLException {
        return Db.queryRows(LIST + "user_id = ? ORDER BY id DESC", userId);
    }

    public static List<Object[]> forAdmins() throws SQLException {
        return Db.queryRows(LIST + "user_id IS NULL ORDER BY id DESC");
    }

    public static int unreadForUser(int userId) throws SQLException {
        return Db.queryInt("SELECT COUNT(*) FROM notifications WHERE is_read = FALSE AND user_id = ?", userId);
    }

    public static int unreadForAdmins() throws SQLException {
        return Db.queryInt("SELECT COUNT(*) FROM notifications WHERE is_read = FALSE AND user_id IS NULL");
    }

    public static void markAllReadForUser(int userId) throws SQLException {
        Db.update("UPDATE notifications SET is_read = TRUE WHERE user_id = ?", userId);
    }

    public static void markAllReadForAdmins() throws SQLException {
        Db.update("UPDATE notifications SET is_read = TRUE WHERE user_id IS NULL");
    }
}
