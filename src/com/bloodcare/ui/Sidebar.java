package com.bloodcare.ui;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Sidebar - the dark-red menu on the left, shared by the admin and user dashboards.
 * The logo and the user's avatar/name are painted straight onto the sidebar (above the menu buttons).
 * It can collapse to an icons-only strip (toggle()) with a short slide animation.
 * An entry with children (like "Users") expands to show them; clicking it opens its first child.
 */
public class Sidebar extends JPanel {
    /** One menu item. Build with Entry.of(...), optionally .withBadge(...) and .with(children...). */
    public record Entry(String key, String label, String icon, List<Entry> children, Supplier<Integer> badge) {
        public static Entry of(String key, String label, String icon) {
            return new Entry(key, label, icon, List.of(), null);
        }

        public Entry withBadge(Supplier<Integer> b) {
            return new Entry(key, label, icon, children, b);
        }

        public Entry with(Entry... kids) {
            return new Entry(key, label, icon, List.of(kids), badge);
        }
    }

    private static final int EXPANDED = 250;
    private static final int COLLAPSED = 76;
    private static final int HEADER = 164; // height reserved at the top for the logo and the profile
    private static final Font ICON_FONT = new Font("Segoe UI Symbol", Font.PLAIN, 17);
    private static final Color SOFT_WHITE = new Color(0xF3C9C9);

    private int width = EXPANDED;
    private boolean collapsed = false;
    private Timer animation;

    private final String subtitle;
    private final String userName;
    private final String role;
    private final Map<String, NavButton> buttons = new LinkedHashMap<>();
    private final Map<String, JPanel> childBoxes = new HashMap<>();
    private final Map<String, String> parentOf = new HashMap<>();
    private final Set<String> expanded = new HashSet<>();

    public Sidebar(String subtitle, String userName, String role, List<Entry> entries,
                   Consumer<String> onNavigate, Runnable onLogout) {
        this.subtitle = subtitle;
        this.userName = userName;
        this.role = role;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        add(Box.createVerticalStrut(HEADER));

        for (Entry e : entries) {
            NavButton b = new NavButton(e, false);
            add(b);
            if (e.children().isEmpty()) {
                b.addActionListener(ev -> onNavigate.accept(e.key()));
            } else {
                b.expandable = true;
                JPanel box = new JPanel();
                box.setOpaque(false);
                box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
                box.setAlignmentX(Component.LEFT_ALIGNMENT);
                for (Entry child : e.children()) {
                    NavButton cb = new NavButton(child, true);
                    cb.addActionListener(ev -> onNavigate.accept(child.key()));
                    box.add(cb);
                    parentOf.put(child.key(), e.key());
                }
                box.setVisible(false);
                childBoxes.put(e.key(), box);
                add(box);
                b.addActionListener(ev -> {
                    setExpanded(e.key(), !expanded.contains(e.key()));
                    if (expanded.contains(e.key())) onNavigate.accept(e.children().get(0).key());
                });
            }
        }
        add(Box.createVerticalGlue());
        NavButton logout = new NavButton(Entry.of("logout", "Logout", "➜"), false);
        logout.addActionListener(ev -> onLogout.run());
        add(logout);
    }

    /** Highlights the entry for the page that is open, expanding its parent group if needed. */
    public void setActive(String key) {
        for (NavButton b : buttons.values()) {
            b.active = b.entry.key().equals(key);
            b.repaint();
        }
        String parent = parentOf.get(key);
        if (parent != null) setExpanded(parent, true);
    }

    /** Re-reads the numbers shown on badges (for example pending requests). */
    public void refreshBadges() {
        for (NavButton b : buttons.values()) {
            if (b.entry.badge() != null) {
                b.badgeCount = b.entry.badge().get();
                b.repaint();
            }
        }
    }

    private void setExpanded(String key, boolean open) {
        if (open) expanded.add(key);
        else expanded.remove(key);
        JPanel box = childBoxes.get(key);
        if (box != null) box.setVisible(open && !collapsed);
        NavButton b = buttons.get(key);
        if (b != null) b.repaint();
        revalidate();
        repaint();
    }

    /** Collapse to icons only, or expand back, with a slide animation. */
    public void toggle() {
        collapsed = !collapsed;
        for (Map.Entry<String, JPanel> e : childBoxes.entrySet()) {
            e.getValue().setVisible(!collapsed && expanded.contains(e.getKey()));
        }
        int target = collapsed ? COLLAPSED : EXPANDED;
        if (animation != null) animation.stop();
        animation = new Timer(10, ev -> {
            width = width < target ? Math.min(target, width + 30) : Math.max(target, width - 30);
            revalidate();
            repaint();
            if (width == target) ((Timer) ev.getSource()).stop();
        });
        animation.start();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, super.getPreferredSize().height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setPaint(new GradientPaint(0, 0, new Color(0x8A1616), 0, getHeight(), new Color(0x470909)));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        paintLogo(g2);
        paintProfile(g2);
        g2.dispose();
        super.paintComponent(g);
    }

