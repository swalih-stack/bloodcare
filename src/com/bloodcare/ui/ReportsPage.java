package com.bloodcare.ui;

import com.bloodcare.db.DonationDao;
import com.bloodcare.db.InventoryDao;
import com.bloodcare.db.RequestDao;
import com.bloodcare.model.BloodGroups;
import com.bloodcare.model.BloodInventory;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reports & Analytics: a stock chart (one vertical progress bar per blood group, lighter red = low stock),
 * requests/donations by status, and stock versus open demand.
 */
public class ReportsPage extends BasePage {
    private final JProgressBar[] bars = new JProgressBar[BloodGroups.ALL.length];
    private final JLabel[] counts = new JLabel[BloodGroups.ALL.length];
    private final JTable requestStatus = Ui.table("Status", "Requests", "Units");
    private final JTable donationStatus = Ui.table("Status", "Donations", "Units");
    private final JTable demand = Ui.table("Group", "In stock", "Units needed (open requests)", "Shortfall");

    public ReportsPage() {
        super(new BorderLayout(0, 14));
        add(Ui.buttonRow(Ui.button("Export Report (CSV)", e -> AdminActions.exportReport(this))), BorderLayout.NORTH);

        JPanel chart = new JPanel(new GridLayout(1, bars.length, 12, 0));
        chart.setOpaque(false);
        for (int i = 0; i < bars.length; i++) {
            bars[i] = new JProgressBar(SwingConstants.VERTICAL, 0, 1);
            counts[i] = new JLabel("0", SwingConstants.CENTER);
            counts[i].setFont(Ui.BOLD);
            JLabel group = new JLabel(BloodGroups.ALL[i], SwingConstants.CENTER);
            group.setFont(Ui.BOLD);
            JPanel column = new JPanel(new BorderLayout(0, 4));
            column.setOpaque(false);
            column.add(counts[i], BorderLayout.NORTH);
            column.add(bars[i], BorderLayout.CENTER);
            column.add(group, BorderLayout.SOUTH);
            chart.add(column);
        }

        JPanel grid = new JPanel(new GridLayout(2, 2, 16, 16));
        grid.setOpaque(false);
        grid.add(Ui.card(Ui.section("Blood stock by group", chart)));
        grid.add(Ui.card(Ui.section("Requests by status", Ui.scroll(requestStatus))));
        grid.add(Ui.card(Ui.section("Stock vs. open demand", Ui.scroll(demand))));
        grid.add(Ui.card(Ui.section("Donations by status", Ui.scroll(donationStatus))));
        add(grid, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        try {
            BloodInventory stock = InventoryDao.load();
            showChart(stock);
            Ui.setRows(requestStatus, RequestDao.byStatus());
            Ui.setRows(donationStatus, DonationDao.byStatus());
            Map<String, Integer> open = RequestDao.openDemandByGroup();
            List<Object[]> rows = new ArrayList<>();
            for (String g : BloodGroups.ALL) {
                int have = stock.getAvailableUnits(g);
                int need = open.getOrDefault(g, 0);
                rows.add(new Object[]{g, have, need, Math.max(0, need - have)});
            }
            Ui.setRows(demand, rows);
        } catch (SQLException e) {
            Ui.dbError(this, e);
        }
    }

    private void showChart(BloodInventory stock) {
        int max = 1;
        for (String g : BloodGroups.ALL) max = Math.max(max, stock.getAvailableUnits(g));
        for (int i = 0; i < bars.length; i++) {
            String group = BloodGroups.ALL[i];
            bars[i].setMaximum(max);
            bars[i].setValue(stock.getAvailableUnits(group));
            bars[i].setForeground(stock.isLow(group) ? new Color(0xEF9A9A) : new Color(0xC62828));
            counts[i].setText(String.valueOf(stock.getAvailableUnits(group)));
        }
    }
}
