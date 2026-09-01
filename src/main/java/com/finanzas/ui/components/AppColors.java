package com.finanzas.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class AppColors {
    public static final Color SIDEBAR_BG = new Color(0x1a2744);
    public static final Color SIDEBAR_ACTIVE = new Color(0x1a73e8);
    public static final Color SIDEBAR_HOVER = new Color(0x243558);
    public static final Color SIDEBAR_TEXT = new Color(0xb0bec5);
    public static final Color SIDEBAR_TEXT_ACTIVE = Color.WHITE;

    public static final Color HEADER_BG = new Color(0x1a2744);
    public static final Color HEADER_TEXT = Color.WHITE;

    public static final Color MAIN_BG = new Color(0xf0f4f8);
    public static final Color CARD_BG = Color.WHITE;

    public static final Color CARD_SALDO = new Color(0x34a853);
    public static final Color CARD_INGRESOS = new Color(0x1a73e8);
    public static final Color CARD_GASTOS = new Color(0xea4335);
    public static final Color CARD_AHORROS = new Color(0x00ACC1);

    public static final Color[] DONUT_COLORS = {
        new Color(0x1a73e8),
        new Color(0x34a853),
        new Color(0xff6d00),
        new Color(0xab47bc),
        new Color(0xfb8c00),
        new Color(0xef5350),
        new Color(0x90a4ae)
    };

    public static final Color BAR_INGRESOS = new Color(0x34a853);
    public static final Color BAR_GASTOS = new Color(0xea4335);

    public static final Color TEXT_PRIMARY = new Color(0x1a2744);
    public static final Color TEXT_SECONDARY = new Color(0x6b7280);
    public static final Color TEXT_MUTED = new Color(0x9ca3af);

    public static final Color ACCENT_BLUE = new Color(0x1a73e8);
    public static final Color ACCENT_GREEN = new Color(0x34a853);
    public static final Color ACCENT_RED = new Color(0xea4335);
    public static final Color ACCENT_YELLOW = new Color(0xfbbc04);
    public static final Color ACCENT_ORANGE = new Color(0xff6d00);
    public static final Color BORDER = new Color(0xe5e7eb);

    public static final Color BTN_GREEN = new Color(0x34a853);
    public static final Color BTN_RED = new Color(0xea4335);
    public static final Color BTN_BLUE = new Color(0x1a73e8);

    public static Font getFont(int style, int size) {
        return new Font("Segoe UI", style, size);
    }

    public static JPanel roundedPanel(int radius) {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), radius, radius));
                g2.dispose();
            }
        };
    }
}
