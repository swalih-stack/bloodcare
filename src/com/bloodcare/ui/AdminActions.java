package com.bloodcare.ui;

import com.bloodcare.db.BankDao;
import com.bloodcare.db.DonationDao;
import com.bloodcare.db.InAppNotificationService;
import com.bloodcare.db.InventoryDao;
import com.bloodcare.db.RequestDao;
import com.bloodcare.model.BloodBank;
import com.bloodcare.model.NotificationService;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AdminActions - dialogs used from several admin pages (Add/Remove Blood Units, Send Notification,
 * Add Blood Bank, Export Report). Each dialog method returns true if something was saved,
 * so the caller knows to refresh its screen. Notifications go through the NotificationService interface.
 */
public final class AdminActions {
    private static final NotificationService NOTIFIER = new InAppNotificationService();

    private AdminActions() {}

    /** Adds units of blood to a selected blood bank. */
    public static boolean addUnits(Component parent) {
        try {
            JComboBox<BloodBank> bank = new JComboBox<>(BankDao.items().toArray(new BloodBank[0]));
            bank.setFont(Ui.BODY);
            JComboBox<String> group = Ui.combo(Ui.GROUPS);
            JSpinner units = new JSpinner(new SpinnerNumberModel(1, 1, 500, 1));
            Ui.Form f = new Ui.Form();
            f.row("Blood bank", bank);
            f.row("Blood group", group);
            f.row("Units", units);
            if (!confirm(parent, f, "Add Blood Units")) return false;
            BloodBank chosen = (BloodBank) bank.getSelectedItem();
            if (chosen == null || chosen.isNone()) {
                Ui.error(parent, "Choose a blood bank first.");
                return false;
            }
            InventoryDao.add(chosen.getId(), (String) group.getSelectedItem(), (Integer) units.getValue(), "Added by admin");
            return true;
        } catch (SQLException e) {
            Ui.dbError(parent, e);
            return false;
        }
    }

    /** Removes units from the selected blood bank, never allowing its stock to become negative. */
    public static boolean removeUnits(Component parent) {
        try {
            JComboBox<BloodBank> bank = new JComboBox<>(BankDao.items().toArray(new BloodBank[0]));
            bank.setFont(Ui.BODY);
            JComboBox<String> group = Ui.combo(Ui.GROUPS);
            JComboBox<RequestChoice> request = new JComboBox<>();
            request.setFont(Ui.BODY);
            Runnable loadRequests = () -> {
                request.removeAllItems();
                try {
                    for (Object[] row : RequestDao.openForFulfillment((String) group.getSelectedItem())) {
                        request.addItem(new RequestChoice((Integer) row[0], (String) row[1], (String) row[2],
                                (Integer) row[3], (String) row[4]));
                    }
                } catch (SQLException e) {
                    Ui.dbError(parent, e);
                }
            };
            group.addActionListener(e -> loadRequests.run());
            Ui.Form f = new Ui.Form();
            f.row("Blood bank", bank);
            f.row("Blood group", group);
            f.row("Patient request", request);
            loadRequests.run();
            if (!confirm(parent, f, "Remove Blood Units")) return false;
            BloodBank chosen = (BloodBank) bank.getSelectedItem();
            if (chosen == null || chosen.isNone()) {
                Ui.error(parent, "Choose a blood bank first.");
                return false;
            }
            RequestChoice selected = (RequestChoice) request.getSelectedItem();
            if (selected == null) {
                Ui.error(parent, "Choose an approved patient request first.");
                return false;
            }
            String problem = RequestDao.fulfillFromBank(selected.id, chosen.getId());
            if (problem != null) {
                Ui.error(parent, problem);
                return false;
            }
            return true;
        } catch (SQLException e) {
            Ui.dbError(parent, e);
            return false;
        }
    }

    /** A compact display object for the approved-request dropdown. */
    private static final class RequestChoice {
        private final int id;
        private final String patient;
        private final String group;
        private final int units;
        private final String hospital;

        private RequestChoice(int id, String patient, String group, int units, String hospital) {
            this.id = id;
            this.patient = patient;
            this.group = group;
            this.units = units;
            this.hospital = hospital;
        }

