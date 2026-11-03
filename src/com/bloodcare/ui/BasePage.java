package com.bloodcare.ui;

import javax.swing.*;
import java.awt.*;

/**
 * BasePage - abstract parent of every page shown inside a dashboard (INHERITANCE). The dashboard shell
 * calls refresh() each time a page is opened, so a page overrides it when it has data to reload from MySQL.
 * It also holds the two layouts the pages share: "form card on the left + content card on the right"
 * and "one card on the left".
 */
public abstract class BasePage extends JPanel {

    protected BasePage(LayoutManager layout) {
        super(layout);
        setOpaque(false);
    }

    /** Reloads the page's data from MySQL. Pages without data (for example Settings) leave it empty. */
    public void refresh() {
    }

    /** Form card with a fixed width on the left, a content card filling the rest. */
    protected void splitLayout(JComponent form, int formWidth, JComponent content) {
        setLayout(new BorderLayout(18, 0));
        add(leftCard(form, formWidth), BorderLayout.WEST);
        add(Ui.card(content), BorderLayout.CENTER);
    }

    /** Only a card on the left (profile, settings). */
    protected void leftOnlyLayout(JComponent form, int formWidth) {
        setLayout(new BorderLayout());
        add(leftCard(form, formWidth), BorderLayout.WEST);
    }

    private static JPanel leftCard(JComponent form, int width) {
        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.add(Ui.card(form), BorderLayout.NORTH);
        left.setPreferredSize(new Dimension(width, 0));
        return left;
    }
}
