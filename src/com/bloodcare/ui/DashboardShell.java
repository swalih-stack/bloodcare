package com.bloodcare.ui;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * DashboardShell - the frame shared by the admin and user dashboards:
 * collapsible sidebar on the left, a header (toggle button, page title, notification bell,
 * live date/time) and the page area that swaps with a CardLayout.
 * Every page is a BasePage and is reloaded from MySQL each time it is shown.
 */
public class DashboardShell extends JPanel {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, String[]> titles = new HashMap<>();
    private final Map<String, BasePage> pages = new HashMap<>();
    private final Sidebar sidebar;
    private final JLabel title = new JLabel();
    private final JLabel subtitle = new JLabel();
    private final JLabel clock = new JLabel();
    private final JButton bell = new JButton(Icons.bellIcon(false));
    private final Supplier<Integer> unreadCount;
    private final String notificationsKey;
    private final Timer clockTimer = new Timer(1000, e -> updateClock());
    private final Timer pollTimer = new Timer(15000, e -> refreshBadges());

    public DashboardShell(MainFrame frame, String brandSubtitle, String userName, String roleLabel,
                          List<Sidebar.Entry> entries, Supplier<Integer> unreadCount, String notificationsKey) {
        super(new BorderLayout());
        this.unreadCount = unreadCount;
        this.notificationsKey = notificationsKey;
        setBackground(Ui.BG);

        sidebar = new Sidebar(brandSubtitle, userName, roleLabel, entries, this::show,
                () -> { if (frame != null) frame.signOut(); });
        add(sidebar, BorderLayout.WEST);

        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        right.add(header(), BorderLayout.NORTH);
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(0, 26, 24, 26));
        right.add(content, BorderLayout.CENTER);
        add(right, BorderLayout.CENTER);
        updateClock();
    }

    private JPanel header() {
        JButton toggle = new JButton("☰");
        toggle.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 22));
        toggle.setForeground(Ui.TEXT);
        toggle.setContentAreaFilled(false);
        toggle.setBorderPainted(false);
        toggle.setFocusPainted(false);
        toggle.setToolTipText("Collapse / expand menu");
        toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggle.addActionListener(e -> sidebar.toggle());

        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(new Color(0x1B1B1B));
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(Ui.MUTED);
        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(title);
        titles.add(subtitle);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        left.add(toggle);
        left.add(titles);

        // the bell is a plain flat button; refreshBadges() swaps its icon (with / without the red dot)
        bell.setContentAreaFilled(false);
        bell.setBorderPainted(false);
        bell.setFocusPainted(false);
        bell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bell.addActionListener(e -> show(notificationsKey));

        clock.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        clock.setForeground(new Color(0x444444));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 0));
        right.setOpaque(false);
        right.add(bell);
        right.add(clock);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(18, 18, 14, 26));
        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    /** Registers a page under a key, with the title and subtitle shown in the header when it is open. */
    public void addPage(String key, String pageTitle, String pageSubtitle, BasePage page) {
        titles.put(key, new String[]{pageTitle, pageSubtitle});
        pages.put(key, page);
        content.add(page, key);
    }

    /** Opens a page: reloads its data, updates the header, sidebar highlight and badges. */
    public void show(String key) {
        String[] t = titles.get(key);
        if (t == null) return;
        title.setText(t[0]);
        subtitle.setText(t[1]);
        pages.get(key).refresh();
        cards.show(content, key);
        sidebar.setActive(key);
        refreshBadges();
    }

    /** Re-reads the unread count (bell) and sidebar badges. Called after any change and every 15 seconds. */
    public void refreshBadges() {
        sidebar.refreshBadges();
        int unread = unreadCount.get();
        bell.setIcon(Icons.bellIcon(unread > 0));
        bell.setToolTipText(unread == 0 ? "No new notifications" : unread + " unread notification(s)");
    }

    private void updateClock() {
        LocalDateTime now = LocalDateTime.now();
        clock.setText("<html><div style='text-align:right'>" + DAY.format(now) + "<br>" + TIME.format(now) + "</div></html>");
    }

    // timers run only while the dashboard is on screen
    @Override
    public void addNotify() {
        super.addNotify();
        clockTimer.start();
        pollTimer.start();
    }

    @Override
    public void removeNotify() {
        clockTimer.stop();
        pollTimer.stop();
        super.removeNotify();
    }
}
