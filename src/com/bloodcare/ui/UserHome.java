package com.bloodcare.ui;

import com.bloodcare.db.DonationDao;
import com.bloodcare.db.RequestDao;
import com.bloodcare.model.Donation;
import com.bloodcare.model.DonationHistory;
import com.bloodcare.model.Donor;
import com.bloodcare.model.DonorEligibilityChecker;
import com.bloodcare.model.EligibilityChecker;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;


public class UserHome extends BasePage {
    private static final DateTimeFormatter SHOW = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final Color GREEN = new Color(0x2E9E48);

    private final Donor donor;
    private final Consumer<String> navigate;
    private final EligibilityChecker checker = DonorEligibilityChecker.DEFAULT;

    private final StatCard donationsCard = new StatCard(Icons.DROP, "Approved Donations", 30);
    private final StatCard unitsCard = new StatCard(Icons.DROP, "Units Donated", 30);
    private final StatCard lastCard = new StatCard(Icons.DROP, "Last Donation", 22);
    private final StatCard nextCard = new StatCard(Icons.DROP, "Next Eligible Date", 22);
    private final JLabel requestsTitle = new JLabel("My Latest Requests");
    private final JTable requestTable = Ui.table("ID", "Patient", "Group", "Units", "Urgency", "Status");
    private final JTable donationTable = Ui.table("ID", "Group", "Units", "Date", "Status");

    public UserHome(Donor donor, Consumer<String> navigate) {
        super(new GridBagLayout());
        this.donor = donor;
        this.navigate = navigate;

        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;
        g.weightx = 1;

        JPanel stats = new JPanel(new GridLayout(1, 4, 16, 0));
        stats.setOpaque(false);
        for (StatCard c : List.of(donationsCard, unitsCard, lastCard, nextCard)) stats.add(c);
        g.gridy = 0; g.weighty = 0; g.insets = new Insets(0, 0, 16, 0);
        add(stats, g);

        JPanel lists = new JPanel(new GridLayout(1, 2, 16, 0));
        lists.setOpaque(false);
        lists.add(Ui.titledCard(requestsTitle, "View All", () -> navigate.accept("request"), Ui.scroll(requestTable)));
        lists.add(Ui.titledCard("My Latest Donations", "View All", () -> navigate.accept("donate"), Ui.scroll(donationTable)));
        g.gridy = 1; g.weighty = 1; g.insets = new Insets(0, 0, 0, 0);
        add(lists, g);

        JPanel actions = new JPanel(new GridLayout(1, 3, 16, 0));
        actions.setOpaque(false);
        actions.add(Ui.actionTile("✚", "Request Blood", new Color(0xFDE3E1), new Color(0xD32F2F), () -> navigate.accept("request")));
        actions.add(Ui.actionTile("♥", "Donate Blood", new Color(0xE6F4EA), new Color(0x2E7D32), () -> navigate.accept("donate")));
        actions.add(Ui.actionTile("✉", "Notifications", new Color(0xE3F0FD), new Color(0x1565C0), () -> navigate.accept("notifications")));
        g.gridy = 2; g.weighty = 0; g.insets = new Insets(16, 0, 0, 0);
        add(actions, g);
    }

    @Override
    public void refresh() {
        try {
            DonationHistory history = DonationDao.historyFor(donor.getId());
            donor.recordDonation(history.getLastApprovedDate());

            donationsCard.set(String.valueOf(history.getTotalDonations()), history.getPendingCount() + " awaiting verification");
            int total = history.getTotalDonations();
            unitsCard.set(String.valueOf(history.getTotalUnits()), "across " + total + (total == 1 ? " donation" : " donations"));
            showEligibility();

            List<Object[]> requests = RequestDao.forUser(donor.getId()); // id, patient, group, units, hospital, district, urgency, status, date
            int approvedRequests = 0;
            List<Object[]> latestRequests = new ArrayList<>();
            for (Object[] r : requests) {
                if (r[7].equals("APPROVED") || r[7].equals("FULFILLED")) approvedRequests++;
                if (latestRequests.size() < 5) latestRequests.add(new Object[]{r[0], r[1], r[2], r[3], r[6], r[7]});
            }
            requestsTitle.setText("My Latest Requests  ·  " + approvedRequests + " approved");
            Ui.setRows(requestTable, latestRequests);

            List<Object[]> latestDonations = new ArrayList<>();
            for (Donation d : history.getDonations()) {
                if (latestDonations.size() == 5) break;
                latestDonations.add(new Object[]{d.getId(), d.getBloodGroup(), d.getUnits(), d.getDate().toString(), d.getStatus()});
            }
            Ui.setRows(donationTable, latestDonations);
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }

    /** Fills the "Last Donation" and "Next Eligible Date" cards from the Donor object. */
    private void showEligibility() {
        LocalDate last = donor.getLastDonationDate();
        if (last == null) {
            lastCard.set("None yet", "No approved donation");
        } else {
            lastCard.set(last.format(SHOW), ChronoUnit.DAYS.between(last, LocalDate.now()) + " days ago");
        }
        if (donor.isEligible(checker)) {
            nextCard.set("Eligible", "You can donate now");
            nextCard.setValueColor(GREEN);
        } else {
            LocalDate next = donor.getNextEligibleDate(checker);
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), next);
            nextCard.set(next.format(SHOW), daysLeft + (daysLeft == 1 ? " day" : " days") + " to go");
            nextCard.setValueColor(null);
        }
    }
}
