package com.bloodcare.ui;

import com.bloodcare.model.Admin;
import com.bloodcare.model.Donor;
import com.bloodcare.model.User;

import javax.swing.*;
import java.awt.*;

/**
 * MainFrame - the one and only window. A CardLayout swaps between
 * authentication and the dashboard, so no extra windows are opened.
 * Which dashboard opens depends on the role stored in the database.
 */
public class MainFrame extends JFrame {
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private JPanel dashboard;

    public MainFrame() {
        super("BloodCare - Blood Donation System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 780);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);
        root.add(new AuthPanel(this), "auth");
        setContentPane(root);
        // BloodCare starts at authentication so users can sign in immediately.
        showAuth();
    }

    public void showAuth() { cards.show(root, "auth"); }

    /** Opens the dashboard that matches the kind of account: an Admin gets the admin page, a Donor the user dashboard. */
    public void openDashboard(User user) {
        if (user instanceof Admin admin) dashboard = new AdminDashboard(this, admin);
        else if (user instanceof Donor donor) dashboard = new UserDashboard(this, donor);
        else throw new IllegalArgumentException("No dashboard for " + user);
        root.add(dashboard, "dashboard");
        cards.show(root, "dashboard");
    }

    /** Drops the dashboard (and the logged-in user with it) and returns to the home page. */
    public void signOut() {
        if (dashboard != null) {
            root.remove(dashboard);
            dashboard = null;
        }
        showAuth();
    }
}
