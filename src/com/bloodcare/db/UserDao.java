package com.bloodcare.db;

import com.bloodcare.model.User;
import com.bloodcare.model.BloodGroups;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** UserDao - all SQL for the users table. Uses PreparedStatement everywhere. */
public final class UserDao {
    private UserDao() {}

    private static User map(ResultSet rs) throws SQLException {
        return User.create(rs.getInt("id"), rs.getString("role"), rs.getString("name"), rs.getString("email"),
                rs.getString("phone"), rs.getString("blood_group"), rs.getString("city"));
    }

    public static boolean emailExists(String email) throws SQLException {
        return Db.queryInt("SELECT COUNT(*) FROM users WHERE email = ?", email.toLowerCase()) > 0;
    }

    /** Inserts a new account with the given role (USER or ADMIN). */
    public static void create(String name, String email, String phone, String bloodGroup,
                              String city, String password, String role) throws SQLException {
        Db.update("INSERT INTO users (name, email, phone, blood_group, city, password_hash, role) VALUES (?,?,?,?,?,?,?)",
                name, email.toLowerCase(), phone, bloodGroup, city, Passwords.hash(password), role);
    }

    /** Returns the user if email+password are right, otherwise null. */
    public static User login(String email, String password) throws SQLException {
        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM users WHERE email = ?")) {
            ps.setString(1, email.toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && Passwords.matches(password, rs.getString("password_hash"))) return map(rs);
            }
        }
        return null;
    }

    /** Saves the profile fields of a User object (already validated by User.updateProfile). */
    public static void updateProfile(User user) throws SQLException {
        Db.update("UPDATE users SET name=?, phone=?, blood_group=?, city=? WHERE id=?",
                user.getName(), user.getPhone(), user.getBloodGroup(), user.getDistrict(), user.getId());
    }

    public static List<User> findAll() throws SQLException {
        List<User> list = new ArrayList<>();
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement("SELECT * FROM users ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public static int countUsers() throws SQLException {
        return Db.queryInt("SELECT COUNT(*) FROM users WHERE role = 'USER'");
    }

    public static int countDonors() throws SQLException {
        return Db.queryInt("SELECT COUNT(DISTINCT user_id) FROM donations");
    }

    /**
     * Other users whose blood can be donated to the recipient's group. The requester is
     * intentionally excluded from their own matching list.
     */
    public static List<Object[]> compatibleDonors(String recipientGroup, String district, int requesterId) throws SQLException {
        String[] groups = BloodGroups.compatibleDonorGroups(recipientGroup);
        if (groups.length == 0) return List.of();
        String marks = String.join(",", java.util.Collections.nCopies(groups.length, "?"));
        Object[] params = new Object[groups.length + 2];
        System.arraycopy(groups, 0, params, 0, groups.length);
        params[groups.length] = district;
        params[groups.length + 1] = requesterId;
        return Db.queryRows("SELECT name, blood_group, city FROM users "
                + "WHERE role = 'USER' AND blood_group IN (" + marks + ") AND city = ? AND id <> ? "
                + "ORDER BY blood_group, name", params);
    }

    public static int newUsersThisWeek() throws SQLException {
        return Db.queryInt("SELECT COUNT(*) FROM users WHERE role='USER' AND created_at >= NOW() - INTERVAL 7 DAY");
    }

    /** Users who have offered at least one donation. Rows: id, name, email, phone, group, district, offers, approved units. */
    public static List<Object[]> donors() throws SQLException {
        return Db.queryRows("SELECT u.id, u.name, u.email, u.phone, u.blood_group, u.city, COUNT(d.id), "
                + "COALESCE(SUM(CASE WHEN d.status='APPROVED' THEN d.units END),0) "
                + "FROM users u JOIN donations d ON d.user_id = u.id GROUP BY u.id ORDER BY u.id");
    }

    /** Users who have asked for blood. Rows: id, name, email, phone, group, district, requests, open requests. */
    public static List<Object[]> seekers() throws SQLException {
        return Db.queryRows("SELECT u.id, u.name, u.email, u.phone, u.blood_group, u.city, COUNT(r.id), "
                + "COALESCE(SUM(r.status IN ('PENDING','APPROVED')),0) "
                + "FROM users u JOIN blood_requests r ON r.user_id = u.id GROUP BY u.id ORDER BY u.id");
    }

    /** Changes the password only if the old one is correct. Returns false if the old password was wrong. */
    public static boolean changePassword(int id, String oldPassword, String newPassword) throws SQLException {
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement("SELECT password_hash FROM users WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !Passwords.matches(oldPassword, rs.getString(1))) return false;
            }
        }
        Db.update("UPDATE users SET password_hash = ? WHERE id = ?", Passwords.hash(newPassword), id);
        return true;
    }

    /** Creates admin@bloodcare.com / admin123 the first time the app runs. */
    static void ensureDefaultAdmin() throws SQLException {
        if (!emailExists("admin@bloodcare.com")) {
            create("Administrator", "admin@bloodcare.com", "", "O+", "", "admin123", "ADMIN");
        }
        if (!emailExists("admin2@bloodcare.com")) {
            create("Administrator2", "admin2@bloodcare.com", "", "O+", "", "admin123", "ADMIN");
        }
    }
}
