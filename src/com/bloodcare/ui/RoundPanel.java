package com.bloodcare.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** A panel with rounded corners, a fill colour and an optional thin border line. */
public class RoundPanel extends JPanel {
    private final int arc;
    private final Color fill;
    private final Color border;

    public RoundPanel(LayoutManager layout, int arc, Color fill, Color border) {
        super(layout);
        this.arc = arc;
        this.fill = fill;
        this.border = border;
        setOpaque(false);
    }

    /** Makes the panel behave like a button: hand cursor, and the action runs when it is clicked. */
    public RoundPanel onClick(Runnable action) {
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { action.run(); }
        });
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        if (border != null) {
            g2.setColor(border);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
