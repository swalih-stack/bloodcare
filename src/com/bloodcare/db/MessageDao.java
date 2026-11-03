package com.bloodcare.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * MessageDao - conversations between a user and the administrator. A message row records
 * both the text and whether it was sent by the administrator, allowing a complete thread.
 */
public final class MessageDao {
    public static final int MAX_LENGTH = 500;

    private MessageDao() {}

    /** Saves a message from the given (signed-in) user and alerts the admins, in one transaction. */
    public static void send(int userId, String body) throws SQLException {
        try (Connection c = Db.get()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO messages (user_id, body) VALUES (?,?)")) {
                    ps.setInt(1, userId);
                    ps.setString(2, body);
                    ps.executeUpdate();
                }
                String name = "a user";
                try (PreparedStatement ps = c.prepareStatement("SELECT name FROM users WHERE id = ?")) {
                    ps.setInt(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) name = rs.getString(1);
                    }
                }
                NotificationDao.toAdmins(c, "New message from " + name + ".");
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** One user's Contact Admin thread. Rows: id, sender, message, sent. */
    public static List<Object[]> forUser(int userId) throws SQLException {
        return Db.queryRows("SELECT id, IF(is_admin_reply,'Admin','You'), body, "
                + "DATE_FORMAT(created_at,'%Y-%m-%d %H:%i') FROM messages WHERE user_id = ? ORDER BY id", userId);
    }

    /** One inbox row per user who has contacted the administrator. Rows: user id, name, email. */
    public static List<Object[]> adminConversations() throws SQLException {
        return Db.queryRows("SELECT u.id, u.name, u.email FROM users u JOIN messages m ON m.user_id = u.id "
                + "WHERE m.is_admin_reply = FALSE GROUP BY u.id, u.name, u.email "
                + "ORDER BY MAX(m.created_at) DESC");
    }

    public static int unreadForAdmin() throws SQLException {
        return Db.queryInt("SELECT COUNT(*) FROM messages WHERE is_read = FALSE AND is_admin_reply = FALSE");
    }

    /** Marks every incoming message in one user's thread as read. */
    public static void markConversationRead(int userId) throws SQLException {
        Db.update("UPDATE messages SET is_read = TRUE WHERE user_id = ? AND is_admin_reply = FALSE", userId);
    }

    /** Rows: sender label, text, sent time. Used by the administrator's conversation view. */
    public static List<Object[]> conversationForAdmin(int userId) throws SQLException {
        return Db.queryRows("SELECT IF(is_admin_reply, 'You', 'User'), body, "
                + "DATE_FORMAT(created_at,'%Y-%m-%d %H:%i') FROM messages WHERE user_id = ? ORDER BY id", userId);
    }

    /** Adds an administrator reply to a user's conversation and notifies that user. */
    public static String sendAdminReply(int userId, String replyText) throws SQLException {
        if (replyText == null || replyText.isBlank() || replyText.length() > MAX_LENGTH) {
            return "Message must be between 1 and " + MAX_LENGTH + " characters.";
        }
        try (Connection c = Db.get()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM users WHERE id = ?")) {
                    ps.setInt(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            c.rollback();
                            return "User not found.";
                        }
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO messages (user_id, body, is_admin_reply, is_read) VALUES (?,?,TRUE,TRUE)")) {
                    ps.setInt(1, userId);
                    ps.setString(2, replyText.trim());
                    ps.executeUpdate();
                }
                NotificationDao.toUser(c, userId, "New reply from the administrator. Open Contact Admin to view it.");
                c.commit();
                return null;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }
}
