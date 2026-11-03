package com.bloodcare.db;

import com.bloodcare.model.BloodRequest;
import com.bloodcare.model.DonorEligibilityChecker;
import com.bloodcare.model.Location;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RequestDao - SQL for the blood_requests table. Rows are returned as Object[] for JTable.
 * The status rules themselves live in the BloodRequest class; this DAO loads the request, asks the object
 * to move, and saves the result together with the side effects (stock, notification) in one transaction.
 */
public final class RequestDao {
    private static final String DATE = "DATE_FORMAT(r.created_at,'%Y-%m-%d %H:%i')";
    private static final String OPEN = "status IN ('PENDING','APPROVED')";

    private RequestDao() {}

    /**
     * Saves a new request, tells the admins, and immediately alerts the donors who live in the same Kerala
     * district: users with the same blood group (not the requester) who have not donated in the last
     * waiting period. All in one transaction. Returns how many donors were alerted.
     */
    public static int add(BloodRequest request) throws SQLException {
        String sql = "INSERT INTO blood_requests (user_id, patient_name, blood_group, units, hospital, city, urgency) "
                + "VALUES (?,?,?,?,?,?,?)";
        String alert = "INSERT INTO notifications (user_id, message) SELECT u.id, ? FROM users u "
                + "WHERE u.role = 'USER' AND u.id <> ? AND u.blood_group = ? AND u.city = ? "
                + "AND NOT EXISTS (SELECT 1 FROM donations x WHERE x.user_id = u.id AND x.status = 'APPROVED' "
                + "AND x.donation_date > CURDATE() - INTERVAL " + DonorEligibilityChecker.DEFAULT.getWaitDays() + " DAY)";
        try (Connection c = Db.get()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, request.getRequesterId());
                    ps.setString(2, request.getPatientName());
                    ps.setString(3, request.getBloodGroup());
                    ps.setInt(4, request.getUnits());
                    ps.setString(5, request.getLocation().getPlace());
                    ps.setString(6, request.getLocation().getDistrict());
                    ps.setString(7, request.getUrgency());
                    ps.executeUpdate();
                }
                NotificationDao.toAdmins(c, request.adminSummary());
                int alerted;
                try (PreparedStatement ps = c.prepareStatement(alert)) {
                    ps.setString(1, request.alertMessage());
                    ps.setInt(2, request.getRequesterId());
                    ps.setString(3, request.getBloodGroup());
                    ps.setString(4, request.getLocation().getDistrict());
                    alerted = ps.executeUpdate();
                }
                c.commit();
                return alerted;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** Rows: id, patient, group, units, hospital, district, urgency, status, date. */
    public static List<Object[]> forUser(int userId) throws SQLException {
        return Db.queryRows("SELECT r.id, r.patient_name, r.blood_group, r.units, r.hospital, r.city, r.urgency, r.status, "
                + DATE + " FROM blood_requests r WHERE r.user_id = ? ORDER BY r.id DESC", userId);
    }

    /** Rows: id, requester, patient, group, units, hospital, district, urgency, status, date. Open ones first. */
    public static List<Object[]> all() throws SQLException {
        return Db.queryRows("SELECT r.id, u.name, r.patient_name, r.blood_group, r.units, r.hospital, r.city, "
                + "r.urgency, r.status, " + DATE + " FROM blood_requests r JOIN users u ON u.id = r.user_id "
                + "ORDER BY FIELD(r.status,'PENDING','APPROVED','FULFILLED','REJECTED','CANCELLED'), "
                + "FIELD(r.urgency,'EMERGENCY','NORMAL'), r.id DESC");
    }

    /** Approved requests that can be fulfilled by handing blood over from a bank. */
    public static List<Object[]> openForFulfillment(String bloodGroup) throws SQLException {
        return Db.queryRows("SELECT id, patient_name, blood_group, units, hospital FROM blood_requests "
                + "WHERE status = 'APPROVED' AND blood_group = ? ORDER BY id DESC", bloodGroup);
    }

