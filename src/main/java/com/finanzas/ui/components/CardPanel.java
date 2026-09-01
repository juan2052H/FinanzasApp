package com.finanzas.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class CardPanel extends JPanel {
    private final int radius;
    private final boolean bordered;

    public CardPanel() {
        this(12, true);
    }

    public CardPanel(int radius, boolean bordered) {
        this.radius = radius;
        this.bordered = bordered;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), radius, radius));
        if (bordered) {
            g2.setColor(AppColors.BORDER);
            g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, radius, radius));
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
