package com.finanzas.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class AppColors {
    private static String currentTheme = "LIGHT";

    public static Color SIDEBAR_BG = new Color(0x1a2744);
    public static Color SIDEBAR_ACTIVE = new Color(0x1a73e8);
    public static Color SIDEBAR_HOVER = new Color(0x243558);
    public static Color SIDEBAR_TEXT = new Color(0xb0bec5);
    public static Color SIDEBAR_TEXT_ACTIVE = Color.WHITE;

    public static Color HEADER_BG = new Color(0x1a2744);
    public static Color HEADER_TEXT = Color.WHITE;

    public static Color MAIN_BG = new Color(0xf0f4f8);
    public static Color CARD_BG = Color.WHITE;

    public static Color CARD_SALDO = new Color(0x34a853);
    public static Color CARD_INGRESOS = new Color(0x1a73e8);
    public static Color CARD_GASTOS = new Color(0xea4335);
    public static Color CARD_AHORROS = new Color(0x00ACC1);

    public static Color[] DONUT_COLORS = {
        new Color(0x1a73e8),
        new Color(0x34a853),
        new Color(0xff6d00),
        new Color(0xab47bc),
        new Color(0xfb8c00),
        new Color(0xef5350),
        new Color(0x90a4ae)
    };

    public static Color BAR_INGRESOS = new Color(0x34a853);
    public static Color BAR_GASTOS = new Color(0xea4335);

    public static Color TEXT_PRIMARY = new Color(0x1a2744);
    public static Color TEXT_SECONDARY = new Color(0x6b7280);
    public static Color TEXT_MUTED = new Color(0x9ca3af);

    public static Color ACCENT_BLUE = new Color(0x1a73e8);
    public static Color ACCENT_GREEN = new Color(0x34a853);
    public static Color ACCENT_RED = new Color(0xea4335);
    public static Color ACCENT_YELLOW = new Color(0xfbbc04);
    public static Color ACCENT_ORANGE = new Color(0xff6d00);
    public static Color BORDER = new Color(0xe5e7eb);

    public static Color BTN_GREEN = new Color(0x34a853);
    public static Color BTN_RED = new Color(0xea4335);
    public static Color BTN_BLUE = new Color(0x1a73e8);

    public static void applyTheme(String theme) {
        String normalized = theme == null || theme.trim().isEmpty()
                ? "LIGHT"
                : theme.trim().toUpperCase(java.util.Locale.ROOT);
        if ("DARK".equals(normalized)) {
            applyDark();
        } else {
            applyLight();
            normalized = "SYSTEM".equals(normalized) ? "SYSTEM" : "LIGHT";
        }
        currentTheme = normalized;
        configureSwingDefaults();
    }

    public static boolean isDarkTheme() {
        return "DARK".equals(currentTheme);
    }

    public static String currentTheme() {
        return currentTheme;
    }

    private static void applyLight() {
        SIDEBAR_BG = new Color(0x1a2744);
        SIDEBAR_ACTIVE = new Color(0x1a73e8);
        SIDEBAR_HOVER = new Color(0x243558);
        SIDEBAR_TEXT = new Color(0xb0bec5);
        SIDEBAR_TEXT_ACTIVE = Color.WHITE;
        HEADER_BG = new Color(0x1a2744);
        HEADER_TEXT = Color.WHITE;
        MAIN_BG = new Color(0xf0f4f8);
        CARD_BG = Color.WHITE;
        TEXT_PRIMARY = new Color(0x1a2744);
        TEXT_SECONDARY = new Color(0x6b7280);
        TEXT_MUTED = new Color(0x9ca3af);
        BORDER = new Color(0xe5e7eb);
        setCharts();
    }

    private static void applyDark() {
        SIDEBAR_BG = new Color(0x111827);
        SIDEBAR_ACTIVE = new Color(0x3b82f6);
        SIDEBAR_HOVER = new Color(0x1f2937);
        SIDEBAR_TEXT = new Color(0xcbd5e1);
        SIDEBAR_TEXT_ACTIVE = Color.WHITE;
        HEADER_BG = new Color(0x111827);
        HEADER_TEXT = Color.WHITE;
        MAIN_BG = new Color(0x0f172a);
        CARD_BG = new Color(0x1f2937);
        TEXT_PRIMARY = new Color(0xf8fafc);
        TEXT_SECONDARY = new Color(0xcbd5e1);
        TEXT_MUTED = new Color(0x94a3b8);
        BORDER = new Color(0x334155);
        setCharts();
    }

    private static void setCharts() {
        CARD_SALDO = new Color(0x34a853);
        CARD_INGRESOS = new Color(0x1a73e8);
        CARD_GASTOS = new Color(0xea4335);
        CARD_AHORROS = new Color(0x00ACC1);
        DONUT_COLORS = new Color[]{
            new Color(0x1a73e8),
            new Color(0x34a853),
            new Color(0xff6d00),
            new Color(0xab47bc),
            new Color(0xfb8c00),
            new Color(0xef5350),
            new Color(0x90a4ae)
        };
        BAR_INGRESOS = new Color(0x34a853);
        BAR_GASTOS = new Color(0xea4335);
        ACCENT_BLUE = new Color(0x1a73e8);
        ACCENT_GREEN = new Color(0x34a853);
        ACCENT_RED = new Color(0xea4335);
        ACCENT_YELLOW = new Color(0xfbbc04);
        ACCENT_ORANGE = new Color(0xff6d00);
        BTN_GREEN = ACCENT_GREEN;
        BTN_RED = ACCENT_RED;
        BTN_BLUE = ACCENT_BLUE;
    }

    private static void configureSwingDefaults() {
        UIManager.put("Panel.background", CARD_BG);
        UIManager.put("Viewport.background", CARD_BG);
        UIManager.put("ScrollPane.background", CARD_BG);
        UIManager.put("Table.background", CARD_BG);
        UIManager.put("Table.foreground", TEXT_PRIMARY);
        UIManager.put("Table.gridColor", BORDER);
        UIManager.put("TableHeader.background", isDarkTheme() ? new Color(0x334155) : new Color(0xf8fafc));
        UIManager.put("TableHeader.foreground", TEXT_PRIMARY);
        UIManager.put("Label.foreground", TEXT_PRIMARY);
        UIManager.put("TextField.background", isDarkTheme() ? new Color(0x111827) : Color.WHITE);
        UIManager.put("TextField.foreground", TEXT_PRIMARY);
        UIManager.put("ComboBox.background", isDarkTheme() ? new Color(0x111827) : Color.WHITE);
        UIManager.put("ComboBox.foreground", TEXT_PRIMARY);
        UIManager.put("CheckBox.background", CARD_BG);
        UIManager.put("CheckBox.foreground", TEXT_PRIMARY);
        UIManager.put("ToolTip.background", isDarkTheme() ? new Color(0x111827) : new Color(0xffffe1));
        UIManager.put("ToolTip.foreground", TEXT_PRIMARY);
    }

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