        @Override
        public String toString() {
            return "#" + id + " · " + patient + " · " + units + " unit(s) · " + hospital;
        }
    }

    /** Sends a message to all users, or only to users of one blood group. */
    public static boolean sendNotification(Component parent) {
        String[] audiences = new String[Ui.GROUPS.length + 1];
        audiences[0] = "All users";
        for (int i = 0; i < Ui.GROUPS.length; i++) audiences[i + 1] = "Blood group " + Ui.GROUPS[i];
        JComboBox<String> audience = Ui.combo(audiences);
        JTextArea message = messageBox();
        Ui.Form f = new Ui.Form();
        f.row("Send to", audience);
        f.row("Message", new JScrollPane(message));
        if (!confirm(parent, f, "Send Notification")) return false;
        String text = message.getText().trim();
        if (text.isEmpty() || text.length() > 280) {
            Ui.error(parent, "Message must be between 1 and 280 characters.");
            return false;
        }
        try {
            int index = audience.getSelectedIndex();
            int sent = NOTIFIER.broadcast(text, index == 0 ? null : Ui.GROUPS[index - 1]);
            Ui.info(parent, "Sent to " + sent + " user(s).");
            return true;
        } catch (SQLException e) {
            Ui.dbError(parent, e);
            return false;
        }
    }

    public static boolean addBank(Component parent) {
        JTextField name = Ui.field();
        JComboBox<String> district = Ui.districtCombo();
        JTextField phone = Ui.field();
        JTextField address = Ui.field();
        Ui.Form f = new Ui.Form();
        f.row("Bank name", name);
        f.row("District", district);
        f.row("Phone", phone);
        f.row("Address", address);
        if (!confirm(parent, f, "Add Blood Bank")) return false;
        try {
            BankDao.add(BloodBank.create(name.getText(), Ui.districtValue(district), address.getText(), phone.getText().trim()));
            return true;
        } catch (IllegalArgumentException e) {
            Ui.error(parent, e.getMessage());
            return false;
        } catch (SQLException e) {
            Ui.dbError(parent, e);
            return false;
        }
    }

    /** Saves stock, requests and donations into one CSV file the admin can open in Excel. */
    public static void exportReport(Component parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("bloodcare-report.csv"));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        try {
            StringBuilder sb = new StringBuilder("BloodCare report generated " + LocalDateTime.now().withNano(0) + "\n\n");
            List<Object[]> stock = new ArrayList<>();
            for (Map.Entry<String, Integer> e : InventoryDao.load().asMap().entrySet()) {
                stock.add(new Object[]{e.getKey(), e.getValue()});
            }
            section(sb, "Blood stock", new String[]{"Group", "Units"}, stock);
            section(sb, "Blood requests", new String[]{"ID", "Requested by", "Patient", "Group", "Units", "Hospital",
                    "District", "Urgency", "Status", "Requested"}, RequestDao.all());
            section(sb, "Donations", new String[]{"ID", "Donor", "Group", "Units", "Date", "Status"}, DonationDao.all());
            Files.writeString(chooser.getSelectedFile().toPath(), sb.toString(), StandardCharsets.UTF_8);
            Ui.info(parent, "Report saved to " + chooser.getSelectedFile().getAbsolutePath());
        } catch (IOException | SQLException e) {
            Ui.error(parent, "Could not create report: " + e.getMessage());
        }
    }

    private static boolean confirm(Component parent, JComponent content, String title) {
        return JOptionPane.showConfirmDialog(parent, content, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }

    private static JTextArea messageBox() {
        JTextArea area = new JTextArea(4, 28);
        area.setFont(Ui.BODY);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }

    private static void section(StringBuilder sb, String title, String[] headers, List<Object[]> rows) {
        sb.append(title).append('\n');
        sb.append(String.join(",", headers)).append('\n');
        for (Object[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                if (i > 0) sb.append(',');
                sb.append('"').append(String.valueOf(row[i]).replace("\"", "\"\"")).append('"');
            }
            sb.append('\n');
        }
        sb.append('\n');
    }
}
