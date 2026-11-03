package com.bloodcare.ui;

import com.bloodcare.db.DonationDao;
import com.bloodcare.model.Donation;
import com.bloodcare.model.Donor;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Donate Blood: the form that creates a Donation offer (validated by Donation.offer) and the donor's own offers. */
public class DonatePage extends BasePage {
    private final Donor donor;
    private final JComboBox<String> group = Ui.combo(Ui.GROUPS);
    private final JSpinner units = new JSpinner(new SpinnerNumberModel(1, 1, Donation.MAX_UNITS, 1));
    private final JSpinner age = new JSpinner(new SpinnerNumberModel(25, 10, 99, 1));
    private final JSpinner weight = new JSpinner(new SpinnerNumberModel(60, 30, 200, 1));
    private final JTextField date = Ui.field();
    private final JTable table = Ui.table("ID", "Group", "Units", "Date", "Status");

    public DonatePage(Donor donor) {
        super(new BorderLayout());
        this.donor = donor;
        date.setText(LocalDate.now().plusDays(1).toString());

        Ui.Form form = new Ui.Form();
        form.row("Blood group", group);
        form.row("Units (1-" + Donation.MAX_UNITS + ")", units);
        form.row("Your age", age);
        form.row("Weight (kg)", weight);
        form.row("Preferred date (yyyy-mm-dd)", date);
        JLabel rule = Ui.wrapped("Eligibility: age 18-65, weight at least 50 kg, and at least 90 days since "
                + "your last approved donation. The admin verifies this before approving.", 330);
        rule.setForeground(Ui.MUTED);
        JPanel formBody = new JPanel(new BorderLayout(0, 10));
        formBody.setOpaque(false);
        formBody.add(form, BorderLayout.NORTH);
        formBody.add(Ui.buttonRow(Ui.button("Offer to Donate", e -> submit())), BorderLayout.CENTER);
        formBody.add(rule, BorderLayout.SOUTH);

        splitLayout(formBody, 450, Ui.section("My donations", Ui.scroll(table)));
    }

    @Override
    public void refresh() {
        group.setSelectedItem(donor.getBloodGroup());
        try {
            Ui.setRows(table, DonationDao.forUser(donor.getId()));
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }

    private void submit() {
        LocalDate when;
        try {
            when = LocalDate.parse(date.getText().trim());
        } catch (DateTimeParseException e) {
            when = null; // Donation.offer reports the problem
        }
        try {
            Donation offer = Donation.offer(donor.getId(), (String) group.getSelectedItem(), (Integer) units.getValue(),
                    when, (Integer) age.getValue(), (Integer) weight.getValue());
            DonationDao.add(offer);
            refresh();
            Ui.info(this, "Thank you! Your donation offer is waiting for admin verification.");
        } catch (IllegalArgumentException e) {
            Ui.error(this, e.getMessage());
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }
}