    /**
     * Fulfils one approved request from one selected bank. Stock deduction, request status change,
     * notification, and inventory note are committed together.
     */
    public static String fulfillFromBank(int requestId, int bankId) throws SQLException {
        try (Connection c = Db.get()) {
            c.setAutoCommit(false);
            try {
                int userId, units;
                String patient, group;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT user_id, patient_name, blood_group, units, status FROM blood_requests WHERE id = ? FOR UPDATE")) {
                    ps.setInt(1, requestId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) { c.rollback(); return "Request not found."; }
                        userId = rs.getInt(1);
                        patient = rs.getString(2);
                        group = rs.getString(3);
                        units = rs.getInt(4);
                        if (!"APPROVED".equals(rs.getString(5))) {
                            c.rollback();
                            return "Only an APPROVED request can be fulfilled here.";
                        }
                    }
                }
                int available = InventoryDao.stockOfBank(c, bankId, group);
                if (available < units) {
                    c.rollback();
                    return "This bank has only " + available + " unit(s) of " + group + ". Need " + units + ".";
                }
                InventoryDao.add(c, bankId, group, -units, "Request #" + requestId + " fulfilled for " + patient);
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE blood_requests SET status = 'FULFILLED' WHERE id = ? AND status = 'APPROVED'")) {
                    ps.setInt(1, requestId);
                    ps.executeUpdate();
                }
                NotificationDao.toUser(c, userId, "Your blood request #" + requestId + " for " + patient
                        + " has been fulfilled.");
                c.commit();
                return null;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** Latest requests for the dashboard. Rows: group, urgency, patient, district, created_at (Timestamp). */
    public static List<Object[]> recent(int limit) throws SQLException {
        return Db.queryRows("SELECT blood_group, urgency, patient_name, city, created_at FROM blood_requests "
                + "ORDER BY id DESC LIMIT ?", limit);
    }

    /**
     * Admin changes a request's status. One transaction, because several things must succeed together:
     * load the request, let the BloodRequest object check the move, (for FULFILLED) check and deduct stock,
     * save the status, notify the owner. Returns null on success, or a message explaining why it was refused.
     */
    public static String setStatus(int requestId, String newStatus) throws SQLException {
        try (Connection c = Db.get()) {
            c.setAutoCommit(false);
            try {
                BloodRequest request;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT user_id, patient_name, blood_group, units, hospital, city, urgency, status "
                                + "FROM blood_requests WHERE id = ? FOR UPDATE")) {
                    ps.setInt(1, requestId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            c.rollback();
                            return "Request not found.";
                        }
                        request = BloodRequest.restore(requestId, rs.getInt(1), rs.getString(2), rs.getString(3),
                                rs.getInt(4), Location.restore(rs.getString(6), rs.getString(5)),
                                rs.getString(7), rs.getString(8));
                    }
                }
                String problem = request.moveTo(newStatus);
                if (problem != null) {
                    c.rollback();
                    return problem;
                }
                if (newStatus.equals(BloodRequest.FULFILLED)) {
                    int stock = InventoryDao.stockOf(c, request.getBloodGroup());
                    if (stock < request.getUnits()) {
                        c.rollback();
                        return "Not enough " + request.getBloodGroup() + " blood in stock: need "
                                + request.getUnits() + ", have " + stock + ".";
                    }
                    InventoryDao.add(c, null, request.getBloodGroup(), -request.getUnits(),
                            "Request #" + requestId + " fulfilled");
                }
                try (PreparedStatement ps = c.prepareStatement("UPDATE blood_requests SET status = ? WHERE id = ?")) {
                    ps.setString(1, request.getStatus());
                    ps.setInt(2, requestId);
                    ps.executeUpdate();
                }
                NotificationDao.toUser(c, request.getRequesterId(),
                        "Your blood request #" + requestId + " is now " + request.getStatus() + ".");
                c.commit();
                return null;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** A user may cancel only their own request, and only while it is still PENDING. */
    public static boolean cancelOwn(int requestId, int userId) throws SQLException {
        return Db.update("UPDATE blood_requests SET status='CANCELLED' WHERE id=? AND user_id=? AND status='PENDING'",
                requestId, userId) > 0;
    }

    public static int countByStatus(String status) throws SQLException {
        return Db.queryInt("SELECT COUNT(*) FROM blood_requests WHERE status = ?", status);
    }

    /** Requests still waiting for blood (PENDING or APPROVED). */
    public static int countActive() throws SQLException {
        return Db.queryInt("SELECT COUNT(*) FROM blood_requests WHERE " + OPEN);
    }

    public static int countActiveEmergency() throws SQLException {
        return Db.queryInt("SELECT COUNT(*) FROM blood_requests WHERE " + OPEN + " AND urgency = 'EMERGENCY'");
    }

    /** Units still needed per blood group by PENDING/APPROVED requests. */
    public static Map<String, Integer> openDemandByGroup() throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (Object[] r : Db.queryRows("SELECT blood_group, SUM(units) FROM blood_requests WHERE " + OPEN
                + " GROUP BY blood_group")) {
            map.put((String) r[0], (Integer) r[1]);
        }
        return map;
    }

    /** Rows: status, number of requests, units. */
    public static List<Object[]> byStatus() throws SQLException {
        return Db.queryRows("SELECT status, COUNT(*), SUM(units) FROM blood_requests GROUP BY status ORDER BY status");
    }
}
