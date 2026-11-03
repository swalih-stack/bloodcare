package com.bloodcare.ui;

import com.bloodcare.db.InventoryDao;
import com.bloodcare.model.BloodGroups;
import com.bloodcare.model.BloodInventory;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

/** Eight tiles (one per blood group) showing units in stock. Red = low stock, green = fine. */
public class StockTiles extends JPanel {
    public StockTiles() {
        super(new GridLayout(2, 4, 12, 12));
        setOpaque(false);
        reload();
    }

    /** Re-reads the stock from MySQL. */
    public void reload() {
        try {
            show(InventoryDao.load());
        } catch (SQLException e) {
            removeAll();
            add(new JLabel("Could not load stock: " + e.getMessage()));
            revalidate();
            repaint();
        }
    }

    /** Draws the tiles for stock that has already been loaded. */
    public void show(BloodInventory inventory) {
        removeAll();
        for (String group : BloodGroups.ALL) add(tile(group, inventory));
        revalidate();
        repaint();
    }

    private JComponent tile(String group, BloodInventory inventory) {
        boolean low = inventory.isLow(group);
        RoundPanel p = new RoundPanel(new BorderLayout(12, 0), 14,
                low ? new Color(0xFDECEA) : new Color(0xE8F5E9), null);
        p.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        JLabel icon = new JLabel(Icons.dropIcon(24, new Color(0xC62828)));
        icon.setVerticalAlignment(SwingConstants.TOP);
        JPanel text = new JPanel(new GridLayout(2, 1));
        text.setOpaque(false);
        JLabel name = new JLabel(group);
        name.setFont(new Font("Segoe UI", Font.BOLD, 18));
        JLabel count = new JLabel(inventory.getAvailableUnits(group) + " units");
        count.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        count.setForeground(low ? new Color(0xB71C1C) : new Color(0x2E7D32));
        text.add(name);
        text.add(count);
        p.add(icon, BorderLayout.WEST);
        p.add(text, BorderLayout.CENTER);
        return p;
    }
}