    /** Logo drop and the "Blood Care" text (text hidden when collapsed). */
    private void paintLogo(Graphics2D g2) {
        Icons.drop(g2, collapsed ? (getWidth() - 36) / 2 : 20, 20, 36, new Color(0xE53935));
        if (collapsed) return;
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
        g2.drawString("Blood Care", 66, 36);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.setColor(SOFT_WHITE);
        g2.drawString(subtitle, 66, 54);
    }

    /** Round avatar with the user's initial, plus the name and role (hidden when collapsed). */
    private void paintProfile(Graphics2D g2) {
        int top = 78, avatar = 46;
        int ax = collapsed ? (getWidth() - avatar) / 2 : 22;
        g2.setColor(Color.WHITE);
        g2.fillOval(ax, top + 14, avatar, avatar);
        g2.setColor(Ui.RED);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
        String initial = userName.isEmpty() ? "?" : userName.substring(0, 1).toUpperCase();
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(initial, ax + (avatar - fm.stringWidth(initial)) / 2,
                top + 14 + (avatar + fm.getAscent() - fm.getDescent()) / 2);
        if (collapsed) return;
        Shape oldClip = g2.getClip();
        g2.setClip(0, top, getWidth() - 10, 76);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
        g2.drawString(userName, 80, top + 33);
        g2.setColor(SOFT_WHITE);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.drawString(role, 80, top + 51);
        g2.setClip(oldClip);
    }

    /** One clickable menu row, fully custom painted. The hover highlight uses the button's own rollover state. */
    private class NavButton extends JButton {
        final Entry entry;
        final boolean child;
        boolean active;
        boolean expandable;
        int badgeCount;

        NavButton(Entry entry, boolean child) {
            this.entry = entry;
            this.child = child;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAlignmentX(LEFT_ALIGNMENT);
            int h = child ? 34 : 42;
            setPreferredSize(new Dimension(EXPANDED, h));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
            setToolTipText(entry.label());
            buttons.put(entry.key(), this);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            if (active) {
                g2.setColor(new Color(0xD32F2F));
                g2.fillRoundRect(10, 2, w - 20, h - 4, 14, 14);
            } else if (getModel().isRollover()) {
                g2.setColor(new Color(255, 255, 255, 30));
                g2.fillRoundRect(10, 2, w - 20, h - 4, 14, 14);
            }
            g2.setColor(Color.WHITE);
            if (!child) {
                g2.setFont(ICON_FONT);
                FontMetrics im = g2.getFontMetrics();
                int iconCenter = collapsed ? w / 2 : 36;
                g2.drawString(entry.icon(), iconCenter - im.stringWidth(entry.icon()) / 2,
                        (h + im.getAscent() - im.getDescent()) / 2);
                if (collapsed && badgeCount > 0) {
                    g2.setColor(new Color(0xFF5252));
                    g2.fillOval(iconCenter + 6, 6, 10, 10);
                }
            }
            if (!collapsed) {
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", child ? Font.PLAIN : Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(entry.label(), child ? 66 : 58, (h + fm.getAscent() - fm.getDescent()) / 2);
                int right = w - 24;
                if (expandable) {
                    boolean open = expanded.contains(entry.key());
                    int cx = right - 4, cy = h / 2;
                    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    if (open) g2.drawPolyline(new int[]{cx - 5, cx, cx + 5}, new int[]{cy + 2, cy - 3, cy + 2}, 3);
                    else g2.drawPolyline(new int[]{cx - 5, cx, cx + 5}, new int[]{cy - 2, cy + 3, cy - 2}, 3);
                } else if (badgeCount > 0) {
                    String text = String.valueOf(badgeCount);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    FontMetrics bm = g2.getFontMetrics();
                    int bw = Math.max(22, bm.stringWidth(text) + 12);
                    g2.setColor(new Color(0xFF5252));
                    g2.fillRoundRect(right - bw + 6, (h - 22) / 2, bw, 22, 22, 22);
                    g2.setColor(Color.WHITE);
                    g2.drawString(text, right - bw + 6 + (bw - bm.stringWidth(text)) / 2, (h - 22) / 2 + 16);
                }
            }
            g2.dispose();
        }
    }
}
