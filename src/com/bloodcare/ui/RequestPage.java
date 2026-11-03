package com.bloodcare.ui;

import com.bloodcare.db.BankDao;
import com.bloodcare.db.RequestDao;
import com.bloodcare.db.UserDao;
import com.bloodcare.model.BloodRequest;
import com.bloodcare.model.Donor;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

/**
 * Request Blood: the form that creates a BloodRequest (through BloodSeeker.createRequest, which validates it)
 * and the user's own requests. Selecting one of those requests also shows other users with the
 * requested blood group; emergency requests additionally show banks that currently hold that group.
 */
public class RequestPage extends BasePage {
    private final Donor donor;
    private final JTextField patient = Ui.field();
    private final JComboBox<String> group = Ui.combo(Ui.GROUPS);
    private final JSpinner units = new JSpinner(new SpinnerNumberModel(1, 1, BloodRequest.MAX_UNITS, 1));
    private final JTextField hospital = Ui.field();
    private final JComboBox<String> district = Ui.districtCombo();
    private final JComboBox<String> urgency = Ui.combo(BloodRequest.URGENCY_LEVELS);
    private final JTable table = Ui.table("ID", "Patient", "Group", "Units", "Hospital", "District",
            "Urgency", "Status", "Requested");
    private final JTable allRequests = Ui.table("ID", "Patient", "Group", "Units", "Hospital", "District",
            "Urgency", "Status", "Requested");
    private final JTable matchingUsers = Ui.table("Name", "Group", "District");
    private final JTable allMatchingUsers = Ui.table("Name", "Group", "District");
    private final JTable emergencyBanks = Ui.table("Blood bank", "Group", "District", "Phone", "Address", "Available units");
    private final JTable allEmergencyBanks = Ui.table("Blood bank", "Group", "District", "Phone", "Address", "Available units");
    private final JLabel bankTitle = new JLabel("Emergency blood banks");
    private final CardLayout views = new CardLayout();
    private final JPanel viewArea = new JPanel(views);

