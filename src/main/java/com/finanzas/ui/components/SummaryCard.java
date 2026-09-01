package com.finanzas.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.text.NumberFormat;
import java.util.Locale;

public class SummaryCard extends JPanel {

    private final String titulo;
    private final double monto;
    private final String subtitulo;
    private final String icono;
    private final Color cardColor;
    private final boolean showExtra;
    private final String extraText;

    public SummaryCard(String titulo, double monto, String subtitulo, String icono, Color cardColor) {
        this(titulo, monto, subtitulo, icono, cardColor, false, "");
    }

    public SummaryCard(String titulo, double monto, String subtitulo, String icono, Color cardColor, boolean showExtra, String extraText) {
        this.titulo = titulo;
        this.monto = monto;
        this.subtitulo = subtitulo;
        this.icono = icono;
        this.cardColor = cardColor;
        this.showExtra = showExtra;
        this.extraText = extraText;
        setOpaque(false);
        setPreferredSize(new Dimension(200, 115));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int width = getWidth();
        int height = getHeight();

        g2.setColor(cardColor);
        g2.fill(new RoundRectangle2D.Double(0, 0, width, height, 14, 14));

        GradientPaint paint = new GradientPaint(0, 0, new Color(255, 255, 255, 30), 0, height, new Color(0, 0, 0, 20));
        g2.setPaint(paint);
        g2.fill(new RoundRectangle2D.Double(0, 0, width, height, 14, 14));

        g2.setColor(new Color(255, 255, 255, 50));
        g2.fillOval(width - 58, 12, 44, 44);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        FontMetrics iconMetrics = g2.getFontMetrics();
        int iconX = width - 58 + (44 - iconMetrics.stringWidth(icono)) / 2;
        g2.drawString(icono, iconX, 44);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.setColor(new Color(255, 255, 255, 200));
        g2.drawString(titulo, 14, 24);

        NumberFormat nf = NumberFormat.getInstance(new Locale("es", "CO"));
        String amount = "$" + nf.format((long) monto);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
        g2.setColor(Color.WHITE);
        g2.drawString(amount, 14, 58);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(new Color(255, 255, 255, 200));
        g2.drawString(subtitulo, 14, 78);

        if (showExtra && !extraText.isEmpty()) {
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g2.setColor(new Color(255, 255, 255, 230));
            g2.drawString(extraText, 14, 96);
        }

        g2.dispose();
    }
}
