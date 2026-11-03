package com.bloodcare.ui;

import com.bloodcare.db.DonationDao;
import com.bloodcare.db.InventoryDao;
import com.bloodcare.db.RequestDao;
import com.bloodcare.db.UserDao;
import com.bloodcare.model.BloodInventory;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.function.Consumer;

/**
 * AdminHome - the dashboard page: four stat cards, blood stock tiles, latest requests,
 * latest donations and quick actions. Every number comes from MySQL when the page opens.
 */
public class AdminHome extends BasePage {
    private final Runnable afterChange;

    private final StatCard usersCard = new StatCard(Icons.DROP, "Total Users", 30);
    private final StatCard donorsCard = new StatCard(Icons.PEOPLE, "Registered Donors", 30);
    private final StatCard requestsCard = new StatCard(Icons.CLIPBOARD, "Active Requests", 30);
    private final StatCard unitsCard = new StatCard(Icons.DROP, "Blood Units Available", 30);
    private final StockTiles tiles = new StockTiles();
    private final JPanel requestList = listPanel();
    private final JPanel donationList = listPanel();

    public AdminHome(Consumer<String> navigate, Runnable afterChange) {
        super(new GridBagLayout());
        this.afterChange = afterChange;

        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;

        JPanel stats = new JPanel(new GridLayout(1, 4, 16, 0));
        stats.setOpaque(false);
        for (StatCard c : List.of(usersCard, donorsCard, requestsCard, unitsCard)) stats.add(c);
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2; g.weightx = 1; g.weighty = 0;
        g.insets = new Insets(0, 0, 16, 0);
        add(stats, g);

        g.gridwidth = 1; g.gridy = 1; g.weighty = 0.5;
        g.gridx = 0; g.weightx = 0.62; g.insets = new Insets(0, 0, 16, 16);
        add(Ui.titledCard("Blood Inventory by Group", "Manage Inventory", () -> navigate.accept("inventory"), tiles), g);
        g.gridx = 1; g.weightx = 0.38; g.insets = new Insets(0, 0, 16, 0);
        add(Ui.titledCard("Recent Requests", "View All", () -> navigate.accept("requests"), requestList), g);

        g.gridy = 2;
        g.gridx = 0; g.weightx = 0.62; g.insets = new Insets(0, 0, 0, 16);
        add(Ui.titledCard("Recent Donations", "View All", () -> navigate.accept("donations"), donationList), g);
        g.gridx = 1; g.weightx = 0.38; g.insets = new Insets(0, 0, 0, 0);
        add(Ui.titledCard("Quick Actions", null, null, quickActions(navigate)), g);
    }

