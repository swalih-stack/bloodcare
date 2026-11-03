package com.bloodcare.ui;

import javax.swing.ImageIcon;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/** Icons drawn with Java2D (so they look the same on every computer and need no image files). */
public final class Icons {
    public static final int DROP = 0, PEOPLE = 1, CLIPBOARD = 2;

    private Icons() {}

    /** Paints into a small image and wraps it as an icon, so a plain JLabel/JButton can show it (no extra component class). */
    private static ImageIcon image(int w, int h, Consumer<Graphics2D> painter) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        painter.accept(g);
        g.dispose();
        return new ImageIcon(img);
    }

    public static ImageIcon dropIcon(int size, Color color) {
        return image(size, size, g -> drop(g, 0, 0, size, color));
    }

    /** A round pink badge holding one of the icons (DROP, PEOPLE or CLIPBOARD). */
    public static ImageIcon badge(int kind) {
        return image(56, 56, g -> {
            g.setColor(new Color(0xFBE9E7));
            g.fillOval(0, 0, 56, 56);
            Color red = new Color(0xD32F2F);
            switch (kind) {
                case DROP -> drop(g, 15, 12, 28, red);
                case PEOPLE -> people(g, 13, 13, 30, red);
                default -> clipboard(g, 13, 12, 30, red);
            }
        });
    }

    /** The notification bell, with a red dot when there is something unread. */
    public static ImageIcon bellIcon(boolean unread) {
        return image(38, 38, g -> {
            bell(g, 5, 5, 28, new Color(0x444444));
            if (unread) {
                g.setColor(Color.WHITE);
                g.fillOval(22, 3, 13, 13);
                g.setColor(new Color(0xE53935));
                g.fillOval(24, 5, 9, 9);
            }
        });
    }

    private static Graphics2D prepare(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    /** A blood drop. x,y is the top-left corner of a size x size box. */
    public static void drop(Graphics g, int x, int y, int size, Color color) {
        Graphics2D g2 = prepare(g);
        double cx = x + size / 2.0;
        double r = size * 0.36;
        double cy = y + size - r - size * 0.04;
        Path2D p = new Path2D.Double();
        p.moveTo(cx, y);
        p.curveTo(cx + r * 0.35, y + size * 0.28, cx + r, cy - r * 0.55, cx + r, cy);
        p.append(new Arc2D.Double(cx - r, cy - r, 2 * r, 2 * r, 0, -180, Arc2D.OPEN), true);
        p.curveTo(cx - r, cy - r * 0.55, cx - r * 0.35, y + size * 0.28, cx, y);
        p.closePath();
        g2.setColor(color);
        g2.fill(p);
        g2.setColor(new Color(255, 255, 255, 110));
        g2.fill(new Ellipse2D.Double(cx - r * 0.55, cy - r * 0.2, r * 0.4, r * 0.55));
        g2.dispose();
    }

    /** Two people (head + shoulders). */
    public static void people(Graphics g, int x, int y, int size, Color color) {
        Graphics2D g2 = prepare(g);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(size * 0.07f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double head = size * 0.28;
        g2.draw(new Ellipse2D.Double(x + size * 0.12, y + size * 0.1, head, head));
        g2.draw(new Arc2D.Double(x + size * 0.02, y + size * 0.5, size * 0.5, size * 0.5, 0, 180, Arc2D.OPEN));
        g2.draw(new Ellipse2D.Double(x + size * 0.55, y + size * 0.2, head * 0.9, head * 0.9));
        g2.draw(new Arc2D.Double(x + size * 0.5, y + size * 0.58, size * 0.46, size * 0.42, 0, 180, Arc2D.OPEN));
        g2.dispose();
    }

    /** A clipboard with a tick. */
    public static void clipboard(Graphics g, int x, int y, int size, Color color) {
        Graphics2D g2 = prepare(g);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(size * 0.07f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new RoundRectangle2D.Double(x + size * 0.14, y + size * 0.14, size * 0.72, size * 0.8, 6, 6));
        g2.fill(new RoundRectangle2D.Double(x + size * 0.36, y + size * 0.04, size * 0.28, size * 0.16, 4, 4));
        Path2D tick = new Path2D.Double();
        tick.moveTo(x + size * 0.32, y + size * 0.56);
        tick.lineTo(x + size * 0.46, y + size * 0.7);
        tick.lineTo(x + size * 0.7, y + size * 0.4);
        g2.draw(tick);
        g2.dispose();
    }

    /** A notification bell. */
    public static void bell(Graphics g, int x, int y, int size, Color color) {
        Graphics2D g2 = prepare(g);
        g2.setColor(color);
        Path2D p = new Path2D.Double();
        double w = size, h = size;
        p.moveTo(x + w * 0.18, y + h * 0.72);
        p.curveTo(x + w * 0.3, y + h * 0.62, x + w * 0.26, y + h * 0.42, x + w * 0.3, y + h * 0.36);
        p.curveTo(x + w * 0.34, y + h * 0.14, x + w * 0.66, y + h * 0.14, x + w * 0.7, y + h * 0.36);
        p.curveTo(x + w * 0.74, y + h * 0.42, x + w * 0.7, y + h * 0.62, x + w * 0.82, y + h * 0.72);
        p.closePath();
        g2.fill(p);
        g2.fill(new Ellipse2D.Double(x + w * 0.4, y + h * 0.76, w * 0.2, h * 0.16));
        g2.fill(new Ellipse2D.Double(x + w * 0.46, y + h * 0.06, w * 0.08, h * 0.1));
        g2.dispose();
    }
}
