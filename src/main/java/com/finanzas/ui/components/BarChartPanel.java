package com.finanzas.ui.components;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;

public class BarChartPanel extends JPanel {

    private final String[] labels;
    private final double[] ingresos;
    private final double[] gastos;
    private int hoveredBar = -1;

    public BarChartPanel(String[] labels, double[] ingresos, double[] gastos) {
        this.labels = labels;
        this.ingresos = ingresos;
        this.gastos = gastos;
        setOpaque(false);

        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int padL = 45;
                int padR = 10;
                int width = getWidth() - padL - padR;
                int count = labels.length;
                if (count == 0 || width <= 0) {
                    hoveredBar = -1;
                    return;
                }
                int groupW = Math.max(1, width / count);
                int col = (e.getX() - padL) / groupW;
                int newHovered = col >= 0 && col < count ? col : -1;
                if (newHovered != hoveredBar) {
                    hoveredBar = newHovered;
                    repaint();
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int padL = 45;
        int padR = 10;
        int padT = 20;
        int padB = 40;
        int width = getWidth() - padL - padR;
        int height = getHeight() - padT - padB;
        if (width <= 0 || height <= 0 || labels.length == 0) {
            g2.dispose();
            return;
        }

        double maxVal = 0;
        for (double value : ingresos) {
            maxVal = Math.max(maxVal, value);
        }
        for (double value : gastos) {
            maxVal = Math.max(maxVal, value);
        }
        if (maxVal <= 0) {
            maxVal = 1;
        } else {
            maxVal = Math.ceil(maxVal / 1000000.0) * 1000000;
        }

        int gridLines = 5;
        g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{3}, 0));
        for (int i = 0; i <= gridLines; i++) {
            int y = padT + height - (i * height / gridLines);
            g2.setColor(new Color(0xe5e7eb));
            g2.drawLine(padL, y, padL + width, y);
            double value = (maxVal / gridLines) * i;
            g2.setColor(AppColors.TEXT_MUTED);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            String label = "$" + (int) (value / 1000000) + "M";
            g2.drawString(label, 2, y + 4);
        }
        g2.setStroke(new BasicStroke(1));

        int count = labels.length;
        int groupW = Math.max(1, width / count);
        int barW = Math.max(8, (int) (groupW * 0.28));
        int gap = Math.max(2, (int) (groupW * 0.06));
        NumberFormat nf = NumberFormat.getInstance(com.finanzas.data.DataManager.getInstance().getDisplayLocale());

        for (int i = 0; i < count; i++) {
            int gx = padL + i * groupW;
            boolean hovered = i == hoveredBar;

            int ingresoHeight = (int) ((ingresos[i] / maxVal) * height);
            int ingresoX = gx + (groupW - barW * 2 - gap) / 2;
            int ingresoY = padT + height - ingresoHeight;

            g2.setColor(hovered ? AppColors.BAR_INGRESOS.brighter() : AppColors.BAR_INGRESOS);
            g2.fillRoundRect(ingresoX, ingresoY, barW, ingresoHeight, 4, 4);

            int gastoHeight = (int) ((gastos[i] / maxVal) * height);
            int gastoX = ingresoX + barW + gap;
            int gastoY = padT + height - gastoHeight;

            g2.setColor(hovered ? AppColors.BAR_GASTOS.brighter() : AppColors.BAR_GASTOS);
            g2.fillRoundRect(gastoX, gastoY, barW, gastoHeight, 4, 4);

            if (hovered) {
                String tip = "$" + nf.format((long) ingresos[i]) + " / $" + nf.format((long) gastos[i]);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(tip) + 8;
                int tx = gx + groupW / 2 - tw / 2;
                int ty = padT - 4;
                g2.setColor(new Color(0x1a2744));
                g2.fillRoundRect(tx, ty - 14, tw, 16, 6, 6);
                g2.setColor(Color.WHITE);
                g2.drawString(tip, tx + 4, ty);
            }

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.setColor(AppColors.TEXT_SECONDARY);
            FontMetrics fm = g2.getFontMetrics();
            int labelW = fm.stringWidth(labels[i]);
            g2.drawString(labels[i], gx + groupW / 2 - labelW / 2, padT + height + 16);
        }

        int legendY = padT + height + 28;
        g2.setColor(AppColors.BAR_INGRESOS);
        g2.fillRect(padL, legendY, 12, 8);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        g2.setColor(AppColors.TEXT_SECONDARY);
        g2.drawString("Ingresos", padL + 16, legendY + 8);

        g2.setColor(AppColors.BAR_GASTOS);
        g2.fillRect(padL + 80, legendY, 12, 8);
        g2.drawString("Gastos", padL + 96, legendY + 8);

        g2.dispose();
    }
}
