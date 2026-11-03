package com.bloodcare.ui;

import com.bloodcare.db.DonationDao;
import com.bloodcare.db.ResponseDao;
import com.bloodcare.model.Donation;
import com.bloodcare.model.DonorEligibilityChecker;
import com.bloodcare.model.Donor;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * Combined user page for offering blood and responding to compatible blood requests.
 * The donation form/history stay above the help-request table so both actions are available
 * without switching sidebar pages.
 */
public class DonateHelpPage extends BasePage {
    private final Donor donor;
    private final Runnable afterChange;
    private final Runnable openChat;
    private final JComboBox<String> group = Ui.combo(Ui.GROUPS);
    private final JSpinner units = new JSpinner(new SpinnerNumberModel(1, 1, Donation.MAX_UNITS, 1));
    private final JSpinner age = new JSpinner(new SpinnerNumberModel(25, 10, 99, 1));
    private final JSpinner weight = new JSpinner(new SpinnerNumberModel(60, 30, 200, 1));
    private final JTextField date = Ui.field();
    private final JTable donations = Ui.table("ID", "Group", "Units", "Date", "Status");
    private final JTable help = Ui.table("ID", "Patient", "Group", "Units", "Hospital", "District",
            "Urgency", "Status", "Requested");

    public DonateHelpPage(Donor donor, Runnable afterChange, Runnable openChat) {
        super(new BorderLayout(0, 16));
        this.donor = donor;
        this.afterChange = afterChange;
        this.openChat = openChat;
        date.setText(LocalDate.now().plusDays(1).toString());

        Ui.Form form = new Ui.Form();
        form.row("Blood group", group);
        form.row("Units (1-" + Donation.MAX_UNITS + ")", units);
        form.row("Your age", age);
        form.row("Weight (kg)", weight);
        form.row("Preferred date (yyyy-mm-dd)", date);
        JLabel rule = Ui.wrapped("Eligibility: age 18-65, weight at least 50 kg, and 90 days since your last approved donation.", 330);
        rule.setForeground(Ui.MUTED);
        JPanel offer = new JPanel(new BorderLayout(0, 8));
        offer.setOpaque(false);
        offer.add(form, BorderLayout.NORTH);
        offer.add(Ui.buttonRow(Ui.button("Offer to Donate", e -> submit())), BorderLayout.CENTER);
        offer.add(rule, BorderLayout.SOUTH);

        JPanel top = new JPanel(new GridLayout(1, 2, 16, 0));
        top.setOpaque(false);
        top.add(Ui.card(Ui.section("Offer blood", offer)));
        top.add(Ui.card(Ui.section("My donations", Ui.scroll(donations))));

        JPanel helpBody = new JPanel(new BorderLayout(0, 10));
        helpBody.setOpaque(false);
        helpBody.add(Ui.section("Compatible help requests", Ui.scroll(help)), BorderLayout.CENTER);
        helpBody.add(Ui.buttonRow(
                Ui.button("Accept & Help", e -> respond(true)),
                Ui.button("Decline", e -> respond(false))), BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(Ui.card(helpBody), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        group.setSelectedItem(donor.getBloodGroup());
        try {
            Ui.setRows(donations, DonationDao.forUser(donor.getId()));
            Ui.setRows(help, ResponseDao.openForDonor(donor.getId()));
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }

    private void submit() {
        try {
            String waitProblem = eligibilityWaitMessage();
            if (waitProblem != null) {
                Ui.error(this, waitProblem);
                return;
            }
        } catch (SQLException e) {
            Ui.dbError(this, e);
            return;
        }
        LocalDate when;
        try {
            when = LocalDate.parse(date.getText().trim());
        } catch (DateTimeParseException e) {
            when = null;
        }
        try {
            Donation offer = Donation.offer(donor.getId(), (String) group.getSelectedItem(), (Integer) units.getValue(),
                    when, (Integer) age.getValue(), (Integer) weight.getValue());
            DonationDao.add(offer);
            refresh();
            afterChange.run();
            Ui.info(this, "Thank you! Your donation offer is waiting for admin verification.");
        } catch (IllegalArgumentException e) {
            Ui.error(this, e.getMessage());
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }

    private void respond(boolean accept) {
        int id = Ui.selectedId(help);
        if (id < 0) {
            Ui.error(this, "Select a help request first.");
            return;
        }
        try {
            String waitProblem = eligibilityWaitMessage();
            if (waitProblem != null) {
                Ui.error(this, waitProblem);
                return;
            }
            String problem = ResponseDao.respond(id, donor.getId(), accept);
            if (problem != null) {
                Ui.error(this, problem);
            } else if (accept) {
                refresh();
                afterChange.run();
                Ui.info(this, "Thank you! The requester has been told. You can chat in Conversations.");
                openChat.run();
            } else {
                refresh();
                afterChange.run();
            }
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }

    /** Checks the current 90-day waiting period immediately before either donor action. */
    private String eligibilityWaitMessage() throws SQLException {
        LocalDate last = DonationDao.lastApprovedDate(donor.getId());
        if (last == null) return null;
        LocalDate next = DonorEligibilityChecker.DEFAULT.nextEligibleDate(last);
        if (!LocalDate.now().isBefore(next)) return null;
        long days = ChronoUnit.DAYS.between(LocalDate.now(), next);
        return "You are not eligible yet. Please wait " + days + (days == 1 ? " day" : " days")
                + " until " + next + " before donating or accepting a help request.";
    }
}
