package com.bloodcare.db;

import com.bloodcare.model.Donation;
import com.bloodcare.model.DonationHistory;
import com.bloodcare.model.DonorEligibilityChecker;
import com.bloodcare.model.EligibilityChecker;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DonationDao - SQL for the donations table.
 * A donation offer starts PENDING. The admin checks eligibility (decided by an EligibilityChecker object)
 * and then approves or rejects it. Approving adds the units to the blood stock in the same transaction.
 */
public final class DonationDao {
    private static final EligibilityChecker CHECKER = DonorEligibilityChecker.DEFAULT;
    private static final String COLUMNS = "id, user_id, blood_group, units, donation_date, donor_age, weight_kg, status";

    private DonationDao() {}

    public static void add(Donation donation) throws SQLException {
        String sql = "INSERT INTO donations (user_id, blood_group, units, donation_date, donor_age, weight_kg) "
                + "VALUES (?,?,?,?,?,?)";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, donation.getDonorId());
            ps.setString(2, donation.getBloodGroup());
            ps.setInt(3, donation.getUnits());
            ps.setDate(4, Date.valueOf(donation.getDate()));
            ps.setInt(5, donation.getDonorAge());
            ps.setInt(6, donation.getWeightKg());
            ps.executeUpdate();
            NotificationDao.toAdmins(c, "New donation offer: " + donation.getUnits() + " unit(s) of "
                    + donation.getBloodGroup() + " on " + donation.getDate() + ". Please verify eligibility.");
        }
    }

    private static Donation read(ResultSet rs) throws SQLException {
        return Donation.restore(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getInt(4),
                rs.getDate(5).toLocalDate(), rs.getInt(6), rs.getInt(7), rs.getString(8));
    }

    /** All donations of one donor, newest first, as a DonationHistory object. */
    public static DonationHistory historyFor(int userId) throws SQLException {
        DonationHistory history = new DonationHistory(userId);
        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM donations WHERE user_id = ? ORDER BY id DESC")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) history.addDonation(read(rs));
            }
        }
        return history;
    }

    /** Latest approved donation date, used to show the same waiting-period rule before donor actions. */
    public static LocalDate lastApprovedDate(int userId) throws SQLException {
        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT MAX(donation_date) FROM donations WHERE user_id = ? AND status = 'APPROVED'")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Date date = rs.getDate(1);
                return date == null ? null : date.toLocalDate();
            }
        }
    }

    /** Rows for the donor's own table: id, group, units, date, status. */
    public static List<Object[]> forUser(int userId) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        for (Donation d : historyFor(userId).getDonations()) {
            rows.add(new Object[]{d.getId(), d.getBloodGroup(), d.getUnits(), d.getDate().toString(), d.getStatus()});
        }
        return rows;
    }

    /** Rows: id, donor, group, units, date, status. */
    public static List<Object[]> all() throws SQLException {
        return Db.queryRows("SELECT d.id, u.name, d.blood_group, d.units, DATE_FORMAT(d.donation_date,'%Y-%m-%d'), d.status "
                + "FROM donations d JOIN users u ON u.id = d.user_id "
                + "ORDER BY FIELD(d.status,'PENDING','APPROVED','REJECTED'), d.id DESC");
    }

    /** Latest donations for the dashboard. Rows: donor, group, date, status. */
    public static List<Object[]> recent(int limit) throws SQLException {
        return Db.queryRows("SELECT u.name, d.blood_group, DATE_FORMAT(d.donation_date,'%d %b %Y'), d.status "
                + "FROM donations d JOIN users u ON u.id = d.user_id ORDER BY d.id DESC LIMIT ?", limit);
    }

    /** Pending offers with the eligibility result. Rows: id, donor, group, units, date, age, weight, last, eligible, reason. */
    public static List<Object[]> pendingWithEligibility() throws SQLException {
        String sql = "SELECT d.id, d.user_id, d.blood_group, d.units, d.donation_date, d.donor_age, d.weight_kg, d.status, "
                + "u.name, (SELECT MAX(x.donation_date) FROM donations x WHERE x.user_id = d.user_id AND x.status = 'APPROVED') "
                + "FROM donations d JOIN users u ON u.id = d.user_id WHERE d.status = 'PENDING' ORDER BY d.id";
        List<Object[]> rows = new ArrayList<>();
        try (Connection c = Db.get(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Donation d = read(rs);
                Date lastSql = rs.getDate(10);
                LocalDate last = lastSql == null ? null : lastSql.toLocalDate();
                String problem = CHECKER.getIneligibilityReason(d, last);
                rows.add(new Object[]{d.getId(), rs.getString(9), d.getBloodGroup(), d.getUnits(), d.getDate().toString(),
                        d.getDonorAge(), d.getWeightKg(), last == null ? "-" : last.toString(),
                        problem == null ? "YES" : "NO", problem == null ? "-" : problem});
            }
        }
        return rows;
    }

    /**
     * Approve or reject a PENDING offer. Approving re-checks eligibility on the server side (so it cannot be
     * bypassed from the screen) and adds the units to the stock, all in one transaction.
     * Returns null on success, or a message explaining why it was refused.
     */
    public static String setStatus(int donationId, String newStatus) throws SQLException {
        try (Connection c = Db.get()) {
            c.setAutoCommit(false);
            try {
                Donation donation;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT " + COLUMNS + " FROM donations WHERE id = ? FOR UPDATE")) {
                    ps.setInt(1, donationId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            c.rollback();
                            return "Donation not found.";
                        }
                        donation = read(rs);
                    }
                }
                if (!donation.isPending()) {
                    c.rollback();
                    return "This donation is already " + donation.getStatus() + ".";
                }
                if (newStatus.equals(Donation.APPROVED)) {
                    String problem = CHECKER.getIneligibilityReason(donation, lastApproved(c, donation.getDonorId()));
                    if (problem != null) {
                        c.rollback();
                        return "Donor is not eligible: " + problem + ".";
                    }
                    donation.approve();
                    InventoryDao.add(c, null, donation.getBloodGroup(), donation.getUnits(), "Donation #" + donationId);
                } else {
                    donation.reject();
                }
                try (PreparedStatement ps = c.prepareStatement("UPDATE donations SET status = ? WHERE id = ?")) {
                    ps.setString(1, donation.getStatus());
                    ps.setInt(2, donationId);
                    ps.executeUpdate();
                }
                NotificationDao.toUser(c, donation.getDonorId(), "Your donation offer #" + donationId + " was "
                        + donation.getStatus() + "." + (donation.isApproved() ? " Thank you for saving lives!" : ""));
                c.commit();
                return null;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private static LocalDate lastApproved(Connection c, int userId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT MAX(donation_date) FROM donations WHERE user_id = ? AND status = 'APPROVED'")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Date d = rs.getDate(1);
                return d == null ? null : d.toLocalDate();
            }
        }
    }

    public static int countByStatus(String status) throws SQLException {
        return Db.queryInt("SELECT COUNT(*) FROM donations WHERE status = ?", status);
    }

    /** Rows: status, number of donations, units. */
    public static List<Object[]> byStatus() throws SQLException {
        return Db.queryRows("SELECT status, COUNT(*), SUM(units) FROM donations GROUP BY status ORDER BY status");
    }
}
