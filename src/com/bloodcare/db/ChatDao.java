package com.bloodcare.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * ChatDao - request-specific conversations between a requester and a donor who accepted.
 * A conversation is identified by the request_responses row. Only those two people can read
 * or write in it; every method re-checks that with the signed-in user's id.
 */
public final class ChatDao {
    public static final int MAX_LENGTH = 500;

    private ChatDao() {}

    /** Conversations the user takes part in. Rows: id, request, with, patient, group, status, unread. */
    public static List<Object[]> conversations(int userId) throws SQLException {
        String sql = "SELECT r.id, q.id, CASE WHEN r.donor_id = ? THEN o.name ELSE d.name END, q.patient_name, "
                + "q.blood_group, q.status, "
                + "(SELECT COUNT(*) FROM chat_messages m WHERE m.response_id = r.id AND m.sender_id <> ? AND m.is_read = FALSE) "
                + "FROM request_responses r JOIN blood_requests q ON q.id = r.request_id "
                + "JOIN users o ON o.id = q.user_id JOIN users d ON d.id = r.donor_id "
                + "WHERE r.status = 'ACCEPTED' AND (r.donor_id = ? OR q.user_id = ?) ORDER BY r.id DESC";
        List<Object[]> rows = Db.queryRows(sql, userId, userId, userId, userId);
        for (Object[] row : rows) row[2] = firstName((String) row[2]); // show first names only
        return rows;
    }

    /** Is this user the requester or the accepted donor of this conversation? */
    private static boolean isParticipant(Connection c, int responseId, int userId) throws SQLException {
        String sql = "SELECT 1 FROM request_responses r JOIN blood_requests q ON q.id = r.request_id "
                + "WHERE r.id = ? AND r.status = 'ACCEPTED' AND (r.donor_id = ? OR q.user_id = ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, responseId);
            ps.setInt(2, userId);
            ps.setInt(3, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Messages in time order. Rows: sender first name, mine (Boolean), body, time. Empty for non-participants. */
    public static List<Object[]> history(int responseId, int userId) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        try (Connection c = Db.get()) {
            if (!isParticipant(c, responseId, userId)) return rows;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT u.name, m.sender_id, m.body, DATE_FORMAT(m.created_at,'%d %b %H:%i') FROM chat_messages m "
                            + "JOIN users u ON u.id = m.sender_id WHERE m.response_id = ? ORDER BY m.id")) {
                ps.setInt(1, responseId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        rows.add(new Object[]{firstName(rs.getString(1)), rs.getInt(2) == userId, rs.getString(3), rs.getString(4)});
                    }
                }
            }
            // opening the conversation marks the other person's messages as read
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE chat_messages SET is_read = TRUE WHERE response_id = ? AND sender_id <> ?")) {
                ps.setInt(1, responseId);
                ps.setInt(2, userId);
                ps.executeUpdate();
            }
        }
        return rows;
    }

    /** Sends a message. Returns null on success, or why it was refused. */
    public static String send(int responseId, int userId, String body) throws SQLException {
        if (body.isEmpty() || body.length() > MAX_LENGTH) return "Message must be between 1 and " + MAX_LENGTH + " characters.";
        try (Connection c = Db.get()) {
            if (!isParticipant(c, responseId, userId)) return "You are not part of this conversation.";
            int donorId, ownerId, requestId;
            String requestStatus;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT r.donor_id, q.user_id, q.status, q.id FROM request_responses r "
                            + "JOIN blood_requests q ON q.id = r.request_id WHERE r.id = ?")) {
                ps.setInt(1, responseId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    donorId = rs.getInt(1);
                    ownerId = rs.getInt(2);
                    requestStatus = rs.getString(3);
                    requestId = rs.getInt(4);
                }
            }
            if (requestStatus.equals("REJECTED") || requestStatus.equals("CANCELLED")) {
                return "This request is " + requestStatus + ", so the conversation is closed.";
            }
            String senderName;
            try (PreparedStatement ps = c.prepareStatement("SELECT name FROM users WHERE id = ?")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    senderName = firstName(rs.getString(1));
                }
            }
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO chat_messages (response_id, sender_id, body) VALUES (?,?,?)")) {
                    ps.setInt(1, responseId);
                    ps.setInt(2, userId);
                    ps.setString(3, body);
                    ps.executeUpdate();
                }
                int other = userId == donorId ? ownerId : donorId;
                NotificationDao.toUser(c, other, "New message from " + senderName + " about request #" + requestId + ".");
                c.commit();
                return null;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** Unread chat messages waiting for this user (for the sidebar badge). */
    public static int unreadFor(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM chat_messages m JOIN request_responses r ON r.id = m.response_id "
                + "JOIN blood_requests q ON q.id = r.request_id "
                + "WHERE m.is_read = FALSE AND m.sender_id <> ? AND (r.donor_id = ? OR q.user_id = ?)";
        return Db.queryInt(sql, userId, userId, userId);
    }

    /** Only first names are shown to the other person, to protect privacy. */
    private static String firstName(String full) {
        return full.split(" ")[0];
    }
}
