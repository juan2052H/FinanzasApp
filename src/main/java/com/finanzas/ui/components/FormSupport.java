package com.finanzas.ui.components;

import javax.swing.*;
import java.awt.*;

public final class FormSupport {
    private FormSupport() {
    }

    public static GridBagConstraints baseConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 4, 8, 4);
        return gbc;
    }

    public static void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, Component field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.35;
        JLabel view = new JLabel(label);
        view.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(view, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.65;
        panel.add(field, gbc);
    }
}
