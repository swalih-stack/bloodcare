package com.bloodcare.ui;

import javax.swing.*;
import java.awt.*;

/** StatCard - a white card with a round icon, a caption, a big number and a small green line under it. */
public class StatCard extends RoundPanel {
    private static final Color DEFAULT_VALUE_COLOR = new Color(0x1B1B1B);

    private final JLabel value = new JLabel("0");
    private final JLabel sub = new JLabel(" ");

    /** icon is Icons.DROP, Icons.PEOPLE or Icons.CLIPBOARD. */
    public StatCard(int icon, String caption, int valueSize) {
        super(new BorderLayout(14, 0), 18, Color.WHITE, Ui.CARD_BORDER);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 14, 16));

        JLabel badge = new JLabel(Icons.badge(icon));
        badge.setVerticalAlignment(SwingConstants.TOP);

        JLabel cap = new JLabel(caption);
        cap.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cap.setForeground(new Color(0x555555));
        value.setFont(new Font("Segoe UI", Font.BOLD, valueSize));
        value.setForeground(DEFAULT_VALUE_COLOR);
        sub.setFont(new Font("Segoe UI", Font.BOLD, 12));
        sub.setForeground(new Color(0x2E9E48));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(cap);
        text.add(value);
        text.add(Box.createVerticalStrut(2));
        text.add(sub);
        add(badge, BorderLayout.WEST);
        add(text, BorderLayout.CENTER);
    }

    public void set(String valueText, String subText) {
        value.setText(valueText);
        sub.setText(subText);
    }

    /** Colours the big number (for example green for "Eligible"); null restores the default. */
    public void setValueColor(Color color) {
        value.setForeground(color == null ? DEFAULT_VALUE_COLOR : color);
    }
}