    public RequestPage(Donor donor) {
        super(new BorderLayout());
        this.donor = donor;

        Ui.Form form = new Ui.Form();
        form.row("Patient name", patient);
        form.row("Blood group needed", group);
        form.row("Units", units);
        form.row("Hospital", hospital);
        form.row("District of need", district);
        form.row("Urgency", urgency);
        JPanel formBody = new JPanel(new BorderLayout(0, 10));
        formBody.setOpaque(false);
        formBody.add(form, BorderLayout.NORTH);
        formBody.add(Ui.buttonRow(Ui.button("Submit Request", e -> submit())), BorderLayout.CENTER);

        JPanel matchingArea = new JPanel(new GridLayout(1, 2, 14, 0));
        matchingArea.setOpaque(false);
        matchingArea.add(tableSection("Users with compatible blood groups", matchingUsers,
                () -> showExpanded("compatible-users")));
        bankTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        matchingArea.add(tableSection(bankTitle, emergencyBanks, () -> showExpanded("emergency-banks")));

        JPanel requestBody = new JPanel(new BorderLayout(0, 10));
        requestBody.setOpaque(false);
        JScrollPane requestScroll = Ui.scroll(table);
        requestScroll.setPreferredSize(new Dimension(1, 105));
        requestBody.add(tableSection("My requests", requestScroll, () -> showExpanded("my-requests")), BorderLayout.NORTH);
        requestBody.add(matchingArea, BorderLayout.CENTER);
        requestBody.add(Ui.buttonRow(Ui.button("Cancel Selected Request", e -> cancel())), BorderLayout.SOUTH);

        // Keep both sections the same width and height: submit on top, request details below.
        JPanel normalView = new JPanel(new GridLayout(2, 1, 0, 18));
        normalView.setOpaque(false);
        normalView.add(Ui.card(formBody));
        normalView.add(Ui.card(requestBody));

        viewArea.setOpaque(false);
        viewArea.add(normalView, "main");
        viewArea.add(expandedTable("All my requests", allRequests), "my-requests");
        viewArea.add(expandedTable("Users with compatible blood groups", allMatchingUsers), "compatible-users");
        viewArea.add(expandedTable("Emergency blood banks", allEmergencyBanks), "emergency-banks");
        setLayout(new BorderLayout());
        add(viewArea, BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(this::showMatchesForSelectedRequest);
    }

    @Override
    public void refresh() {
        if (district.getSelectedIndex() == 0) Ui.setDistrict(district, donor.getDistrict()); // default: my own district
        try {
            List<Object[]> rows = RequestDao.forUser(donor.getId());
            Ui.setRows(table, rows);
            Ui.setRows(allRequests, rows);
            if (table.getRowCount() > 0) table.setRowSelectionInterval(0, 0);
            else clearMatches();
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }

    private void submit() {
        try {
            BloodRequest request = donor.toSeeker().createRequest(patient.getText(), (String) group.getSelectedItem(),
                    (Integer) units.getValue(), hospital.getText(), Ui.districtValue(district),
                    (String) urgency.getSelectedItem());
            int alerted = RequestDao.add(request);
            String where = request.getLocation().getDistrict();
            patient.setText("");
            hospital.setText("");
            units.setValue(1);
            refresh();
            Ui.info(this, "Request submitted. An admin will review it.\n" + (alerted == 0
                    ? "No matching donors were found in " + where + " yet."
                    : alerted + " matching donor(s) in " + where + " were alerted right away."));
        } catch (IllegalArgumentException e) {
            Ui.error(this, e.getMessage());
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }

    private void cancel() {
        int id = Ui.selectedId(table);
        if (id < 0) {
            Ui.error(this, "Select a request first.");
            return;
        }
        try {
            if (RequestDao.cancelOwn(id, donor.getId())) refresh();
            else Ui.error(this, "Only your own PENDING requests can be cancelled.");
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }

    /** Reloads the helper lists for the selected request without exposing a user's own row. */
    private void showMatchesForSelectedRequest(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) return;
        int row = table.getSelectedRow();
        if (row < 0) {
            clearMatches();
            return;
        }
        String bloodGroup = String.valueOf(table.getValueAt(row, 2));
        String requestUrgency = String.valueOf(table.getValueAt(row, 6));
        try {
            String requestDistrict = String.valueOf(table.getValueAt(row, 5));
            List<Object[]> users = UserDao.compatibleDonors(bloodGroup, requestDistrict, donor.getId());
            Ui.setRows(matchingUsers, users);
            Ui.setRows(allMatchingUsers, users);
            if (BloodRequest.EMERGENCY.equals(requestUrgency)) {
                bankTitle.setText("Emergency blood banks");
                List<Object[]> banks = BankDao.withCompatibleBlood(bloodGroup);
                Ui.setRows(emergencyBanks, banks);
                Ui.setRows(allEmergencyBanks, banks);
            } else {
                bankTitle.setText("Emergency blood banks");
                Ui.setRows(emergencyBanks, List.of());
                Ui.setRows(allEmergencyBanks, List.of());
            }
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }

    private void clearMatches() {
        Ui.setRows(matchingUsers, List.of());
        Ui.setRows(allMatchingUsers, List.of());
        Ui.setRows(emergencyBanks, List.of());
        Ui.setRows(allEmergencyBanks, List.of());
        bankTitle.setText("Emergency blood banks");
    }

    /** A compact table section with a link that opens the same data at full page size. */
    private JPanel tableSection(String title, JComponent tableBody, Runnable viewAll) {
        return tableSection(new JLabel(title), tableBody, viewAll);
    }

    private JPanel tableSection(JLabel title, JComponent tableBody, Runnable viewAll) {
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        heading.add(title, BorderLayout.WEST);
        heading.add(Ui.link("View all →", Ui.RED, e -> viewAll.run()), BorderLayout.EAST);

        JPanel section = new JPanel(new BorderLayout(0, 10));
        section.setOpaque(false);
        section.add(heading, BorderLayout.NORTH);
        section.add(tableBody instanceof JTable ? Ui.scroll((JTable) tableBody) : tableBody, BorderLayout.CENTER);
        return section;
    }

    private JPanel expandedTable(String title, JTable table) {
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);
        content.add(Ui.buttonRow(Ui.link("← Back to Request Blood", Ui.RED,
                e -> views.show(viewArea, "main"))), BorderLayout.NORTH);
        content.add(Ui.card(Ui.section(title, Ui.scroll(table))), BorderLayout.CENTER);
        return content;
    }

    private void showExpanded(String key) {
        views.show(viewArea, key);
    }
}