    private static JPanel listPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        return p;
    }

    private JComponent quickActions(Consumer<String> navigate) {
        JPanel grid = new JPanel(new GridLayout(2, 2, 12, 12));
        grid.setOpaque(false);
        grid.add(Ui.actionTile("✔", "Verify Donor<br>Eligibility", new Color(0xFDE3E1), new Color(0xD32F2F),
                () -> navigate.accept("eligibility")));
        grid.add(Ui.actionTile("+", "Add Blood<br>Units", new Color(0xE3F0FD), new Color(0x1565C0), () -> {
            if (AdminActions.addUnits(this)) { refresh(); afterChange.run(); }
        }));
        grid.add(Ui.actionTile("✉", "Send<br>Notification", new Color(0xF0E6FA), new Color(0x7B3FC4), () -> {
            if (AdminActions.sendNotification(this)) afterChange.run();
        }));
        grid.add(Ui.actionTile("▥", "Generate<br>Report", new Color(0xE6F4EA), new Color(0x2E7D32),
                () -> AdminActions.exportReport(this)));
        return grid;
    }

    @Override
    public void refresh() {
        try {
            BloodInventory stock = InventoryDao.load();
            int approved = DonationDao.countByStatus("APPROVED");
            int pending = DonationDao.countByStatus("PENDING");
            usersCard.set(String.valueOf(UserDao.countUsers()), "↑ +" + UserDao.newUsersThisWeek() + " this week");
            donorsCard.set(String.valueOf(UserDao.countDonors()),
                    approved + (approved == 1 ? " donation" : " donations") + " approved");
            requestsCard.set(String.valueOf(RequestDao.countActive()), RequestDao.countActiveEmergency() + " emergency");
            unitsCard.set(String.valueOf(stock.getTotalUnits()), pending + (pending == 1 ? " offer" : " offers") + " pending");
            tiles.show(stock);
            fillRequests(RequestDao.recent(3));
            fillDonations(DonationDao.recent(3));
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }

    private void fillRequests(List<Object[]> rows) {
        requestList.removeAll();
        if (rows.isEmpty()) requestList.add(empty("No requests yet."));
        for (Object[] r : rows) {
            JPanel row = new JPanel(new BorderLayout(12, 0));
            row.setOpaque(false);
            row.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

            // the blood group in a pink circle: a RoundPanel whose arc equals its size
            RoundPanel circle = new RoundPanel(new GridBagLayout(), 46, new Color(0xFDECEA), null);
            circle.setPreferredSize(new Dimension(46, 46));
            JLabel group = new JLabel((String) r[0]);
            group.setFont(new Font("Segoe UI", Font.BOLD, 16));
            group.setForeground(new Color(0xC62828));
            circle.add(group);

            JPanel mid = new JPanel(new GridLayout(2, 1, 0, 4));
            mid.setOpaque(false);
            mid.add(leftAligned(Ui.statusChip((String) r[1])));
            JLabel name = new JLabel((String) r[2]);
            name.setFont(Ui.BOLD);
            mid.add(name);

            JPanel end = new JPanel(new GridLayout(2, 1, 0, 4));
            end.setOpaque(false);
            JLabel place = new JLabel((String) r[3], SwingConstants.RIGHT);
            place.setFont(Ui.BODY);
            JLabel ago = new JLabel(Ui.ago((Timestamp) r[4]), SwingConstants.RIGHT);
            ago.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            ago.setForeground(Ui.MUTED);
            end.add(place);
            end.add(ago);

            row.add(circle, BorderLayout.WEST);
            row.add(mid, BorderLayout.CENTER);
            row.add(end, BorderLayout.EAST);
            requestList.add(row);
        }
        requestList.revalidate();
        requestList.repaint();
    }

    private void fillDonations(List<Object[]> rows) {
        donationList.removeAll();
        JPanel head = new JPanel(new GridLayout(1, 4));
        head.setBackground(new Color(0xF5F1F0));
        head.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        for (String h : new String[]{"Donor", "Blood Group", "Date", "Status"}) {
            JLabel l = new JLabel("  " + h);
            l.setFont(new Font("Segoe UI", Font.BOLD, 13));
            l.setForeground(new Color(0x555555));
            head.add(l);
        }
        donationList.add(head);
        if (rows.isEmpty()) donationList.add(empty("No donations yet."));
        for (Object[] r : rows) {
            JPanel row = new JPanel(new GridLayout(1, 4));
            row.setOpaque(false);
            row.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            JLabel donor = new JLabel("  " + r[0]);
            donor.setFont(Ui.BOLD);
            row.add(donor);
            row.add(leftAligned(Ui.chip((String) r[1], new Color(0xFDECEA), new Color(0xC62828))));
            JLabel date = new JLabel((String) r[2]);
            date.setFont(Ui.BODY);
            row.add(date);
            row.add(leftAligned(Ui.statusChip((String) r[3])));
            donationList.add(row);
        }
        donationList.revalidate();
        donationList.repaint();
    }

    private static JPanel leftAligned(JComponent c) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.setOpaque(false);
        p.add(c);
        return p;
    }

    private static JLabel empty(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Ui.BODY);
        l.setForeground(Ui.MUTED);
        l.setBorder(BorderFactory.createEmptyBorder(14, 4, 14, 4));
        return l;
    }
}
