package com.bloodcare.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * ResponseDao - donors answering approved blood requests (request_responses table).
 * A donor only sees APPROVED requests for their own blood group (matched by group only, the admin and
 * hospital still decide real suitability), can answer each request once, and never answers their own request.
 * These rules are checked here in the DAO, not just by hiding buttons.
 */
public final class ResponseDao {
    private ResponseDao() {}

    /**
     * Requests this donor could help with: same blood group, and either APPROVED (anywhere in Kerala) or still
     * PENDING but in the donor's own district (so nearby donors can react before the admin has reviewed it).
     * Own district first. Rows: id, patient, group, units, hospital, district, urgency, status, requested.
     */
    public static List<Object[]> openForDonor(int donorId) throws SQLException {
        String sql = "SELECT q.id, q.patient_name, q.blood_group, q.units, q.hospital, q.city, q.urgency, q.status, "
                + "DATE_FORMAT(q.created_at,'%Y-%m-%d %H:%i') FROM blood_requests q JOIN users d ON d.id = ? "
                + "WHERE (q.status = 'APPROVED' OR (q.status = 'PENDING' AND q.city = d.city)) "
                + "AND q.blood_group = d.blood_group AND q.user_id <> d.id "
                + "AND NOT EXISTS (SELECT 1 FROM request_responses r WHERE r.request_id = q.id AND r.donor_id = d.id) "
                + "ORDER BY (q.city = d.city) DESC, FIELD(q.urgency,'EMERGENCY','NORMAL'), q.id DESC";
        return Db.queryRows(sql, donorId);
    }

    /**
     * Accept or decline an approved request. One transaction: lock the request, check every rule, save the
     * answer, and (on accept) tell the requester and the admins. Returns null on success, or why it was refused.
     */
    public static String respond(int requestId, int donorId, boolean accept) throws SQLException {
        try (Connection c = Db.get()) {
            c.setAutoCommit(false);
            try {
                int ownerId;
                String group, status, patient, district;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT user_id, blood_group, status, patient_name, city FROM blood_requests WHERE id = ? FOR UPDATE")) {
                    ps.setInt(1, requestId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            c.rollback();
                            return "Request not found.";
                        }
                        ownerId = rs.getInt(1);
                        group = rs.getString(2);
                        status = rs.getString(3);
                        patient = rs.getString(4);
                        district = rs.getString(5);
                    }
                }
                if (!status.equals("APPROVED") && !status.equals("PENDING")) {
                    c.rollback();
                    return "This request is no longer open for donors.";
                }
                if (ownerId == donorId) {
                    c.rollback();
                    return "You cannot respond to your own request.";
                }
                String donorGroup, donorName, donorDistrict;
                try (PreparedStatement ps = c.prepareStatement("SELECT blood_group, name, city FROM users WHERE id = ?")) {
                    ps.setInt(1, donorId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            c.rollback();
                            return "Account not found.";
                        }
                        donorGroup = rs.getString(1);
                        donorName = rs.getString(2);
                        donorDistrict = rs.getString(3);
                    }
                }
                if (!group.equals(donorGroup)) {
                    c.rollback();
                    return "Your blood group does not match this request.";
                }
                // a request the admin has not approved yet can only be answered by donors in the same district
                if (status.equals("PENDING") && !district.equals(donorDistrict)) {
                    c.rollback();
                    return "This request is still waiting for admin approval.";
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT 1 FROM request_responses WHERE request_id = ? AND donor_id = ?")) {
                    ps.setInt(1, requestId);
                    ps.setInt(2, donorId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            c.rollback();
                            return "You have already responded to this request.";
                        }
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO request_responses (request_id, donor_id, status) VALUES (?,?,?)")) {
                    ps.setInt(1, requestId);
                    ps.setInt(2, donorId);
                    ps.setString(3, accept ? "ACCEPTED" : "DECLINED");
                    ps.executeUpdate();
                }
                if (accept) {
                    String first = donorName.split(" ")[0];
                    NotificationDao.toUser(c, ownerId, first + " offered to help with request #" + requestId
                            + " (" + patient + "). Open Conversations to chat.");
                    NotificationDao.toAdmins(c, first + " accepted request #" + requestId + " (" + group + ").");
                }
                c.commit();
                return null;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** Rows: id, request, patient, group, donor, requester, status, date. For the admin. */
    public static List<Object[]> all() throws SQLException {
        String sql = "SELECT r.id, q.id, q.patient_name, q.blood_group, d.name, o.name, r.status, "
                + "DATE_FORMAT(r.created_at,'%Y-%m-%d %H:%i') FROM request_responses r "
                + "JOIN blood_requests q ON q.id = r.request_id JOIN users d ON d.id = r.donor_id "
                + "JOIN users o ON o.id = q.user_id ORDER BY r.id DESC";
        return Db.queryRows(sql);
    }
}
