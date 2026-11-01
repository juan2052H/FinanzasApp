package com.finanzas.ui.components;

public final class AppIcons {
    public static final String APP = "💠";
    public static final String HOME = "🏠";
    public static final String INCOME = "📈";
    public static final String EXPENSE = "💸";
    public static final String BUDGET = "📊";
    public static final String GOALS = "🎯";
    public static final String HOUSEHOLD = "🏡";
    public static final String REPORTS = "📋";
    public static final String SETTINGS = "⚙️";
    public static final String LOGOUT = "🚪";
    public static final String BALANCE = "💼";
    public static final String SAVINGS = "🏦";
    public static final String CALENDAR = "🗓️";
    public static final String DONUT = "🍩";
    public static final String CHART = "📉";
    public static final String PLUS = "➕";
    public static final String ALERT = "⚠️";
    public static final String INFO = "ℹ️";
    public static final String IDEA = "💡";
    public static final String EXPORT = "📤";
    public static final String ARROW = "›";

    private static final int[] WINDOW_ICON_SIZES = {16, 32, 48, 256};

    private AppIcons() {
    }

    /** Asigna el icono de FinanzasApp a la ventana (barra de titulo y barra de tareas). */
    public static void applyWindowIcon(java.awt.Window window) {
        java.util.List<java.awt.Image> images = new java.util.ArrayList<>();
        for (int size : WINDOW_ICON_SIZES) {
            java.net.URL url = AppIcons.class.getResource("/icons/finanzasapp-" + size + ".png");
            if (url != null) {
                images.add(new javax.swing.ImageIcon(url).getImage());
            }
        }
        if (!images.isEmpty()) {
            window.setIconImages(images);
        }
    }
}
