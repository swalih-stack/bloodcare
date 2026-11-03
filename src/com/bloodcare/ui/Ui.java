package com.bloodcare.ui;

import com.bloodcare.model.BloodGroups;
import com.bloodcare.model.Districts;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionListener;
import java.sql.Timestamp;
import java.util.List;

/** Ui - shared colours, fonts and small helpers so every screen looks the same. */
public final class Ui {
    public static final Color RED = new Color(0xB71C1C);
    public static final Color LIGHT = new Color(0xF5F5F5);
    public static final Color BG = new Color(0xF7F3F2);
    public static final Color CARD_BORDER = new Color(0xEADFDD);
    public static final Color TEXT = new Color(0x333333);
    public static final Color MUTED = new Color(0x777777);
    public static final Font BODY = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font BOLD = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font HEADING = new Font("Segoe UI", Font.BOLD, 26);
    public static final Font SYMBOL = new Font("Segoe UI Symbol", Font.PLAIN, 18);
    public static final String[] GROUPS = BloodGroups.ALL;

    private Ui() {}

    public static JButton button(String text, ActionListener action) {
        JButton b = new JButton(text);
        b.setFont(BOLD);
        b.setBackground(RED);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(9, 20, 9, 20));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(action);
        return b;
    }

    /** Plain text-style button used for navigation links. */
    public static JButton link(String text, Color color, ActionListener action) {
        JButton b = new JButton(text);
        b.setFont(BOLD);
        b.setForeground(color);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(action);
        return b;
    }

    public static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(HEADING);
        l.setForeground(RED);
        return l;
    }

    public static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(BODY);
        l.setForeground(TEXT);
        return l;
    }

    /** A label that wraps its text at the given pixel width. */
    public static JLabel wrapped(String text, int width) {
        JLabel l = new JLabel("<html><div style='width:" + width + "px'>" + text + "</div></html>");
        l.setFont(BODY);
        l.setForeground(TEXT);
        return l;
    }

    public static JTextField field() {
        JTextField f = new JTextField(22);
        f.setFont(BODY);
        return f;
    }

    public static JPasswordField password() {
        JPasswordField f = new JPasswordField(22);
        f.setFont(BODY);
        return f;
    }

    public static JComboBox<String> combo(String... items) {
        JComboBox<String> c = new JComboBox<>(items);
        c.setFont(BODY);
        return c;
    }

    /** A white rounded card with padding around the given content. */
    public static RoundPanel card(JComponent content) {
        RoundPanel p = new RoundPanel(new BorderLayout(), 18, Color.WHITE, CARD_BORDER);
        p.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        p.add(content, BorderLayout.CENTER);
        return p;
    }

    /** A row of buttons side by side, aligned left, on a transparent background. */
    public static JPanel buttonRow(JComponent... buttons) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        for (int i = 0; i < buttons.length; i++) {
            if (i > 0) p.add(Box.createHorizontalStrut(8));
            p.add(buttons[i]);
        }
        p.add(Box.createHorizontalGlue());
        return p;
    }

    /** A bold caption above some content (used above the tables of the form pages). */
    public static JPanel section(String title, JComponent body) {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 16));
        p.add(t, BorderLayout.NORTH);
        p.add(body, BorderLayout.CENTER);
        return p;
    }

    /** A card with a big title (the label can be changed later) and an optional "View All ->" link at the top right. */
    public static RoundPanel titledCard(JLabel title, String linkText, Runnable onLink, JComponent body) {
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        head.add(title, BorderLayout.WEST);
        if (linkText != null) {
            JButton link = link(linkText + "  →", RED, e -> onLink.run());
            link.setFont(new Font("Segoe UI", Font.BOLD, 13));
            head.add(link, BorderLayout.EAST);
        }
        JPanel inner = new JPanel(new BorderLayout());
        inner.setOpaque(false);
        inner.add(head, BorderLayout.NORTH);
        inner.add(body, BorderLayout.CENTER);
        return card(inner);
    }

    public static RoundPanel titledCard(String title, String linkText, Runnable onLink, JComponent body) {
        return titledCard(new JLabel(title), linkText, onLink, body);
    }

    /** A coloured clickable tile with a symbol and a label (the dashboard shortcuts). htmlText may contain &lt;br&gt;. */
    public static RoundPanel actionTile(String glyph, String htmlText, Color bg, Color fg, Runnable onClick) {
        RoundPanel p = new RoundPanel(new BorderLayout(14, 0), 14, bg, null);
        p.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        JLabel icon = new JLabel(glyph);
        icon.setFont(new Font("Segoe UI Symbol", Font.BOLD, 24));
        icon.setForeground(fg);
        JLabel label = new JLabel("<html>" + htmlText + "</html>");
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(new Color(0x222222));
        p.add(icon, BorderLayout.WEST);
        p.add(label, BorderLayout.CENTER);
        return p.onClick(onClick);
    }

    // ---------- status colours and chips ----------

    /** Returns {background, text} colours for a status word such as PENDING or APPROVED. */
    public static Color[] statusColors(String s) {
        Color green = new Color(0x1E7B34), amber = new Color(0x9A6700), red = new Color(0xB71C1C);
        Color blue = new Color(0x1565C0), grey = new Color(0x666666);
        return switch (s) {
            case "APPROVED", "FULFILLED", "YES", "ACCEPTED" -> new Color[]{new Color(0xE6F4EA), green};
            case "PENDING" -> new Color[]{new Color(0xFFF3D6), amber};
            case "REJECTED", "CANCELLED", "NO", "EMERGENCY", "NEW", "DECLINED" -> new Color[]{new Color(0xFDE3E1), red};
            case "NORMAL" -> new Color[]{new Color(0xE3F0FD), blue};
            default -> new Color[]{new Color(0xEEEEEE), grey};
        };
    }

    /** A small rounded "pill" (a RoundPanel holding a label). */
    public static JComponent chip(String text, Color bg, Color fg) {
        RoundPanel pill = new RoundPanel(new FlowLayout(FlowLayout.CENTER, 12, 3), 24, bg, null);
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(fg);
        pill.add(label);
        return pill;
    }

    public static JComponent statusChip(String status) {
        Color[] c = statusColors(status);
        return chip(status.charAt(0) + status.substring(1).toLowerCase(), c[0], c[1]);
    }

    /** "5 min ago" style text for a timestamp. */
    public static String ago(Timestamp t) {
        long minutes = Math.max(0, (System.currentTimeMillis() - t.getTime()) / 60000);
        if (minutes < 1) return "just now";
        if (minutes < 60) return minutes + " min ago";
        if (minutes < 60 * 24) return (minutes / 60) + " hr ago";
        return (minutes / (60 * 24)) + " days ago";
    }

    // ---------- tables ----------

    /** Drop-down of Kerala districts; the first item is a "Select district" placeholder. */
    public static JComboBox<String> districtCombo() {
        String[] items = new String[Districts.ALL.length + 1];
        items[0] = "Select district";
        System.arraycopy(Districts.ALL, 0, items, 1, Districts.ALL.length);
        return combo(items);
    }

    /** The chosen district name, or null if the placeholder is still selected. */
    public static String districtValue(JComboBox<String> combo) {
        return combo.getSelectedIndex() <= 0 ? null : (String) combo.getSelectedItem();
    }

    /** Selects the given district, or the placeholder if it is empty or not a Kerala district. */
    public static void setDistrict(JComboBox<String> combo, String district) {
        combo.setSelectedItem(Districts.isValid(district) ? district : "Select district");
    }

    /** Read-only table; the first column is always the record id. Status-like columns are coloured. */
    public static JTable table(String... columns) {
        JTable t = new JTable(new DefaultTableModel(columns, 0));
        t.setDefaultEditor(Object.class, null); // cells are read-only
        t.setFont(BODY);
        t.setRowHeight(30);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionBackground(new Color(0xF4C7C3));
        t.setSelectionForeground(Color.BLACK);
        t.getTableHeader().setFont(BOLD);
        t.getTableHeader().setBackground(new Color(0xF3ECEA));
        t.getTableHeader().setForeground(TEXT);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // status-like columns are drawn in bold and coloured by their value (a lambda, so no extra class)
        TableCellRenderer plain = new DefaultTableCellRenderer();
        TableCellRenderer coloured = (table, value, selected, focus, row, col) -> {
            Component c = plain.getTableCellRendererComponent(table, value, selected, focus, row, col);
            c.setFont(BOLD);
            if (!selected) c.setForeground(statusColors(String.valueOf(value))[1]);
            return c;
        };
        for (int i = 0; i < columns.length; i++) {
            if (columns[i].equals("Status") || columns[i].equals("Urgency") || columns[i].equals("Eligible")) {
                t.getColumnModel().getColumn(i).setCellRenderer(coloured);
            }
        }
        return t;
    }

    public static void setRows(JTable table, List<Object[]> rows) {
        DefaultTableModel m = (DefaultTableModel) table.getModel();
        m.setRowCount(0);
        for (Object[] r : rows) m.addRow(r);
        // give wider columns more room: preferred width follows the longest text in each column
        for (int c = 0; c < table.getColumnCount(); c++) {
            int longest = Math.max(4, String.valueOf(table.getColumnName(c)).length());
            for (Object[] r : rows) longest = Math.max(longest, String.valueOf(r[c]).length());
            table.getColumnModel().getColumn(c).setPreferredWidth(Math.min(longest, 45) * 8 + 24);
        }
    }

    /** A scroll pane around a table with a white background, so empty space does not look grey. */
    public static JScrollPane scroll(JTable table) {
        JScrollPane s = new JScrollPane(table);
        s.setBorder(BorderFactory.createLineBorder(CARD_BORDER));
        s.getViewport().setBackground(Color.WHITE);
        return s;
    }

    /** Id (column 0) of the selected row, or -1 if nothing is selected. */
    public static int selectedId(JTable table) {
        int row = table.getSelectedRow();
        return row < 0 ? -1 : (Integer) table.getValueAt(row, 0);
    }

    /** Two-column label/field form. Call row() for each input. */
    public static class Form extends JPanel {
        private int y = 0;

        public Form() {
            super(new GridBagLayout());
            setOpaque(false);
        }

        public void row(String labelText, JComponent input) {
            GridBagConstraints g = new GridBagConstraints();
            g.gridy = y++;
            g.insets = new Insets(6, 4, 6, 10);
            g.anchor = GridBagConstraints.WEST;
            g.gridx = 0;
            add(label(labelText), g);
            g.gridx = 1;
            g.fill = GridBagConstraints.HORIZONTAL;
            g.weightx = 1;
            add(input, g);
        }
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "BloodCare", JOptionPane.ERROR_MESSAGE);
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "BloodCare", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Standard message for a failed database call. */
    public static void dbError(Component parent, Exception e) {
        error(parent, "Database problem: " + e.getMessage());
    }
}
