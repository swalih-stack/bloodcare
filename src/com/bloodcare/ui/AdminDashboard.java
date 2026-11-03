package com.bloodcare.ui;

import com.bloodcare.db.BankDao;
import com.bloodcare.db.DonationDao;
import com.bloodcare.db.MessageDao;
import com.bloodcare.db.NotificationDao;
import com.bloodcare.db.RequestDao;
import com.bloodcare.db.ResponseDao;
import com.bloodcare.db.UserDao;
import com.bloodcare.model.Admin;
import com.bloodcare.model.User;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.bloodcare.ui.Sidebar.Entry;

/**
 * AdminDashboard - opened only when the signed-in account has role ADMIN.
 * Pages: Dashboard, Users (All / Donors / Blood Seekers), Requests, Donations, Blood Inventory,
 * Blood Banks, Eligibility Verification, Notifications, Reports & Analytics, Settings.
 * Business rules (stock check, eligibility check) live in the DAOs, not in these buttons.
 */
public class AdminDashboard extends DashboardShell {

    public AdminDashboard(MainFrame frame, Admin admin) {
        super(frame, "Blood Donation System", admin.getName(), admin.getDisplayRole(), entries(),
                AdminDashboard::unreadAdmin, "notifications");
        Runnable changed = this::refreshBadges;

        addPage("dashboard", "Admin Dashboard", "Manage users, requests, donations and blood inventory",
                new AdminHome(this::show, changed));

        addPage("users_all", "All Users", "Everyone registered in BloodCare",
                new DataPage(AdminDashboard::allUsers, changed, "ID", "Name", "Email", "Phone", "Group", "District", "Role"));
        addPage("donors", "Donors", "Users who have offered to donate",
                new DataPage(UserDao::donors, changed, "ID", "Name", "Email", "Phone", "Group", "District",
                        "Offers", "Approved units"));
        addPage("seekers", "Blood Seekers", "Users who have requested blood",
                new DataPage(UserDao::seekers, changed, "ID", "Name", "Email", "Phone", "Group", "District",
                        "Requests", "Open requests"));

        DataPage requests = new DataPage(RequestDao::all, changed, "ID", "Requested by", "Patient", "Group", "Units",
                "Hospital", "District", "Urgency", "Status", "Requested");
        requests.addAction("Approve", () -> changeRequest(requests, "APPROVED"));
        requests.addAction("Reject", () -> changeRequest(requests, "REJECTED"));
        addPage("requests", "Blood Requests", "Review requests; fulfill approved requests from Blood Inventory", requests);

        addPage("responses", "Donor Responses", "Donors who accepted or declined approved requests",
                new DataPage(ResponseDao::all, changed, "ID", "Request", "Patient", "Group", "Donor", "Requester",
                        "Status", "Date"));

        addPage("donations", "Donations", "History of all donation offers",
                new DataPage(DonationDao::all, changed, "ID", "Donor", "Group", "Units", "Date", "Status"));

        addPage("inventory", "Blood Inventory", "Stock by group and every change made to it", new InventoryPage(changed));

        DataPage banks = new DataPage(BankDao::all, changed, "ID", "Name", "District", "Phone", "Address", "Units in stock");
        banks.addAction("Add Blood Bank", () -> AdminActions.addBank(banks));
        banks.addAction("Delete Selected", () -> deleteBank(banks));
        addPage("banks", "Blood Banks", "Partner blood banks and the units they hold", banks);

        DataPage eligibility = new DataPage(DonationDao::pendingWithEligibility, changed, "ID", "Donor", "Group", "Units",
                "Date", "Age", "Weight (kg)", "Last donation", "Eligible", "Reason");
        eligibility.addAction("Approve", () -> changeDonation(eligibility, "APPROVED"));
        eligibility.addAction("Reject", () -> changeDonation(eligibility, "REJECTED"));
        addPage("eligibility", "Eligibility Verification",
                "Pending donation offers checked against age, weight and 90-day rules", eligibility);

        addPage("messages", "Conversations", "Select a user to read and reply in one conversation", new AdminConversationPage(changed));

        DataPage notifications = new DataPage(NotificationDao::forAdmins, changed, "ID", "Message", "Received", "Status");
        notifications.addAction("Send Notification", () -> AdminActions.sendNotification(notifications));
        notifications.addAction("Mark All Read", () -> {
            try {
                NotificationDao.markAllReadForAdmins();
            } catch (SQLException e) {
                Ui.dbError(notifications, e);
            }
        });
        addPage("notifications", "Notifications", "New requests and donation offers", notifications);

        addPage("reports", "Reports & Analytics", "Stock, demand and activity summary", new ReportsPage());
        addPage("settings", "Settings", "Your details and password", new ProfilePage(admin));

        show("dashboard");
    }

    private static List<Entry> entries() {
        return List.of(
                Entry.of("dashboard", "Dashboard", "▣"),
                Entry.of("users", "Users", "☻").with(
                        Entry.of("users_all", "All Users", ""),
                        Entry.of("donors", "Donors", ""),
                        Entry.of("seekers", "Blood Seekers", "")),
                Entry.of("requests", "Requests", "✚").withBadge(AdminDashboard::pendingRequests),
                Entry.of("responses", "Donor Responses", "❤"),
                Entry.of("donations", "Donations", "♥"),
                Entry.of("inventory", "Blood Inventory", "▤"),
                Entry.of("banks", "Blood Banks", "⌂"),
                Entry.of("eligibility", "Eligibility Verification", "✔").withBadge(AdminDashboard::pendingDonations),
                Entry.of("messages", "Conversations", "☎").withBadge(AdminDashboard::unreadMessages),
                Entry.of("notifications", "Notifications", "✉").withBadge(AdminDashboard::unreadAdmin),
                Entry.of("reports", "Reports & Analytics", "▥"),
                Entry.of("settings", "Settings", "⚙"));
    }

    // ---- small count helpers for badges (a failed query just shows 0) ----

    private static int pendingRequests() {
        try {
            return RequestDao.countByStatus("PENDING");
        } catch (SQLException e) {
            return 0;
        }
    }

    private static int pendingDonations() {
        try {
            return DonationDao.countByStatus("PENDING");
        } catch (SQLException e) {
            return 0;
        }
    }

    private static int unreadMessages() {
        try {
            return MessageDao.unreadForAdmin();
        } catch (SQLException e) {
            return 0;
        }
    }

    private static int unreadAdmin() {
        try {
            return NotificationDao.unreadForAdmins();
        } catch (SQLException e) {
            return 0;
        }
    }

    private static List<Object[]> allUsers() throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        for (User u : UserDao.findAll()) {
            rows.add(new Object[]{u.getId(), u.getName(), u.getEmail(), u.getPhone(), u.getBloodGroup(), u.getDistrict(), u.getRole()});
        }
        return rows;
    }

    // ---- button actions ----

    private void changeRequest(DataPage page, String status) {
        int id = page.selectedId();
        if (id < 0) {
            Ui.error(this, "Select a request first.");
            return;
        }
        try {
            String problem = RequestDao.setStatus(id, status);
            if (problem != null) Ui.error(this, problem);
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }

    private void changeDonation(DataPage page, String status) {
        int id = page.selectedId();
        if (id < 0) {
            Ui.error(this, "Select a donation offer first.");
            return;
        }
        try {
            String problem = DonationDao.setStatus(id, status);
            if (problem != null) Ui.error(this, problem);
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }

    private void deleteBank(DataPage page) {
        int id = page.selectedId();
        if (id < 0) {
            Ui.error(this, "Select a blood bank first.");
            return;
        }
        try {
            BankDao.delete(id);
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }
}
