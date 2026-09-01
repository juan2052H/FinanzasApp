package com.finanzas.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;

public class DonutChartPanel extends JPanel {

    private String[] labels;
    private double[] values;
    private Color[] colors;
    private int hoveredSlice = -1;

    public DonutChartPanel(String[] labels, double[] values, Color[] colors) {
        this.labels = labels;
        this.values = values;
        this.colors = colors;
        setOpaque(false);
        setPreferredSize(new Dimension(200, 200));

        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                double dx = e.getX() - cx;
                double dy = e.getY() - cy;
                double dist = Math.sqrt(dx * dx + dy * dy);
                int outer = Math.min(getWidth(), getHeight()) / 2 - 5;
                int inner = outer - 55;
                int newHovered = -1;
                if (dist >= inner && dist <= outer) {
                    double angle = Math.toDegrees(Math.atan2(-dy, dx));
                    if (angle < 0) angle += 360;
                    double startAngle = 0;
                    double total = 0;
                    for (double v : values) total += v;
                    if (total <= 0) {
                        hoveredSlice = -1;
                        repaint();
                        return;
                    }
                    for (int i = 0; i < values.length; i++) {
                        double sweep = (values[i] / total) * 360;
                        double end = startAngle + sweep;
                        if (angle >= startAngle && angle < end) { newHovered = i; break; }
                        startAngle = end;
                    }
                }
                if (newHovered != hoveredSlice) { hoveredSlice = newHovered; repaint(); }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        int outer = Math.min(getWidth(), getHeight()) / 2 - 5;
        int inner = outer - 55;

        double total = 0;
        for (double v : values) total += v;
        if (values.length == 0 || total <= 0) {
            g2.setColor(new Color(0xf0f4f8));
            g2.fillOval(cx - outer, cy - outer, outer * 2, outer * 2);
            g2.setColor(Color.WHITE);
            g2.fill(new Ellipse2D.Double(cx - inner, cy - inner, inner * 2, inner * 2));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g2.setColor(AppColors.TEXT_SECONDARY);
            String empty = "Sin datos";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(empty, cx - fm.stringWidth(empty) / 2, cy + 4);
            g2.dispose();
            return;
        }

        double startAngle = 90;
        for (int i = 0; i < values.length; i++) {
            double sweep = -(values[i] / total) * 360;
            int expand = (i == hoveredSlice) ? 5 : 0;

            Shape arc = new Arc2D.Double(
                    cx - outer - expand, cy - outer - expand,
                    (outer + expand) * 2, (outer + expand) * 2,
                    startAngle, sweep, Arc2D.PIE);

            g2.setColor(colors[i % colors.length]);
            g2.fill(arc);

            // Border
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2));
            g2.draw(arc);

            startAngle += sweep;
        }

        // Inner hole
        g2.setColor(Color.WHITE);
        g2.fill(new Ellipse2D.Double(cx - inner, cy - inner, inner * 2, inner * 2));

        // Center text
        if (hoveredSlice >= 0) {
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g2.setColor(AppColors.TEXT_PRIMARY);
            String pct = (int) values[hoveredSlice] + "%";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(pct, cx - fm.stringWidth(pct) / 2, cy + 5);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            g2.setColor(AppColors.TEXT_SECONDARY);
            String lbl = labels[hoveredSlice];
            FontMetrics fm2 = g2.getFontMetrics();
            g2.drawString(lbl, cx - fm2.stringWidth(lbl) / 2, cy + 18);
        } else {
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g2.setColor(AppColors.TEXT_PRIMARY);
            String total2 = "100%";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(total2, cx - fm.stringWidth(total2) / 2, cy + 5);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            g2.setColor(AppColors.TEXT_SECONDARY);
            String lbl = "Total";
            FontMetrics fm2 = g2.getFontMetrics();
            g2.drawString(lbl, cx - fm2.stringWidth(lbl) / 2, cy + 18);
        }

        g2.dispose();
    }
}
