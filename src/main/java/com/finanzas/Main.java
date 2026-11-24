package com.finanzas;

import com.finanzas.ui.MainFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Enable antialiasing system-wide
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(() -> {
            // La pantalla de acceso usa el tema claro; MainFrame aplica luego el tema del usuario.
            com.finanzas.ui.components.AppColors.applyTheme("LIGHT");
            new com.finanzas.ui.LoginFrame().setVisible(true);
        });
    }
}
