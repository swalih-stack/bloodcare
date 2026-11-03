package com.bloodcare.ui;

import com.bloodcare.db.ChatDao;
import com.bloodcare.db.NotificationDao;
import com.bloodcare.model.Donor;

import java.sql.SQLException;
import java.util.List;

import static com.bloodcare.ui.Sidebar.Entry;

/**
 * UserDashboard - the signed-in donor's dashboard. It only wires things together: the sidebar entries
 * and one page object per menu item (each page class holds its own form and logic).
 */
public class UserDashboard extends DashboardShell {

    public UserDashboard(MainFrame frame, Donor donor) {
        super(frame, "Blood Donation System", donor.getName(), donor.getDisplayRole(), entries(donor.getId()),
                () -> unread(donor.getId()), "notifications");

        addPage("dashboard", "My Dashboard", "Your donations and eligibility at a glance", new UserHome(donor, this::show));
        addPage("profile", "My Profile", "Your details and password", new ProfilePage(donor));
        addPage("request", "Request Blood", "Ask for blood for a patient and track your requests", new RequestPage(donor));
        addPage("donate", "Donate & Help", "Offer blood and respond to compatible help requests",
                new DonateHelpPage(donor, this::refreshBadges, () -> show("chat")));

        addPage("chat", "Conversations", "Chat with the requester or donor, no phone or email needed",
                new ChatPage(donor, this::refreshBadges));
        addPage("contact", "Contact Admin", "Send a message to the BloodCare admin", new ContactPage(donor));

        DataPage notes = new DataPage(() -> NotificationDao.forUser(donor.getId()), this::refreshBadges,
                "ID", "Message", "Received", "Status");
        notes.addAction("Mark All Read", () -> {
            try {
                NotificationDao.markAllReadForUser(donor.getId());
            } catch (SQLException e) {
                Ui.dbError(notes, e);
            }
        });
        addPage("notifications", "Notifications", "Updates about your requests and donations", notes);

        show("dashboard");
    }

    private static List<Entry> entries(int userId) {
        return List.of(
                Entry.of("dashboard", "Dashboard", "▣"),
                Entry.of("profile", "My Profile", "☺"),
                Entry.of("request", "Request Blood", "✚"),
                Entry.of("donate", "Donate & Help", "♥"),
                Entry.of("chat", "Conversations", "☎").withBadge(() -> unreadChat(userId)),
                Entry.of("contact", "Contact Admin", "✎"),
                Entry.of("notifications", "Notifications", "✉").withBadge(() -> unread(userId)));
    }

    private static int unread(int userId) {
        try {
            return NotificationDao.unreadForUser(userId);
        } catch (SQLException e) {
            return 0;
        }
    }

    private static int unreadChat(int userId) {
        try {
            return ChatDao.unreadFor(userId);
        } catch (SQLException e) {
            return 0;
        }
    }
}
