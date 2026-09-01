package com.finanzas;

import com.finanzas.ui.MainFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Enable antialiasing system-wide
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            // new MainFrame();
            new com.finanzas.ui.LoginFrame().setVisible(true);
        });
    }
}
